/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.inline.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.calculateInnerClassAccessFlags;
import static org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE;
import static org.jetbrains.kotlin.resolve.BindingContext.VARIABLE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.TraitImpl;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class MemberCodegen<T extends JetElement/* TODO: & JetDeclarationContainer*/> {
    protected final GenerationState state;
    protected final T element;
    protected final FieldOwnerContext context;
    protected final ClassBuilder v;
    protected final FunctionCodegen functionCodegen;
    protected final PropertyCodegen propertyCodegen;
    protected final JetTypeMapper typeMapper;
    protected final BindingContext bindingContext;
    private final MemberCodegen<?> parentCodegen;
    private final ReifiedTypeParametersUsages reifiedTypeParametersUsages = new ReifiedTypeParametersUsages();
    protected final Collection<ClassDescriptor> innerClasses = new LinkedHashSet<ClassDescriptor>();

    protected ExpressionCodegen clInit;
    private NameGenerator inlineNameGenerator;

    private SourceMapper sourceMapper;

    public MemberCodegen(
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen,
            @NotNull FieldOwnerContext context,
            T element,
            @NotNull ClassBuilder builder
    ) {
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = state.getBindingContext();
        this.element = element;
        this.context = context;
        this.v = builder;
        this.functionCodegen = new FunctionCodegen(context, v, state, this);
        this.propertyCodegen = new PropertyCodegen(context, v, functionCodegen, this);
        this.parentCodegen = parentCodegen;
    }

    protected MemberCodegen(@NotNull MemberCodegen<T> wrapped, T declaration, FieldOwnerContext codegenContext) {
        this(wrapped.state, wrapped.getParentCodegen(), codegenContext, declaration, wrapped.v);
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

    @Nullable
    protected ClassDescriptor classForInnerClassRecord() {
        return null;
    }

    protected void done() {
        if (clInit != null) {
            clInit.v.visitInsn(RETURN);
            FunctionCodegen.endVisit(clInit.v, "static initializer", element);
        }

        writeInnerClasses();

        if (sourceMapper != null) {
            SourceMapper.Default.flushToClassBuilder(sourceMapper, v);
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

    private void writeInnerClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        ClassDescriptor classDescriptor = classForInnerClassRecord();
        if (classDescriptor != null) {
            if (parentCodegen != null) {
                parentCodegen.innerClasses.add(classDescriptor);
            }

            for (MemberCodegen<?> codegen = this; codegen != null; codegen = codegen.getParentCodegen()) {
                ClassDescriptor outerClass = codegen.classForInnerClassRecord();
                if (outerClass != null) {
                    innerClasses.add(outerClass);
                }
            }
        }

        for (ClassDescriptor innerClass : innerClasses) {
            writeInnerClass(innerClass);
        }
    }

    private void writeInnerClass(@NotNull ClassDescriptor innerClass) {
        DeclarationDescriptor containing = innerClass.getContainingDeclaration();
        String outerClassInternalName =
                containing instanceof ClassDescriptor ? typeMapper.mapClass((ClassDescriptor) containing).getInternalName() : null;

        String innerName = innerClass.getName().isSpecial() ? null : innerClass.getName().asString();

        String innerClassInternalName = typeMapper.mapClass(innerClass).getInternalName();
        v.visitInnerClass(innerClassInternalName, outerClassInternalName, innerName, calculateInnerClassAccessFlags(innerClass));
    }

    protected void writeOuterClassAndEnclosingMethod() {
        CodegenContext context = this.context.getParentContext();
        while (context instanceof MethodContext && ((MethodContext) context).isInliningLambda()) {
            // If this is a lambda which will be inlined, skip its MethodContext and enclosing ClosureContext
            //noinspection ConstantConditions
            context = context.getParentContext().getParentContext();
        }
        assert context != null : "Outermost context can't be null: " + this.context;

        Type enclosingAsmType = computeOuterClass(context);
        if (enclosingAsmType != null) {
            Method method = computeEnclosingMethod(context);

            v.visitOuterClass(
                    enclosingAsmType.getInternalName(),
                    method == null ? null : method.getName(),
                    method == null ? null : method.getDescriptor()
            );
        }
    }

    @Nullable
    private Type computeOuterClass(@NotNull CodegenContext<?> context) {
        CodegenContext<? extends ClassOrPackageFragmentDescriptor> outermost = context.getClassOrPackageParentContext();
        if (outermost instanceof ClassContext) {
            return typeMapper.mapType(((ClassContext) outermost).getContextDescriptor());
        }
        else if (outermost instanceof PackageContext && !(outermost instanceof PackageFacadeContext)) {
            return PackagePartClassUtils.getPackagePartType(element.getContainingJetFile());
        }
        return null;
    }

    @Nullable
    private Method computeEnclosingMethod(@NotNull CodegenContext context) {
        if (context instanceof MethodContext) {
            Method method = typeMapper.mapSignature(((MethodContext) context).getFunctionDescriptor()).getAsmMethod();
            if (!method.getName().equals("<clinit>")) {
                return method;
            }
        }
        return null;
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
                    SimpleFunctionDescriptorImpl.create(descriptor, Annotations.EMPTY, Name.special("<clinit>"), SYNTHESIZED, NO_SOURCE);
            clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                              Collections.<ValueParameterDescriptor>emptyList(),
                              DescriptorUtilPackage.getModule(descriptor).getBuiltIns().getUnitType(),
                              null, Visibilities.PRIVATE);

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

        StackValue.Property propValue = codegen.intermediateValueForProperty(propertyDescriptor, true, null, MethodKind.INITIALIZER,
                                                                             StackValue.LOCAL_0);

        propValue.store(codegen.gen(initializer), codegen.v);

        ResolvedCall<FunctionDescriptor> pdResolvedCall =
                bindingContext.get(BindingContext.DELEGATED_PROPERTY_PD_RESOLVED_CALL, propertyDescriptor);
        if (pdResolvedCall != null) {
            int index = PropertyCodegen.indexOfDelegatedProperty(property);
            StackValue lastValue = PropertyCodegen.invokeDelegatedPropertyConventionMethod(
                    propertyDescriptor, codegen, typeMapper, pdResolvedCall, index, 0
            );
            lastValue.put(Type.VOID_TYPE, codegen.v);
        }
    }

    private boolean shouldInitializeProperty(@NotNull JetProperty property) {
        if (!property.hasDelegateExpressionOrInitializer()) return false;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        JetExpression initializer = property.getInitializer();

        CompileTimeConstant<?> initializerValue;
        if (property.isVar() && initializer != null) {
            BindingTrace tempTrace = TemporaryBindingTrace.create(state.getBindingTrace(), "property initializer");
            initializerValue = ConstantExpressionEvaluator.evaluate(initializer, tempTrace, propertyDescriptor.getType());
        }
        else {
            initializerValue = propertyDescriptor.getCompileTimeInitializer();
        }
        // we must write constant values for fields in light classes,
        // because Java's completion for annotation arguments uses this information
        if (initializerValue == null) return state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES;

        //TODO: OPTIMIZATION: don't initialize static final fields

        Object value = initializerValue instanceof IntegerValueTypeConstant
            ? ((IntegerValueTypeConstant) initializerValue).getValue(propertyDescriptor.getType())
            : initializerValue.getValue();
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
            if (!propertyDescriptor.getType().isMarkedNullable() && value instanceof Number) {
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
            @NotNull Method factory,
            @NotNull String fieldName,
            @NotNull InstructionAdapter v
    ) {
        String type = factory.getReturnType().getDescriptor();
        // TODO: generic signature
        classBuilder.newField(NO_ORIGIN, ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, fieldName, type, null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return;

        v.aconst(thisAsmType);
        v.invokestatic(REFLECTION, factory.getName(), factory.getDescriptor(), false);
        v.putstatic(thisAsmType.getInternalName(), fieldName, type);
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
            iv.invokespecial(PROPERTY_METADATA_IMPL_TYPE.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
            iv.astore(PROPERTY_METADATA_IMPL_TYPE);
        }

        iv.putstatic(thisAsmType.getInternalName(), JvmAbi.PROPERTY_METADATA_ARRAY_NAME, "[" + PROPERTY_METADATA_TYPE);
    }

    public String getClassName() {
        return v.getThisName();
    }

    @NotNull
    public FieldOwnerContext<?> getContext() {
        return context;
    }

    @NotNull
    public ReifiedTypeParametersUsages getReifiedTypeParametersUsages() {
        return reifiedTypeParametersUsages;
    }

    public MemberCodegen<?> getParentCodegen() {
        return parentCodegen;
    }

    @Override
    public String toString() {
        return context.toString();
    }

    @NotNull
    public SourceMapper getOrCreateSourceMapper() {
        if (sourceMapper == null) {
            sourceMapper = new DefaultSourceMapper(SourceInfo.Default.createInfo(element, getClassName()), null);
        }
        return sourceMapper;
    }
}
