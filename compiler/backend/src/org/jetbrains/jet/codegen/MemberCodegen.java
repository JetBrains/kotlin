/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.context.ClassContext;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil;
import org.jetbrains.jet.codegen.inline.NameGenerator;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.jet.lang.resolve.BindingContext.VARIABLE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.*;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.TraitImpl;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class MemberCodegen<T extends JetElement/* TODO: & JetDeclarationContainer*/> extends ParentCodegenAware {
    protected final T element;
    protected final FieldOwnerContext context;
    protected final ClassBuilder v;
    protected final FunctionCodegen functionCodegen;
    protected final PropertyCodegen propertyCodegen;

    protected ExpressionCodegen clInit;
    private NameGenerator inlineNameGenerator;

    public MemberCodegen(
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen,
            @NotNull FieldOwnerContext context,
            T element,
            @NotNull ClassBuilder builder
    ) {
        super(state, parentCodegen);
        this.element = element;
        this.context = context;
        this.v = builder;
        this.functionCodegen = new FunctionCodegen(context, v, state, this);
        this.propertyCodegen = new PropertyCodegen(context, v, functionCodegen, this);
    }

    public void generate() {
        generateDeclaration();

        generateBody();

        generateSyntheticParts();

        generateKotlinAnnotation();

        done();
    }

    protected abstract void generateDeclaration();

    protected abstract void generateBody();

    protected void generateSyntheticParts() {
    }

    protected abstract void generateKotlinAnnotation();

    private void done() {
        if (clInit != null) {
            clInit.v.visitInsn(RETURN);
            FunctionCodegen.endVisit(clInit.v, "static initializer", element);
        }

        v.done();
    }

    public void genFunctionOrProperty(@NotNull JetDeclaration functionOrProperty) {
        if (functionOrProperty instanceof JetNamedFunction) {
            try {
                functionCodegen.gen((JetNamedFunction) functionOrProperty);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate function " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else if (functionOrProperty instanceof JetProperty) {
            try {
                propertyCodegen.gen((JetProperty) functionOrProperty);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate property " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown parameter: " + functionOrProperty);
        }
    }

    public static void genClassOrObject(
            @NotNull CodegenContext parentContext,
            @NotNull JetClassOrObject aClass,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        if (descriptor == null || ErrorUtils.isError(descriptor)) {
            badDescriptor(descriptor, state.getClassBuilderMode());
            return;
        }

        if (descriptor.getName().equals(SpecialNames.NO_NAME_PROVIDED)) {
            badDescriptor(descriptor, state.getClassBuilderMode());
        }

        Type classType = state.getTypeMapper().mapClass(descriptor);
        ClassBuilder classBuilder = state.getFactory().newVisitor(OtherOrigin(aClass, descriptor), classType, aClass.getContainingFile());
        ClassContext classContext = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state);
        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state, parentCodegen).generate();

        if (aClass instanceof JetClass && ((JetClass) aClass).isTrait()) {
            Type traitImplType = state.getTypeMapper().mapTraitImpl(descriptor);
            ClassBuilder traitImplBuilder = state.getFactory().newVisitor(TraitImpl(aClass, descriptor), traitImplType, aClass.getContainingFile());
            ClassContext traitImplContext = parentContext.intoClass(descriptor, OwnerKind.TRAIT_IMPL, state);
            new TraitImplBodyCodegen(aClass, traitImplContext, traitImplBuilder, state, parentCodegen).generate();
        }
    }

    private static void badDescriptor(ClassDescriptor descriptor, ClassBuilderMode mode) {
        if (mode != ClassBuilderMode.LIGHT_CLASSES) {
            throw new IllegalStateException("Generating bad descriptor in ClassBuilderMode = " + mode + ": " + descriptor);
        }
    }

    public void genClassOrObject(JetClassOrObject aClass) {
        genClassOrObject(context, aClass, state, this);
    }

    @NotNull
    public NameGenerator getInlineNameGenerator() {
        if (inlineNameGenerator == null) {
            String prefix = InlineCodegenUtil.getInlineName(context, typeMapper);
            inlineNameGenerator = new NameGenerator(prefix);
        }
        return inlineNameGenerator;
    }

    @NotNull
    protected ExpressionCodegen createOrGetClInitCodegen() {
        DeclarationDescriptor descriptor = context.getContextDescriptor();
        if (clInit == null) {
            MethodVisitor mv = v.newMethod(OtherOrigin(descriptor), ACC_STATIC, "<clinit>", "()V", null, null);
            SimpleFunctionDescriptorImpl clInit =
                    SimpleFunctionDescriptorImpl.create(descriptor, Annotations.EMPTY, Name.special("<clinit>"), SYNTHESIZED);
            clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                              Collections.<ValueParameterDescriptor>emptyList(), null, null, Visibilities.PRIVATE);

            this.clInit = new ExpressionCodegen(mv, new FrameMap(), Type.VOID_TYPE, context.intoFunction(clInit), state, this);
        }
        return clInit;
    }

    protected void generateInitializers(@NotNull Function0<ExpressionCodegen> createCodegen) {
        NotNullLazyValue<ExpressionCodegen> codegen = LockBasedStorageManager.NO_LOCKS.createLazyValue(createCodegen);
        for (JetDeclaration declaration : ((JetDeclarationContainer) element).getDeclarations()) {
            if (declaration instanceof JetProperty) {
                if (shouldInitializeProperty((JetProperty) declaration)) {
                    initializeProperty(codegen.invoke(), (JetProperty) declaration);
                }
            }
            else if (declaration instanceof JetClassInitializer) {
                codegen.invoke().gen(((JetClassInitializer) declaration).getBody(), Type.VOID_TYPE);
            }
        }
    }

    private void initializeProperty(@NotNull ExpressionCodegen codegen, @NotNull JetProperty property) {
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        JetExpression initializer = property.getDelegateExpressionOrInitializer();
        assert initializer != null : "shouldInitializeProperty must return false if initializer is null";

        JetType jetType = getPropertyOrDelegateType(property, propertyDescriptor);

        StackValue.Property propValue = codegen.intermediateValueForProperty(propertyDescriptor, true, null, MethodKind.INITIALIZER);

        if (!propValue.isStatic) {
            codegen.v.load(0, OBJECT_TYPE);
        }

        Type type = codegen.expressionType(initializer);
        if (jetType.isNullable()) {
            type = boxType(type);
        }
        codegen.gen(initializer, type);

        propValue.store(type, codegen.v);
    }

    private boolean shouldInitializeProperty(@NotNull JetProperty property) {
        if (!property.hasDelegateExpressionOrInitializer()) return false;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        CompileTimeConstant<?> compileTimeValue = propertyDescriptor.getCompileTimeInitializer();
        // we must write constant values for fields in light classes,
        // because Java's completion for annotation arguments uses this information
        if (compileTimeValue == null) return state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES;

        //TODO: OPTIMIZATION: don't initialize static final fields

        Object value = compileTimeValue.getValue();
        JetType jetType = getPropertyOrDelegateType(property, propertyDescriptor);
        Type type = typeMapper.mapType(jetType);
        return !skipDefaultValue(propertyDescriptor, value, type);
    }

    @NotNull
    private JetType getPropertyOrDelegateType(@NotNull JetProperty property, @NotNull PropertyDescriptor descriptor) {
        JetExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            JetType delegateType = bindingContext.get(BindingContext.EXPRESSION_TYPE, delegateExpression);
            assert delegateType != null : "Type of delegate expression should be recorded";
            return delegateType;
        }
        return descriptor.getType();
    }

    private static boolean skipDefaultValue(@NotNull PropertyDescriptor propertyDescriptor, Object value, @NotNull Type type) {
        if (isPrimitive(type)) {
            if (!propertyDescriptor.getType().isNullable() && value instanceof Number) {
                if (type == Type.INT_TYPE && ((Number) value).intValue() == 0) {
                    return true;
                }
                if (type == Type.BYTE_TYPE && ((Number) value).byteValue() == 0) {
                    return true;
                }
                if (type == Type.LONG_TYPE && ((Number) value).longValue() == 0L) {
                    return true;
                }
                if (type == Type.SHORT_TYPE && ((Number) value).shortValue() == 0) {
                    return true;
                }
                if (type == Type.DOUBLE_TYPE && ((Number) value).doubleValue() == 0d) {
                    return true;
                }
                if (type == Type.FLOAT_TYPE && ((Number) value).floatValue() == 0f) {
                    return true;
                }
            }
            if (type == Type.BOOLEAN_TYPE && value instanceof Boolean && !((Boolean) value)) {
                return true;
            }
            if (type == Type.CHAR_TYPE && value instanceof Character && ((Character) value) == 0) {
                return true;
            }
        }
        else {
            if (value == null) {
                return true;
            }
        }
        return false;
    }

    public static void generateReflectionObjectField(
            @NotNull GenerationState state,
            @NotNull Type thisAsmType,
            @NotNull ClassBuilder classBuilder,
            @NotNull Type kImplType,
            @NotNull String fieldName,
            @NotNull InstructionAdapter v
    ) {
        // TODO: generic signature
        classBuilder.newField(NO_ORIGIN, ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, fieldName, kImplType.getDescriptor(),
                              null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return;

        v.anew(kImplType);
        v.dup();
        v.aconst(thisAsmType);
        v.invokespecial(kImplType.getInternalName(), "<init>", "(Ljava/lang/Class;)V", false);
        v.putstatic(thisAsmType.getInternalName(), fieldName, kImplType.getDescriptor());
    }

    protected void generatePropertyMetadataArrayFieldIfNeeded(@NotNull Type thisAsmType) {
        List<JetProperty> delegatedProperties = new ArrayList<JetProperty>();
        for (JetDeclaration declaration : ((JetDeclarationContainer) element).getDeclarations()) {
            if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                if (property.hasDelegate()) {
                    delegatedProperties.add(property);
                }
            }
        }
        if (delegatedProperties.isEmpty()) return;

        v.newField(NO_ORIGIN, ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, JvmAbi.PROPERTY_METADATA_ARRAY_NAME,
                   "[" + PROPERTY_METADATA_TYPE, null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return;

        InstructionAdapter iv = createOrGetClInitCodegen().v;
        iv.iconst(delegatedProperties.size());
        iv.newarray(PROPERTY_METADATA_TYPE);

        for (int i = 0, size = delegatedProperties.size(); i < size; i++) {
            VariableDescriptor property = BindingContextUtils.getNotNull(bindingContext, VARIABLE, delegatedProperties.get(i));

            iv.dup();
            iv.iconst(i);
            iv.anew(PROPERTY_METADATA_IMPL_TYPE);
            iv.dup();
            iv.visitLdcInsn(property.getName().asString());
            iv.invokespecial(PROPERTY_METADATA_IMPL_TYPE.getInternalName(), "<init>", "(Ljava/lang/String;)V");
            iv.astore(PROPERTY_METADATA_IMPL_TYPE);
        }

        iv.putstatic(thisAsmType.getInternalName(), JvmAbi.PROPERTY_METADATA_ARRAY_NAME, "[" + PROPERTY_METADATA_TYPE);
    }

    public String getClassName() {
        return v.getThisName();
    }
}
