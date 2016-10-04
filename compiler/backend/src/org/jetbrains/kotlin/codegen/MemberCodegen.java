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
import com.intellij.psi.PsiElement;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.inline.*;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.load.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.kotlin.resolve.BindingContext.VARIABLE;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt.Synthetic;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class MemberCodegen<T extends KtElement/* TODO: & JetDeclarationContainer*/> {
    protected final GenerationState state;
    protected final T element;
    protected final FieldOwnerContext context;
    protected final ClassBuilder v;
    protected final FunctionCodegen functionCodegen;
    protected final PropertyCodegen propertyCodegen;
    protected final KotlinTypeMapper typeMapper;
    protected final BindingContext bindingContext;
    protected final JvmFileClassesProvider fileClassesProvider;
    private final MemberCodegen<?> parentCodegen;
    private final ReifiedTypeParametersUsages reifiedTypeParametersUsages = new ReifiedTypeParametersUsages();
    private final Collection<ClassDescriptor> innerClasses = new LinkedHashSet<ClassDescriptor>();

    protected ExpressionCodegen clInit;
    private NameGenerator inlineNameGenerator;

    private DefaultSourceMapper sourceMapper;

    private final ConstantExpressionEvaluator constantExpressionEvaluator;

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
        this.fileClassesProvider = state.getFileClassesProvider();
        this.element = element;
        this.context = context;
        this.v = builder;
        this.functionCodegen = new FunctionCodegen(context, v, state, this);
        this.propertyCodegen = new PropertyCodegen(context, v, functionCodegen, this);
        this.parentCodegen = parentCodegen;
        this.constantExpressionEvaluator = new ConstantExpressionEvaluator(state.getModule().getBuiltIns());
    }

    protected MemberCodegen(@NotNull MemberCodegen<T> wrapped, T declaration, FieldOwnerContext codegenContext) {
        this(wrapped.state, wrapped.getParentCodegen(), codegenContext, declaration, wrapped.v);
    }

    public void generate() {
        generateDeclaration();

        generateBody();

        generateSyntheticParts();

        if (state.getClassBuilderMode().generateMetadata) {
            generateKotlinMetadataAnnotation();
        }

        done();
    }

    protected abstract void generateDeclaration();

    protected abstract void generateBody();

    protected void generateSyntheticParts() {
    }

    protected abstract void generateKotlinMetadataAnnotation();

    @Nullable
    protected ClassDescriptor classForInnerClassRecord() {
        return null;
    }

    public static void markLineNumberForDescriptor(@Nullable ClassDescriptor declarationDescriptor, @NotNull InstructionAdapter v) {
        if (declarationDescriptor == null) {
            return;
        }

        PsiElement classElement = DescriptorToSourceUtils.getSourceFromDescriptor(declarationDescriptor);
        if (classElement != null) {
            markLineNumberForElement(classElement, v);
        }
    }

    public static void markLineNumberForElement(@NotNull PsiElement element, @NotNull InstructionAdapter v) {
        Integer lineNumber = CodegenUtil.getLineNumberForElement(element, false);
        if (lineNumber != null) {
            Label label = new Label();
            v.visitLabel(label);
            v.visitLineNumber(lineNumber, label);
        }
    }

    protected void done() {
        if (clInit != null) {
            clInit.v.visitInsn(RETURN);
            FunctionCodegen.endVisit(clInit.v, "static initializer", element);
        }

        writeInnerClasses();

        if (sourceMapper != null) {
            SourceMapper.Companion.flushToClassBuilder(sourceMapper, v);
        }

        v.done();
    }

    public void genFunctionOrProperty(@NotNull KtDeclaration functionOrProperty) {
        if (functionOrProperty instanceof KtNamedFunction) {
            try {
                functionCodegen.gen((KtNamedFunction) functionOrProperty);
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
        else if (functionOrProperty instanceof KtProperty) {
            try {
                propertyCodegen.gen((KtProperty) functionOrProperty);
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
            @NotNull KtClassOrObject aClass,
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
        ClassBuilder classBuilder = state.getFactory().newVisitor(JvmDeclarationOriginKt.OtherOrigin(aClass, descriptor), classType, aClass.getContainingFile());
        ClassContext classContext = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state);
        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state, parentCodegen, false).generate();
    }

    private static void badDescriptor(ClassDescriptor descriptor, ClassBuilderMode mode) {
        if (mode.generateBodies) {
            throw new IllegalStateException("Generating bad descriptor in ClassBuilderMode = " + mode + ": " + descriptor);
        }
    }

    public void genClassOrObject(KtClassOrObject aClass) {
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

    // It's necessary for proper recovering of classId by plain string JVM descriptor when loading annotations
    // See FileBasedKotlinClass.convertAnnotationVisitor
    public void addInnerClassInfoFromAnnotation(@NotNull ClassDescriptor classDescriptor) {
        DeclarationDescriptor current = classDescriptor;
        while (current != null && !isTopLevelDeclaration(current)) {
            if (current instanceof ClassDescriptor) {
                innerClasses.add(((ClassDescriptor) current));
            }
            current = current.getContainingDeclaration();
        }
    }

    private void writeInnerClass(@NotNull ClassDescriptor innerClass) {
        DeclarationDescriptor containing = innerClass.getContainingDeclaration();
        String outerClassInternalName = null;
        if (containing instanceof ClassDescriptor) {
            outerClassInternalName = typeMapper.mapClass((ClassDescriptor) containing).getInternalName();
        }
        String innerName = innerClass.getName().isSpecial() ? null : innerClass.getName().asString();
        String innerClassInternalName = typeMapper.mapClass(innerClass).getInternalName();
        v.visitInnerClass(innerClassInternalName, outerClassInternalName, innerName, calculateInnerClassAccessFlags(innerClass));
    }

    protected void writeOuterClassAndEnclosingMethod() {
        CodegenContext context = this.context.getParentContext();

        while (context instanceof InlineLambdaContext) {
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
        else if (outermost instanceof MultifileClassFacadeContext || outermost instanceof DelegatingToPartContext) {
            Type implementationOwnerType = CodegenContextUtil.getImplementationOwnerClassType(outermost);
            if (implementationOwnerType != null) {
                return implementationOwnerType;
            }
            else {
                return FileClasses.getFileClassType(fileClassesProvider, element.getContainingKtFile());
            }
        }

        return null;
    }

    @Nullable
    private Method computeEnclosingMethod(@NotNull CodegenContext context) {
        if (context instanceof MethodContext) {
            Method method = typeMapper.mapAsmMethod(((MethodContext) context).getFunctionDescriptor());
            if (!method.getName().equals("<clinit>")) {
                return method;
            }
        }
        return null;
    }

    @NotNull
    public NameGenerator getInlineNameGenerator() {
        if (inlineNameGenerator == null) {
            String prefix = InlineCodegenUtil.getInlineName(context, typeMapper, fileClassesProvider);
            inlineNameGenerator = new NameGenerator(prefix);
        }
        return inlineNameGenerator;
    }

    @NotNull
    protected final ExpressionCodegen createOrGetClInitCodegen() {
        if (clInit == null) {
            DeclarationDescriptor contextDescriptor = context.getContextDescriptor();
            SimpleFunctionDescriptorImpl clInitDescriptor = createClInitFunctionDescriptor(contextDescriptor);
            MethodVisitor mv = createClInitMethodVisitor(contextDescriptor);
            clInit = new ExpressionCodegen(mv, new FrameMap(), Type.VOID_TYPE, context.intoFunction(clInitDescriptor), state, this);
        }
        return clInit;
    }

    @NotNull
    protected MethodVisitor createClInitMethodVisitor(@NotNull DeclarationDescriptor contextDescriptor) {
        return v.newMethod(JvmDeclarationOriginKt.OtherOrigin(contextDescriptor), ACC_STATIC, "<clinit>", "()V", null, null);
    }

    @NotNull
    protected SimpleFunctionDescriptorImpl createClInitFunctionDescriptor(@NotNull DeclarationDescriptor descriptor) {
        SimpleFunctionDescriptorImpl clInit = SimpleFunctionDescriptorImpl.create(descriptor, Annotations.Companion.getEMPTY(),
                Name.special("<clinit>"), SYNTHESIZED, KotlinSourceElementKt.toSourceElement(element));
        clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                          Collections.<ValueParameterDescriptor>emptyList(),
                          DescriptorUtilsKt.getModule(descriptor).getBuiltIns().getUnitType(),
                          null, Visibilities.PRIVATE);
        return clInit;
    }

    protected void generateInitializers(@NotNull Function0<ExpressionCodegen> createCodegen) {
        NotNullLazyValue<ExpressionCodegen> codegen = LockBasedStorageManager.NO_LOCKS.createLazyValue(createCodegen);
        for (KtDeclaration declaration : ((KtDeclarationContainer) element).getDeclarations()) {
            if (declaration instanceof KtProperty) {
                if (shouldInitializeProperty((KtProperty) declaration)) {
                    initializeProperty(codegen.invoke(), (KtProperty) declaration);
                }
            }
            else if (declaration instanceof KtAnonymousInitializer) {
                KtExpression body = ((KtAnonymousInitializer) declaration).getBody();
                if (body != null) {
                    codegen.invoke().gen(body, Type.VOID_TYPE);
                }
            }
        }
    }

    public void beforeMethodBody(@NotNull MethodVisitor mv) {
    }

    private void initializeProperty(@NotNull ExpressionCodegen codegen, @NotNull KtProperty property) {
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        KtExpression initializer = property.getDelegateExpressionOrInitializer();
        assert initializer != null : "shouldInitializeProperty must return false if initializer is null";

        StackValue.Property propValue = codegen.intermediateValueForProperty(propertyDescriptor, true, false, null, true, StackValue.LOCAL_0,
                                                                             null);

        propValue.store(codegen.gen(initializer), codegen.v);
    }

    protected boolean shouldInitializeProperty(@NotNull KtProperty property) {
        if (!property.hasDelegateExpressionOrInitializer()) return false;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        if (propertyDescriptor.isConst()) {
            //const initializer always inlined
            return false;
        }

        KtExpression initializer = property.getInitializer();

        ConstantValue<?> initializerValue =
                initializer != null ? ExpressionCodegen.getCompileTimeConstant(initializer, bindingContext) : null;
        // we must write constant values for fields in light classes,
        // because Java's completion for annotation arguments uses this information
        if (initializerValue == null) return state.getClassBuilderMode().generateBodies;

        //TODO: OPTIMIZATION: don't initialize static final fields
        KotlinType jetType = getPropertyOrDelegateType(property, propertyDescriptor);
        Type type = typeMapper.mapType(jetType);
        return !skipDefaultValue(propertyDescriptor, initializerValue.getValue(), type);
    }

    @NotNull
    private KotlinType getPropertyOrDelegateType(@NotNull KtProperty property, @NotNull PropertyDescriptor descriptor) {
        KtExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            KotlinType delegateType = bindingContext.getType(delegateExpression);
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

    protected void generatePropertyMetadataArrayFieldIfNeeded(@NotNull Type thisAsmType) {
        List<KtProperty> delegatedProperties = new ArrayList<KtProperty>();
        for (KtDeclaration declaration : ((KtDeclarationContainer) element).getDeclarations()) {
            if (declaration instanceof KtProperty) {
                KtProperty property = (KtProperty) declaration;
                if (property.hasDelegate()) {
                    delegatedProperties.add(property);
                }
            }
        }
        if (delegatedProperties.isEmpty()) return;

        v.newField(NO_ORIGIN, ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME,
                   "[" + K_PROPERTY_TYPE, null, null);

        if (!state.getClassBuilderMode().generateBodies) return;

        InstructionAdapter iv = createOrGetClInitCodegen().v;
        iv.iconst(delegatedProperties.size());
        iv.newarray(K_PROPERTY_TYPE);

        for (int i = 0, size = delegatedProperties.size(); i < size; i++) {
            PropertyDescriptor property =
                    (PropertyDescriptor) BindingContextUtils.getNotNull(bindingContext, VARIABLE, delegatedProperties.get(i));

            iv.dup();
            iv.iconst(i);

            int receiverCount = (property.getDispatchReceiverParameter() != null ? 1 : 0) +
                                (property.getExtensionReceiverParameter() != null ? 1 : 0);
            Type implType = property.isVar() ? MUTABLE_PROPERTY_REFERENCE_IMPL[receiverCount] : PROPERTY_REFERENCE_IMPL[receiverCount];
            iv.anew(implType);
            iv.dup();
            // TODO: generate the container once and save to a local field instead (KT-10495)
            ClosureCodegen.generateCallableReferenceDeclarationContainer(iv, property, state);
            iv.aconst(property.getName().asString());
            PropertyReferenceCodegen.generateCallableReferenceSignature(iv, property, state);
            iv.invokespecial(
                    implType.getInternalName(), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, K_DECLARATION_CONTAINER_TYPE, JAVA_STRING_TYPE, JAVA_STRING_TYPE), false
            );
            Method wrapper = PropertyReferenceCodegen.getWrapperMethodForPropertyReference(property, receiverCount);
            iv.invokestatic(REFLECTION, wrapper.getName(), wrapper.getDescriptor(), false);

            StackValue.onStack(implType).put(K_PROPERTY_TYPE, iv);

            iv.astore(K_PROPERTY_TYPE);
        }

        iv.putstatic(thisAsmType.getInternalName(), JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME, "[" + K_PROPERTY_TYPE);
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
    public DefaultSourceMapper getOrCreateSourceMapper() {
        if (sourceMapper == null) {
            sourceMapper = new DefaultSourceMapper(SourceInfo.Companion.createInfo(element, getClassName()));
        }
        return sourceMapper;
    }

    protected void generateConstInstance(@NotNull Type thisAsmType, @NotNull Type fieldAsmType) {
        v.newField(
                JvmDeclarationOriginKt.OtherOrigin(element), ACC_STATIC | ACC_FINAL | ACC_PUBLIC, JvmAbi.INSTANCE_FIELD,
                fieldAsmType.getDescriptor(), null, null
        );

        if (state.getClassBuilderMode().generateBodies) {
            InstructionAdapter iv = createOrGetClInitCodegen().v;
            iv.anew(thisAsmType);
            iv.dup();
            iv.invokespecial(thisAsmType.getInternalName(), "<init>", "()V", false);
            iv.putstatic(thisAsmType.getInternalName(), JvmAbi.INSTANCE_FIELD, fieldAsmType.getDescriptor());
        }
    }

    protected void generateSyntheticAccessors() {
        for (AccessorForCallableDescriptor<?> accessor : ((CodegenContext<?>) context).getAccessors()) {
            generateSyntheticAccessor(accessor);
        }
    }

    private void generateSyntheticAccessor(@NotNull AccessorForCallableDescriptor<?> accessorForCallableDescriptor) {
        if (accessorForCallableDescriptor instanceof FunctionDescriptor) {
            final FunctionDescriptor accessor = (FunctionDescriptor) accessorForCallableDescriptor;
            final FunctionDescriptor original = (FunctionDescriptor) accessorForCallableDescriptor.getCalleeDescriptor();
            functionCodegen.generateMethod(
                    Synthetic(null, original), accessor,
                    new FunctionGenerationStrategy.CodegenBased(state) {
                        @Override
                        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                            markLineNumberForElement(element, codegen.v);

                            generateMethodCallTo(original, accessor, codegen.v).coerceTo(signature.getReturnType(), codegen.v);

                            codegen.v.areturn(signature.getReturnType());
                        }
                    }
            );
        }
        else if (accessorForCallableDescriptor instanceof AccessorForPropertyDescriptor) {
            final AccessorForPropertyDescriptor accessor = (AccessorForPropertyDescriptor) accessorForCallableDescriptor;
            final PropertyDescriptor original = accessor.getCalleeDescriptor();

            class PropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased {
                private final PropertyAccessorDescriptor callableDescriptor;
                public PropertyAccessorStrategy(@NotNull PropertyAccessorDescriptor callableDescriptor) {
                    super(MemberCodegen.this.state);
                    this.callableDescriptor = callableDescriptor;
                }

                @Override
                public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                    boolean syntheticBackingField = accessor instanceof AccessorForPropertyBackingFieldFromLocal;
                    boolean forceFieldForCompanionProperty = JvmAbi.isPropertyWithBackingFieldInOuterClass(original) &&
                                                             !isCompanionObject(accessor.getContainingDeclaration());
                    boolean forceField = forceFieldForCompanionProperty ||
                                         syntheticBackingField ||
                                         original.getVisibility() == JavaVisibilities.PROTECTED_STATIC_VISIBILITY;
                    StackValue property = codegen.intermediateValueForProperty(
                            original, forceField, syntheticBackingField, accessor.getSuperCallTarget(),
                            forceFieldForCompanionProperty, StackValue.none(), null
                    );

                    InstructionAdapter iv = codegen.v;

                    markLineNumberForElement(element, iv);

                    Type[] argTypes = signature.getAsmMethod().getArgumentTypes();
                    for (int i = 0, reg = 0; i < argTypes.length; i++) {
                        Type argType = argTypes[i];
                        iv.load(reg, argType);
                        //noinspection AssignmentToForLoopParameter
                        reg += argType.getSize();
                    }

                    if (callableDescriptor instanceof PropertyGetterDescriptor) {
                        property.put(signature.getReturnType(), iv);
                    }
                    else {
                        property.store(StackValue.onStack(property.type), iv, true);
                    }

                    iv.areturn(signature.getReturnType());
                }
            }

            if (accessor.isWithSyntheticGetterAccessor()) {
                PropertyGetterDescriptor getter = accessor.getGetter();
                assert getter != null;
                functionCodegen.generateMethod(Synthetic(null, original.getGetter() != null ? original.getGetter() : original),
                                               getter, new PropertyAccessorStrategy(getter));
            }

            if (accessor.isVar() && accessor.isWithSyntheticSetterAccessor()) {
                PropertySetterDescriptor setter = accessor.getSetter();
                assert setter != null;

                functionCodegen.generateMethod(Synthetic(null, original.getSetter() != null ? original.getSetter() : original),
                                               setter, new PropertyAccessorStrategy(setter));
            }
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    protected StackValue generateMethodCallTo(
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable FunctionDescriptor accessorDescriptor,
            @NotNull InstructionAdapter iv
    ) {
        CallableMethod callableMethod = typeMapper.mapToCallableMethod(
                functionDescriptor,
                accessorDescriptor instanceof AccessorForCallableDescriptor &&
                ((AccessorForCallableDescriptor) accessorDescriptor).getSuperCallTarget() != null
        );

        boolean hasDispatchReceiver = !isStaticDeclaration(functionDescriptor) && !isInterface(functionDescriptor.getContainingDeclaration());
        int reg = hasDispatchReceiver ? 1 : 0;
        boolean accessorIsConstructor = accessorDescriptor instanceof AccessorForConstructorDescriptor;
        if (!accessorIsConstructor && functionDescriptor instanceof ConstructorDescriptor) {
            iv.anew(callableMethod.getOwner());
            iv.dup();
            reg = 0;
        }
        else if (accessorIsConstructor || (accessorDescriptor != null && KotlinTypeMapper.isAccessor(accessorDescriptor) && hasDispatchReceiver)) {
            if (!CodegenUtilKt.isJvmStaticInObjectOrClass(functionDescriptor)) {
                iv.load(0, OBJECT_TYPE);
            }
        }

        for (Type argType : callableMethod.getParameterTypes()) {
            if (AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.equals(argType)) {
                iv.aconst(null);
            }
            else {
                iv.load(reg, argType);
                reg += argType.getSize();
            }
        }

        callableMethod.genInvokeInstruction(iv);

        return StackValue.onStack(callableMethod.getReturnType());
    }

    protected void generateKotlinClassMetadataAnnotation(ClassDescriptor descriptor) {
        final DescriptorSerializer serializer =
                DescriptorSerializer.create(descriptor, new JvmSerializerExtension(v.getSerializationBindings(), state));

        final ProtoBuf.Class classProto = serializer.classProto(descriptor).build();

        WriteAnnotationUtilKt.writeKotlinMetadata(v, KotlinClassHeader.Kind.CLASS, new Function1<AnnotationVisitor, Unit>() {
            @Override
            public Unit invoke(AnnotationVisitor av) {
                writeAnnotationData(av, serializer, classProto);
                return Unit.INSTANCE;
            }
        });
    }

}
