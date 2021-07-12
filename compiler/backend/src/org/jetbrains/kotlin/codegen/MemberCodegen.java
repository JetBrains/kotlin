/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.inline.NameGenerator;
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages;
import org.jetbrains.kotlin.codegen.inline.SourceMapper;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.codegen.state.TypeMapperUtilsKt;
import org.jetbrains.kotlin.config.JvmDefaultMode;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil;
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.calculateInnerClassAccessFlags;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isNonDefaultInterfaceMember;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtilsKt.getInlineName;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt.Synthetic;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class MemberCodegen<T extends KtPureElement/* TODO: & KtDeclarationContainer*/> implements InnerClassConsumer {
    public final GenerationState state;

    protected final T element;
    protected final FieldOwnerContext<?> context;

    public final ClassBuilder v;
    public final FunctionCodegen functionCodegen;
    public final PropertyCodegen propertyCodegen;
    public final KotlinTypeMapper typeMapper;
    public final BindingContext bindingContext;

    private final MemberCodegen<?> parentCodegen;
    private final ReifiedTypeParametersUsages reifiedTypeParametersUsages = new ReifiedTypeParametersUsages();

    private final Collection<ClassDescriptor> innerClasses = new LinkedHashSet<>();
    private final Collection<SyntheticInnerClassInfo> syntheticInnerClasses = new LinkedHashSet<>();

    private ExpressionCodegen clInit;
    private NameGenerator inlineNameGenerator;
    private boolean jvmAssertFieldGenerated;

    private boolean alwaysWriteSourceMap;
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
        this.jvmAssertFieldGenerated = false;
    }

    protected MemberCodegen(@NotNull MemberCodegen<T> wrapped, T declaration, FieldOwnerContext codegenContext) {
        this(wrapped.state, wrapped.getParentCodegen(), codegenContext, declaration, wrapped.v);
    }

    public void generate() {
        generateDeclaration();

        boolean shouldGenerateSyntheticParts =
                !(element instanceof KtClassOrObject) ||
                state.getGenerateDeclaredClassFilter().shouldGenerateClassMembers((KtClassOrObject) element);

        if (shouldGenerateSyntheticParts) {
            generateSyntheticPartsBeforeBody();
        }

        generateBody();

        if (shouldGenerateSyntheticParts) {
            generateSyntheticPartsAfterBody();
        }

        if (state.getClassBuilderMode().generateMetadata) {
            generateKotlinMetadataAnnotation();
        }

        done();
    }

    protected abstract void generateDeclaration();

    protected void generateSyntheticPartsBeforeBody() {
    }

    protected abstract void generateBody();

    protected void generateSyntheticPartsAfterBody() {
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

        if (alwaysWriteSourceMap || (sourceMapper != null && !sourceMapper.isTrivial())) {
            v.visitSMAP(getOrCreateSourceMapper(), !state.getLanguageVersionSettings().supportsFeature(LanguageFeature.CorrectSourceMappingSyntax));
        }

        v.done();
    }

    public void genSimpleMember(@NotNull KtDeclaration declaration) {
        if (declaration instanceof KtNamedFunction) {
            try {
                functionCodegen.gen((KtNamedFunction) declaration);
            }
            catch (ProcessCanceledException | CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate function " + declaration.getName(), e, declaration);
            }
        }
        else if (declaration instanceof KtProperty) {
            try {
                propertyCodegen.gen((KtProperty) declaration);
            }
            catch (ProcessCanceledException | CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate property " + declaration.getName(), e, declaration);
            }
        }
        else if (declaration instanceof KtTypeAlias) {
            genTypeAlias((KtTypeAlias) declaration);
        }
        else if (declaration instanceof KtDestructuringDeclarationEntry) {
            try {
                propertyCodegen.genDestructuringDeclaration((KtDestructuringDeclarationEntry) declaration);
            }
            catch (ProcessCanceledException | CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate destructuring declaration entry " + declaration.getName(), e, declaration);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown parameter: " + declaration);
        }
    }

    private void genTypeAlias(@NotNull KtTypeAlias typeAlias) {
        if (!state.getClassBuilderMode().generateMetadata) return;

        TypeAliasDescriptor typeAliasDescriptor = bindingContext.get(TYPE_ALIAS, typeAlias);
        if (typeAliasDescriptor == null) {
            throw ExceptionLogger.logDescriptorNotFound("Type alias " + typeAlias.getName() + " should have a descriptor", typeAlias);
        }

        genTypeAliasAnnotationsMethodIfRequired(typeAliasDescriptor);
    }

    private void genTypeAliasAnnotationsMethodIfRequired(TypeAliasDescriptor typeAliasDescriptor) {
        boolean isAnnotationsMethodOwner = CodegenContextUtil.isImplementationOwner(context, typeAliasDescriptor);
        Annotations annotations = typeAliasDescriptor.getAnnotations();
        if (!isAnnotationsMethodOwner || annotations.isEmpty()) return;

        String name = JvmAbi.getSyntheticMethodNameForAnnotatedTypeAlias(typeAliasDescriptor.getName());
        generateSyntheticAnnotationsMethod(typeAliasDescriptor, new Method(name, "()V"), annotations);
    }

    protected void generateSyntheticAnnotationsMethod(
            @NotNull MemberDescriptor descriptor,
            @NotNull Method syntheticMethod,
            @NotNull Annotations annotations
    ) {
        int flags = ACC_DEPRECATED | ACC_STATIC | ACC_SYNTHETIC | DescriptorAsmUtil.getVisibilityAccessFlag(descriptor);
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(descriptor), flags, syntheticMethod.getName(),
                                       syntheticMethod.getDescriptor(), null, null);
        AnnotationCodegen.forMethod(mv, this, state).genAnnotations(new AnnotatedImpl(annotations), Type.VOID_TYPE, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitEnd();
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
        genClassOrObject(parentContext, aClass, state, parentCodegen, descriptor);
    }

    private static void genClassOrObject(
            @NotNull CodegenContext parentContext,
            @NotNull KtPureClassOrObject aClass,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen,
            @NotNull ClassDescriptor descriptor
    ) {

        Type classType = state.getTypeMapper().mapClass(descriptor);
        ClassBuilder classBuilder = state.getFactory().newVisitor(
                JvmDeclarationOriginKt.OtherOriginFromPure(aClass, descriptor),
                classType, aClass.getContainingKtFile());
        ClassContext classContext = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state);
        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state, parentCodegen, false).generate();
    }

    public static void badDescriptor(ClassDescriptor descriptor, ClassBuilderMode mode) {
        if (mode.generateBodies) {
            throw new IllegalStateException("Generating bad descriptor in ClassBuilderMode = " + mode + ": " + descriptor);
        }
    }

    public void genClassOrObject(KtClassOrObject aClass) {
        genClassOrObject(context, aClass, state, this);
    }

    public void genSyntheticClassOrObject(SyntheticClassOrObjectDescriptor descriptor) {
        genClassOrObject(context, descriptor.getSyntheticDeclaration(), state, this, descriptor);
    }

    public void addSyntheticAnonymousInnerClass(SyntheticInnerClassInfo syntheticInnerClassInfo) {
        syntheticInnerClasses.add(syntheticInnerClassInfo);
    }

    private void writeInnerClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        ClassDescriptor classDescriptor = classForInnerClassRecord();
        if (classDescriptor != null) {
            if (parentCodegen != null) {
                parentCodegen.innerClasses.add(classDescriptor);
            }

            addParentsToInnerClassesIfNeeded(innerClasses);
        }

        for (ClassDescriptor innerClass : innerClasses) {
            writeInnerClass(innerClass);
        }
        for (SyntheticInnerClassInfo syntheticInnerClass : syntheticInnerClasses) {
            v.visitInnerClass(syntheticInnerClass.getInternalName(), null, null, syntheticInnerClass.getFlags());
        }
    }

    protected void addParentsToInnerClassesIfNeeded(@NotNull Collection<ClassDescriptor> innerClasses) {
        ClassDescriptor outerClass = classForInnerClassRecord();
        if (outerClass != null) {
            innerClasses.add(outerClass);
        }

        MemberCodegen<?> parentCodegen = getParentCodegen();
        if (parentCodegen != null) {
            parentCodegen.addParentsToInnerClassesIfNeeded(innerClasses);
        }
    }

    // It's necessary for proper recovering of classId by plain string JVM descriptor when loading annotations
    // See FileBasedKotlinClass.convertAnnotationVisitor
    @Override
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
        if (!ErrorUtils.isError(innerClass)) {
            writeInnerClass(innerClass, typeMapper, v);
        }
    }

    public static void writeInnerClass(@NotNull ClassDescriptor innerClass, @NotNull KotlinTypeMapper typeMapper, @NotNull ClassBuilder v) {
        DeclarationDescriptor containing = innerClass.getContainingDeclaration();
        String outerClassInternalName = null;
        if (containing instanceof ClassDescriptor) {
            outerClassInternalName = typeMapper.classInternalName((ClassDescriptor) containing);
        }
        String innerName = innerClass.getName().isSpecial() ? null : innerClass.getName().asString();
        String innerClassInternalName = typeMapper.classInternalName(innerClass);
        v.visitInnerClass(innerClassInternalName, outerClassInternalName, innerName, calculateInnerClassAccessFlags(innerClass));
    }

    protected void writeOuterClassAndEnclosingMethod() {
        CodegenContext<?> context = getNonInlineOuterContext(this.context.getParentContext());
        assert context != null : "Outermost context can't be null: " + this.context;

        Type enclosingAsmType = computeOuterClass(typeMapper, state.getJvmDefaultMode(), element, context);
        if (enclosingAsmType != null) {
            Method method = computeEnclosingMethod(typeMapper, context);

            v.visitOuterClass(
                    enclosingAsmType.getInternalName(),
                    method == null ? null : method.getName(),
                    method == null ? null : method.getDescriptor()
            );
        }
    }

    public static CodegenContext<?> getNonInlineOuterContext(CodegenContext<?> parentContext) {
        CodegenContext<?> context = parentContext;
        while (context instanceof InlineLambdaContext) {
            // If this is a lambda which will be inlined, skip its MethodContext and enclosing ClosureContext
            //noinspection ConstantConditions
            context = context.getParentContext().getParentContext();
        }
        return context;
    }

    @Nullable
    public static Type computeOuterClass(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull JvmDefaultMode jvmDefaultMode,
            @NotNull KtPureElement element,
            @NotNull CodegenContext<?> context
    ) {
        CodegenContext<? extends ClassOrPackageFragmentDescriptor> outermost = context.getClassOrPackageParentContext();
        if (outermost instanceof ClassContext) {
            ClassDescriptor classDescriptor = ((ClassContext) outermost).getContextDescriptor();
            if (context instanceof MethodContext) {
                FunctionDescriptor functionDescriptor = ((MethodContext) context).getFunctionDescriptor();
                if (isInterface(functionDescriptor.getContainingDeclaration()) &&
                    !JvmAnnotationUtilKt.isCompiledToJvmDefault(functionDescriptor, jvmDefaultMode)
                ) {
                    return typeMapper.mapDefaultImpls(classDescriptor);
                }
            }
            return typeMapper.mapClass(classDescriptor);
        }
        else if (outermost instanceof MultifileClassFacadeContext || outermost instanceof DelegatingToPartContext) {
            Type implementationOwnerType = CodegenContextUtil.getImplementationOwnerClassType(outermost);
            if (implementationOwnerType != null) {
                return implementationOwnerType;
            }
            else {
                return Type.getObjectType(JvmFileClassUtil.getFileClassInternalName(element.getContainingKtFile()));
            }
        }

        return null;
    }

    @Nullable
    public static Method computeEnclosingMethod(@NotNull KotlinTypeMapper typeMapper, @NotNull CodegenContext context) {
        if (context instanceof MethodContext) {
            FunctionDescriptor functionDescriptor = ((MethodContext) context).getFunctionDescriptor();
            if ("<clinit>".equals(functionDescriptor.getName().asString())) {
                return null;
            }

            if (((MethodContext) context).isDefaultFunctionContext()) {
                return typeMapper.mapDefaultMethod(functionDescriptor, context.getContextKind());
            }

            return typeMapper.mapAsmMethod(functionDescriptor, context.getContextKind());

        }
        return null;
    }

    @NotNull
    public NameGenerator getInlineNameGenerator() {
        if (inlineNameGenerator == null) {
            String prefix = getInlineName(context, typeMapper);
            inlineNameGenerator = new NameGenerator(prefix);
        }
        return inlineNameGenerator;
    }

    @NotNull
    public final ExpressionCodegen createOrGetClInitCodegen() {
        if (clInit == null) {
            DeclarationDescriptor contextDescriptor = context.getContextDescriptor();
            SimpleFunctionDescriptorImpl clInitDescriptor = createClInitFunctionDescriptor(contextDescriptor);
            MethodVisitor mv =
                    v.newMethod(JvmDeclarationOriginKt.OtherOrigin(contextDescriptor), ACC_STATIC, "<clinit>", "()V", null, null);
            clInit = new ExpressionCodegen(mv, new FrameMap(), Type.VOID_TYPE, context.intoFunction(clInitDescriptor), state, this);
        }
        return clInit;
    }

    @NotNull
    private SimpleFunctionDescriptorImpl createClInitFunctionDescriptor(@NotNull DeclarationDescriptor descriptor) {
        SimpleFunctionDescriptorImpl clInit = SimpleFunctionDescriptorImpl.create(descriptor, Annotations.Companion.getEMPTY(),
                Name.special("<clinit>"), SYNTHESIZED, KotlinSourceElementKt.toSourceElement(element));
        clInit.initialize(null, null, CollectionsKt.emptyList(), Collections.emptyList(), Collections.emptyList(),
                          DescriptorUtilsKt.getModule(descriptor).getBuiltIns().getUnitType(),
                          Modality.FINAL, DescriptorVisibilities.PRIVATE);
        return clInit;
    }

    private boolean nopSeparatorNeeded = false;

    private void generateNopSeparatorIfNeeded(NotNullLazyValue<ExpressionCodegen> codegen) {
        if (nopSeparatorNeeded) {
            nopSeparatorNeeded = false;
            codegen.invoke().v.nop();
        }
    }

    protected void generateInitializers(@NotNull Function0<ExpressionCodegen> createCodegen) {
        NotNullLazyValue<ExpressionCodegen> codegen = LockBasedStorageManager.NO_LOCKS.createLazyValue(createCodegen);

        List<KtDeclaration> declarations = ((KtDeclarationContainer) element).getDeclarations();
        for (int i = 0; i < declarations.size(); i++) {
            KtDeclaration declaration = declarations.get(i);
            if (declaration instanceof KtProperty) {
                if (shouldInitializeProperty((KtProperty) declaration)) {
                    generateNopSeparatorIfNeeded(codegen);
                    initializeProperty(codegen.invoke(), (KtProperty) declaration);
                }
            }
            else if (declaration instanceof KtDestructuringDeclaration) {
                generateNopSeparatorIfNeeded(codegen);
                codegen.invoke().initializeDestructuringDeclaration((KtDestructuringDeclaration) declaration, true);
            }
            else if (declaration instanceof KtAnonymousInitializer) {
                KtExpression body = ((KtAnonymousInitializer) declaration).getBody();
                if (body != null) {
                    generateNopSeparatorIfNeeded(codegen);

                    ExpressionCodegen expressionCodegen = codegen.invoke();
                    Type bodyExpressionType = Type.VOID_TYPE;
                    if (i == declarations.size() - 1 && this instanceof ScriptCodegen) {
                        bodyExpressionType = expressionCodegen.expressionType(body);
                    }

                    if (declaration instanceof KtClassInitializer) {
                        KtClassInitializer classInitializer = (KtClassInitializer) declaration;
                        expressionCodegen.markLineNumber(classInitializer.getInitKeyword(), true);
                        expressionCodegen.v.nop();
                    }

                    expressionCodegen.gen(body, bodyExpressionType);
                    if (declaration instanceof KtClassInitializer) {
                        expressionCodegen.markLineNumber(declaration, true);
                        nopSeparatorNeeded = true;
                    }
                }
            }
        }
    }

    // Requires public access, because it is used by serialization plugin to generate initializer in synthetic constructor
    public void initializeProperty(@NotNull ExpressionCodegen codegen, @NotNull KtProperty property) {
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        KtExpression initializer = property.getDelegateExpressionOrInitializer();
        assert initializer != null : "shouldInitializeProperty must return false if initializer is null";

        StackValue.Property propValue = codegen.intermediateValueForProperty(
                propertyDescriptor, true, false, null, true, StackValue.LOCAL_0, null, false
        );

        if (property.getDelegateExpression() == null) {
            propValue.store(codegen.gen(initializer), codegen.v);
        }
        else {
            StackValue.Property delegate = propValue.getDelegateOrNull();
            assert delegate != null : "No delegate for delegated property: " + propertyDescriptor;

            ResolvedCall<FunctionDescriptor> provideDelegateResolvedCall =
                    bindingContext.get(PROVIDE_DELEGATE_RESOLVED_CALL, propertyDescriptor);

            if (provideDelegateResolvedCall == null) {
                delegate.store(codegen.gen(initializer), codegen.v);
            }
            else {
                StackValue provideDelegateReceiver = codegen.gen(initializer);

                StackValue delegateValue = PropertyCodegen.invokeDelegatedPropertyConventionMethod(
                        codegen, provideDelegateResolvedCall, provideDelegateReceiver, propertyDescriptor
                );

                delegate.store(delegateValue, codegen.v);
            }
        }
    }

    // Public accessible for serialization plugin to check whether call to initializeProperty(..) is legal.
    public boolean shouldInitializeProperty(@NotNull KtProperty property) {
        if (!property.hasDelegateExpressionOrInitializer()) return false;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(VARIABLE, property);
        assert propertyDescriptor != null;

        if (propertyDescriptor.isConst()) {
            //const initializer always inlined
            return false;
        }

        KtExpression initializer = property.getInitializer();

        ConstantValue<?> initializerValue =
                initializer != null ? ExpressionCodegen.getCompileTimeConstant(initializer, bindingContext, state.getShouldInlineConstVals()) : null;
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
                if (type == Type.DOUBLE_TYPE && value.equals(0.0)) {
                    return true;
                }
                if (type == Type.FLOAT_TYPE && value.equals(0.0f)) {
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
        List<VariableDescriptorWithAccessors> delegatedProperties = bindingContext.get(CodegenBinding.DELEGATED_PROPERTIES_WITH_METADATA, thisAsmType);
        if (delegatedProperties == null || delegatedProperties.isEmpty()) return;

        int additionalFlags = context.getContextKind() != OwnerKind.DEFAULT_IMPLS && isInterface(context.getContextDescriptor()) ? ACC_PUBLIC : 0;
        v.newField(NO_ORIGIN, ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC | additionalFlags, JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME,
                   "[" + K_PROPERTY_TYPE, null, null);

        if (!state.getClassBuilderMode().generateBodies) return;

        InstructionAdapter iv = createOrGetClInitCodegen().v;
        iv.iconst(delegatedProperties.size());
        iv.newarray(K_PROPERTY_TYPE);

        for (int i = 0, size = delegatedProperties.size(); i < size; i++) {
            iv.dup();
            iv.iconst(i);
            generatePropertyReference(iv, delegatedProperties.get(i), state);
            iv.astore(K_PROPERTY_TYPE);
        }

        iv.putstatic(thisAsmType.getInternalName(), JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME, "[" + K_PROPERTY_TYPE);
    }

    public static void generatePropertyReference(
            @NotNull InstructionAdapter iv,
            @NotNull VariableDescriptorWithAccessors property,
            @NotNull GenerationState state
    ) {
        int receiverCount = (property.getDispatchReceiverParameter() != null ? 1 : 0) +
                            (property.getExtensionReceiverParameter() != null ? 1 : 0);
        Type implType = property.isVar() ? MUTABLE_PROPERTY_REFERENCE_IMPL[receiverCount] : PROPERTY_REFERENCE_IMPL[receiverCount];
        iv.anew(implType);
        iv.dup();

        List<Type> superCtorArgTypes = new ArrayList<>();
        if (state.getGenerateOptimizedCallableReferenceSuperClasses()) {
            CallableReferenceUtilKt.generateCallableReferenceDeclarationContainerClass(iv, property, state);
            superCtorArgTypes.add(JAVA_CLASS_TYPE);
        } else {
            // TODO: generate the container once and save to a local field instead (KT-10495)
            CallableReferenceUtilKt.generateCallableReferenceDeclarationContainer(iv, property, state);
            superCtorArgTypes.add(K_DECLARATION_CONTAINER_TYPE);
        }

        iv.aconst(property.getName().asString());
        CallableReferenceUtilKt.generatePropertyReferenceSignature(iv, property, state);
        superCtorArgTypes.add(JAVA_STRING_TYPE);
        superCtorArgTypes.add(JAVA_STRING_TYPE);

        if (state.getGenerateOptimizedCallableReferenceSuperClasses()) {
            iv.aconst(CallableReferenceUtilKt.getCallableReferenceTopLevelFlag(property));
            superCtorArgTypes.add(Type.INT_TYPE);
        }

        iv.invokespecial(
                implType.getInternalName(), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, superCtorArgTypes.toArray(new Type[0])), false
        );
        Method wrapper = PropertyReferenceCodegen.getWrapperMethodForPropertyReference(property, receiverCount);
        iv.invokestatic(REFLECTION, wrapper.getName(), wrapper.getDescriptor(), false);

        StackValue.onStack(implType).put(K_PROPERTY_TYPE, iv);
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
            // note: this is used in InlineCodegen and the element is always physical (KtElement) there
            sourceMapper = new SourceMapper(SourceInfo.Companion.createFromPsi((KtElement)element, getClassName()));
        }
        return sourceMapper;
    }

    protected void initDefaultSourceMappingIfNeeded() {
        if (state.isInlineDisabled()) return;

        CodegenContext parentContext = context.getParentContext();
        while (parentContext != null) {
            if (parentContext.isInlineMethodContext()) {
                alwaysWriteSourceMap = true;
                return;
            }
            parentContext = parentContext.getParentContext();
        }
    }

    protected void generateConstInstance(@NotNull Type thisAsmType, @NotNull Type fieldAsmType) {
        v.newField(
                JvmDeclarationOriginKt.OtherOriginFromPure(element), ACC_STATIC | ACC_FINAL | ACC_PUBLIC, JvmAbi.INSTANCE_FIELD,
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

    protected final void generateSyntheticAccessors() {
        for (AccessorForCallableDescriptor<?> accessor : ((CodegenContext<?>) context).getAccessors()) {
            boolean compiledToJvmDefault =
                    JvmAnnotationUtilKt.isCompiledToJvmDefault(accessor.getCalleeDescriptor(), state.getJvmDefaultMode());
            OwnerKind kind = context.getContextKind();

            if (!isInterface(context.getContextDescriptor()) ||
                (compiledToJvmDefault && kind == OwnerKind.IMPLEMENTATION) ||
                (!compiledToJvmDefault && kind == OwnerKind.DEFAULT_IMPLS)) {
                generateSyntheticAccessor(accessor);
            }
        }

        Collection<AccessorForCompanionObjectInstanceFieldDescriptor> accessorsForCompanionObjects =
                context.getRequiredAccessorsForCompanionObjects();
        if (!accessorsForCompanionObjects.isEmpty()) {
            List<AccessorForCompanionObjectInstanceFieldDescriptor> sortedAccessorsForCompanionObjects =
                    new ArrayList<>(accessorsForCompanionObjects);
            sortedAccessorsForCompanionObjects.sort(BY_COMPANION_FQN);

            for (AccessorForCompanionObjectInstanceFieldDescriptor accessor : sortedAccessorsForCompanionObjects) {
                generateSyntheticAccessorForCompanionObject(accessor);
            }
        }
    }

    private final Comparator<AccessorForCompanionObjectInstanceFieldDescriptor> BY_COMPANION_FQN =
            (o1, o2) -> {
                String companionFQN1 = DescriptorUtils.getFqName(o1.getCompanionObjectDescriptor()).asString();
                String companionFQN2 = DescriptorUtils.getFqName(o2.getCompanionObjectDescriptor()).asString();
                return companionFQN1.compareTo(companionFQN2);
            };

    private void generateSyntheticAccessorForCompanionObject(@NotNull AccessorForCompanionObjectInstanceFieldDescriptor accessor) {
        ClassDescriptor companionObjectDescriptor = accessor.getCompanionObjectDescriptor();
        DeclarationDescriptor hostClassDescriptor = companionObjectDescriptor.getContainingDeclaration();
        assert hostClassDescriptor instanceof ClassDescriptor : "Class descriptor expected: " + hostClassDescriptor;
        functionCodegen.generateMethod(
                Synthetic(null, companionObjectDescriptor),
                accessor,
                new FunctionGenerationStrategy.CodegenBased(state) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        Type companionObjectType = typeMapper.mapClass(companionObjectDescriptor);
                        StackValue.singleton(companionObjectDescriptor, typeMapper).put(companionObjectType, codegen.v);
                        codegen.v.areturn(companionObjectType);
                    }
                }
        );
    }

    private void generateSyntheticAccessor(@NotNull AccessorForCallableDescriptor<?> accessorForCallableDescriptor) {
        if (accessorForCallableDescriptor instanceof FunctionDescriptor) {
            FunctionDescriptor accessor = (FunctionDescriptor) accessorForCallableDescriptor;
            FunctionDescriptor original = (FunctionDescriptor) accessorForCallableDescriptor.getCalleeDescriptor();
            functionCodegen.generateMethod(
                    Synthetic(null, original), accessor,
                    new FunctionGenerationStrategy.CodegenBased(state) {
                        @Override
                        public boolean skipNotNullAssertionsForParameters() {
                            return true;
                        }

                        @Override
                        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                            markLineNumberForElement(element.getPsiOrParent(), codegen.v);
                            if (accessorForCallableDescriptor.getAccessorKind() == AccessorKind.JVM_DEFAULT_COMPATIBILITY) {
                                FunctionDescriptor descriptor = unwrapFakeOverrideToAnyDeclaration(original).getOriginal();
                                if (descriptor != original) {
                                    descriptor = descriptor
                                            .copy(original.getContainingDeclaration(), descriptor.getModality(), descriptor.getVisibility(),
                                                  descriptor.getKind(), false);
                                }
                                generateMethodCallTo(descriptor, accessor, codegen.v).coerceTo(signature.getReturnType(), null, codegen.v);
                            }
                            else {
                                generateMethodCallTo(original, accessor, codegen.v).coerceTo(signature.getReturnType(), null, codegen.v);
                            }

                            codegen.v.areturn(signature.getReturnType());
                        }
                    }
            );
        }
        else if (accessorForCallableDescriptor instanceof AccessorForPropertyDescriptor) {
            AccessorForPropertyDescriptor accessor = (AccessorForPropertyDescriptor) accessorForCallableDescriptor;
            PropertyDescriptor original = accessor.getCalleeDescriptor();

            class PropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased {
                private final PropertyAccessorDescriptor callableDescriptor;

                private PropertyAccessorStrategy(@NotNull PropertyAccessorDescriptor callableDescriptor) {
                    super(MemberCodegen.this.state);
                    this.callableDescriptor = callableDescriptor;
                }

                @Override
                public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                    if (accessorForCallableDescriptor.getAccessorKind() == AccessorKind.JVM_DEFAULT_COMPATIBILITY) {
                        markLineNumberForElement(element.getPsiOrParent(), codegen.v);
                        PropertyDescriptor descriptor = unwrapFakeOverrideToAnyDeclaration(original).getOriginal();
                        if (descriptor != original) {
                            descriptor = (PropertyDescriptor) descriptor
                                    .copy(original.getContainingDeclaration(), descriptor.getModality(), descriptor.getVisibility(),
                                          descriptor.getKind(), false);
                        }
                        boolean isGetter = callableDescriptor instanceof PropertyGetterDescriptor;
                        PropertyAccessorDescriptor originalAccessor = isGetter ? descriptor.getGetter(): descriptor.getSetter();
                        PropertyAccessorDescriptor accessorDescriptor = isGetter ? accessor.getGetter() : accessor.getSetter();
                        generateMethodCallTo(originalAccessor, accessorDescriptor, codegen.v)
                                .coerceTo(signature.getReturnType(), null, codegen.v);
                        codegen.v.areturn(signature.getReturnType());
                        return;
                    }

                    AccessorKind fieldAccessorKind = accessor instanceof AccessorForPropertyBackingField
                                                          ? accessor.getAccessorKind() : null;
                    boolean syntheticBackingField = fieldAccessorKind == AccessorKind.FIELD_FROM_LOCAL;
                    boolean forceFieldForCompanionProperty = DescriptorsJvmAbiUtil.isPropertyWithBackingFieldInOuterClass(original) &&
                                                             !isCompanionObject(accessor.getContainingDeclaration());
                    boolean forceField = forceFieldForCompanionProperty ||
                                         syntheticBackingField ||
                                         original.getVisibility() == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY;
                    StackValue property = codegen.intermediateValueForProperty(
                            original, forceField, syntheticBackingField, accessor.getSuperCallTarget(),
                            forceFieldForCompanionProperty, StackValue.none(), null,
                            fieldAccessorKind == AccessorKind.LATEINIT_INTRINSIC
                    );

                    InstructionAdapter iv = codegen.v;

                    markLineNumberForElement(element.getPsiOrParent(), iv);

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
                        property.store(StackValue.onStack(property.type, property.kotlinType), iv, true);
                    }

                    iv.areturn(signature.getReturnType());
                }

                @Override
                public boolean skipNotNullAssertionsForParameters() {
                    return true;
                }
            }

            if (accessor.isWithSyntheticGetterAccessor()) {
                PropertyGetterDescriptor getter = accessor.getGetter();
                assert getter != null;
                functionCodegen.generateMethod(Synthetic(null, original.getGetter() != null ? original.getGetter() : original),
                                               getter, new PropertyAccessorStrategy(getter));
            }

            if (accessor.isVar() && accessor.isWithSyntheticSetterAccessor()) {
                if (isProhibitedAccessorToPrivatePropertySetter(original)) return;

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

    private boolean isProhibitedAccessorToPrivatePropertySetter(PropertyDescriptor original) {
        // Property setter might be less visible than the property itself.
        // We can generate accessor for a private setter only if we are in the same class
        // or in a class for the containing companion object (see KT-22465).
        // NB we don't allow private or protected interface members in Kotlin (so far),
        // so a property that might require an accessor can't be declared in an interface.
        PropertyDescriptor overriddenProperty = DescriptorUtils.unwrapFakeOverride(original);
        PropertySetterDescriptor overriddenSetter = overriddenProperty.getSetter();
        if (overriddenSetter == null || !DescriptorVisibilities.isPrivate(overriddenSetter.getVisibility())) return false;
        DeclarationDescriptor contextDescriptor = context.getContextDescriptor();
        return contextDescriptor != overriddenProperty.getContainingDeclaration() &&
               contextDescriptor != DescriptorUtils.getContainingClass(overriddenProperty);
    }

    protected StackValue generateMethodCallTo(
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable FunctionDescriptor accessorDescriptor,
            @NotNull InstructionAdapter iv
    ) {
        CallableMethod callableMethod = typeMapper.mapToCallableMethod(
                functionDescriptor,
                accessorDescriptor instanceof AccessorForCallableDescriptor &&
                (((AccessorForCallableDescriptor) accessorDescriptor).getSuperCallTarget() != null ||
                 ((AccessorForCallableDescriptor) accessorDescriptor).getAccessorKind() == AccessorKind.JVM_DEFAULT_COMPATIBILITY),
                (accessorDescriptor != null && TypeMapperUtilsKt.isInlineClassConstructorAccessor(accessorDescriptor)
                 ? OwnerKind.ERASED_INLINE_CLASS : null)
        );

        boolean isJvmStaticInObjectOrClass = CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(functionDescriptor);
        boolean hasDispatchReceiver = !isStaticDeclaration(functionDescriptor) &&
                                      !isNonDefaultInterfaceMember(functionDescriptor, state.getJvmDefaultMode()) &&
                                      !isJvmStaticInObjectOrClass &&
                                      !InlineClassesUtilsKt.isInlineClass(functionDescriptor.getContainingDeclaration());
        boolean accessorIsConstructor = accessorDescriptor instanceof AccessorForConstructorDescriptor;

        ReceiverParameterDescriptor dispatchReceiver = functionDescriptor.getDispatchReceiverParameter();
        Type dispatchReceiverType = dispatchReceiver != null && !accessorIsConstructor
                                    ? typeMapper.mapType(dispatchReceiver.getType())
                                    : AsmTypes.OBJECT_TYPE;

        int accessorParam = (hasDispatchReceiver && !accessorIsConstructor) ? 1 : 0;
        int reg = hasDispatchReceiver ? dispatchReceiverType.getSize() : 0;
        if (!accessorIsConstructor && functionDescriptor instanceof ConstructorDescriptor) {
            iv.anew(callableMethod.getOwner());
            iv.dup();
            reg = 0;
            accessorParam = 0;
        }
        else if (KotlinTypeMapper.isAccessor(accessorDescriptor) && (hasDispatchReceiver || accessorIsConstructor)) {
            if (!isJvmStaticInObjectOrClass) {
                iv.load(0, dispatchReceiverType);
            }
        }

        Type[] calleeParameterTypes = callableMethod.getParameterTypes();
        Type[] accessorParameterTypes = accessorDescriptor != null
                                        ? typeMapper.mapToCallableMethod(accessorDescriptor, false).getParameterTypes()
                                        : calleeParameterTypes;
        for (Type calleeArgType: calleeParameterTypes) {
            if (AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.equals(calleeArgType)) {
                iv.aconst(null);
            }
            else {
                Type accessorParameterType = accessorParameterTypes[accessorParam];
                iv.load(reg, accessorParameterType);
                StackValue.coerce(accessorParameterType, calleeArgType, iv);
                reg += accessorParameterType.getSize();
            }
            accessorParam++;
        }

        callableMethod.genInvokeInstruction(iv);

        return StackValue.onStack(callableMethod.getReturnType(), functionDescriptor.getReturnType());
    }

    public void generateAssertField() {
        if (jvmAssertFieldGenerated) return;
        AssertCodegenUtilKt.generateAssertionsDisabledFieldInitialization(v, createOrGetClInitCodegen().v, v.getThisName());
        jvmAssertFieldGenerated = true;
    }

}
