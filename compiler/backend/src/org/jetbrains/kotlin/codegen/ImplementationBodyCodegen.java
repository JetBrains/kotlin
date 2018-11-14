/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap;
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap.PlatformMutabilityMapping;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isGenericToArray;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isNonGenericToArray;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtils2Kt.initDefaultSourceMappingIfNeeded;
import static org.jetbrains.kotlin.load.java.JvmAbi.*;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getNotNull;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.types.Variance.INVARIANT;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isLocalFunction;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;
import static org.jetbrains.org.objectweb.asm.Type.getObjectType;

public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private static final String ENUM_VALUES_FIELD_NAME = "$VALUES";
    private Type superClassAsmType;
    @NotNull
    private SuperClassInfo superClassInfo;
    private final Type classAsmType;
    private final boolean isLocal;

    private List<PropertyAndDefaultValue> companionObjectPropertiesToCopy;

    private final DelegationFieldsInfo delegationFieldsInfo;

    private final List<Function2<ImplementationBodyCodegen, ClassBuilder, Unit>> additionalTasks = new ArrayList<>();

    private final DescriptorSerializer serializer;

    private final ConstructorCodegen constructorCodegen;

    public ImplementationBodyCodegen(
            @NotNull KtPureClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen,
            boolean isLocal
    ) {
        super(aClass, context, v, state, parentCodegen);
        this.classAsmType = getObjectType(typeMapper.classInternalName(descriptor));
        this.isLocal = isLocal;

        this.delegationFieldsInfo =
                new DelegationFieldsInfo(classAsmType, descriptor, state, bindingContext)
                        .getDelegationFieldsInfo(myClass.getSuperTypeListEntries());

        JvmSerializerExtension extension = new JvmSerializerExtension(v.getSerializationBindings(), state);
        this.serializer = DescriptorSerializer.create(
                descriptor, extension,
                parentCodegen instanceof ImplementationBodyCodegen
                ? ((ImplementationBodyCodegen) parentCodegen).serializer
                : DescriptorSerializer.createTopLevel(extension)
        );

        this.constructorCodegen = new ConstructorCodegen(
                descriptor, context, functionCodegen, this,
                this, state, kind, v, classAsmType, myClass, bindingContext
        );
    }

    @Override
    protected void generateDeclaration() {
        superClassInfo = SuperClassInfo.getSuperClassInfo(descriptor, typeMapper);
        superClassAsmType = superClassInfo.getType();

        JvmClassSignature signature = signature();

        boolean isAbstract = false;
        boolean isInterface = false;
        boolean isFinal = false;
        boolean isAnnotation = false;
        boolean isEnum = false;

        ClassKind kind = descriptor.getKind();

        Modality modality = descriptor.getModality();

        if (modality == Modality.ABSTRACT || modality == Modality.SEALED) {
            isAbstract = true;
        }

        if (kind == ClassKind.INTERFACE) {
            isAbstract = true;
            isInterface = true;
        }
        else if (kind == ClassKind.ANNOTATION_CLASS) {
            isAbstract = true;
            isInterface = true;
            isAnnotation = true;
        }
        else if (kind == ClassKind.ENUM_CLASS) {
            isAbstract = hasAbstractMembers(descriptor);
            isEnum = true;
        }

        if (modality != Modality.OPEN && !isAbstract) {
            isFinal = kind == ClassKind.OBJECT ||
                      // Light-class mode: Do not make enum classes final since PsiClass corresponding to enum is expected to be inheritable from
                      !(kind == ClassKind.ENUM_CLASS && !state.getClassBuilderMode().generateBodies);
        }

        int access = 0;

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            // !ClassBuilderMode.generateBodies means we are generating light classes & looking at a nested or inner class
            // Light class generation is implemented so that Cls-classes only read bare code of classes,
            // without knowing whether these classes are inner or not (see ClassStubBuilder.EMPTY_STRATEGY)
            // Thus we must write full accessibility flags on inner classes in this mode
            access |= getVisibilityAccessFlag(descriptor);
            // Same for STATIC
            if (!descriptor.isInner()) {
                access |= ACC_STATIC;
            }
        }
        else {
            access |= getVisibilityAccessFlagForClass(descriptor);
        }
        if (isAbstract) {
            access |= ACC_ABSTRACT;
        }
        if (isInterface) {
            access |= ACC_INTERFACE; // ACC_SUPER
        }
        else {
            access |= ACC_SUPER;
        }
        if (isFinal) {
            access |= ACC_FINAL;
        }
        if (isAnnotation) {
            access |= ACC_ANNOTATION;
        }
        if (KotlinBuiltIns.isDeprecated(descriptor)) {
            access |= ACC_DEPRECATED;
        }
        if (isEnum) {
            for (KtDeclaration declaration : myClass.getDeclarations()) {
                if (declaration instanceof KtEnumEntry) {
                    if (enumEntryNeedSubclass(bindingContext, (KtEnumEntry) declaration)) {
                        access &= ~ACC_FINAL;
                    }
                }
            }
            access |= ACC_ENUM;
        }

        v.defineClass(
                myClass.getPsiOrParent(),
                state.getClassFileVersion(),
                access,
                signature.getName(),
                signature.getJavaGenericSignature(),
                signature.getSuperclassName(),
                ArrayUtil.toStringArray(signature.getInterfaces())
        );

        v.visitSource(myClass.getContainingKtFile().getName(), null);

        initDefaultSourceMappingIfNeeded(context, this, state);

        writeEnclosingMethod();

        AnnotationCodegen.forClass(v.getVisitor(), this, state).genAnnotations(descriptor, null);

        generateEnumEntries();
    }

    @Override
    protected void generateDefaultImplsIfNeeded() {
        if (isInterface(descriptor) && !isLocal) {
            Type defaultImplsType = state.getTypeMapper().mapDefaultImpls(descriptor);
            ClassBuilder defaultImplsBuilder =
                    state.getFactory().newVisitor(JvmDeclarationOriginKt.DefaultImpls(myClass.getPsiOrParent(), descriptor), defaultImplsType, myClass.getContainingKtFile());

            CodegenContext parentContext = context.getParentContext();
            assert parentContext != null : "Parent context of interface declaration should not be null";

            ClassContext defaultImplsContext = parentContext.intoDefaultImplsClass(descriptor, (ClassContext) context, state);
            new InterfaceImplBodyCodegen(myClass, defaultImplsContext, defaultImplsBuilder, state, this).generate();
        }
    }

    @Override
    protected void generateErasedInlineClassIfNeeded() {
        if (!(myClass instanceof KtClass)) return;
        if (!descriptor.isInline()) return;

        ClassContext erasedInlineClassContext = context.intoWrapperForErasedInlineClass(descriptor, state);
        new ErasedInlineClassBodyCodegen((KtClass) myClass, erasedInlineClassContext, v, state, this).generate();
    }

    @Override
    protected void generateUnboxMethodForInlineClass() {
        if (!(myClass instanceof KtClass)) return;
        if (!descriptor.isInline()) return;

        Type ownerType = typeMapper.mapClass(descriptor);
        ValueParameterDescriptor inlinedValue = InlineClassesUtilsKt.underlyingRepresentation(this.descriptor);
        if (inlinedValue == null) return;

        Type valueType = typeMapper.mapType(inlinedValue.getType());
        SimpleFunctionDescriptor functionDescriptor = InlineClassDescriptorResolver.createUnboxFunctionDescriptor(this.descriptor);
        assert functionDescriptor != null : "FunctionDescriptor for unbox method should be not null during codegen";

        functionCodegen.generateMethod(
                JvmDeclarationOriginKt.UnboxMethodOfInlineClass(functionDescriptor), functionDescriptor,
                new FunctionGenerationStrategy.CodegenBased(state) {
                    @Override
                    public void doGenerateBody(
                            @NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature
                    ) {
                        InstructionAdapter iv = codegen.v;
                        iv.load(0, OBJECT_TYPE);
                        iv.getfield(ownerType.getInternalName(), inlinedValue.getName().asString(), valueType.getDescriptor());
                        iv.areturn(valueType);
                    }
                }
        );
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        ProtoBuf.Class classProto = serializer.classProto(descriptor).build();

        WriteAnnotationUtilKt.writeKotlinMetadata(v, state, KotlinClassHeader.Kind.CLASS, 0, av -> {
            writeAnnotationData(av, serializer, classProto);
            return Unit.INSTANCE;
        });
    }

    private void writeEnclosingMethod() {
        // Do not emit enclosing method in "light-classes mode" since currently we generate local light classes as if they're top level
        if (!state.getClassBuilderMode().generateBodies) {
            return;
        }

        //JVMS7: A class must have an EnclosingMethod attribute if and only if it is a local class or an anonymous class.
        if (isAnonymousObject(descriptor) || !(descriptor.getContainingDeclaration() instanceof ClassOrPackageFragmentDescriptor)) {
            writeOuterClassAndEnclosingMethod();
        }
    }

    private static final Map<FqName, String> KOTLIN_MARKER_INTERFACES = new HashMap<>();

    static {
        for (PlatformMutabilityMapping platformMutabilityMapping : JavaToKotlinClassMap.INSTANCE.getMutabilityMappings()) {
            KOTLIN_MARKER_INTERFACES.put(
                    platformMutabilityMapping.getKotlinReadOnly().asSingleFqName(),
                    "kotlin/jvm/internal/markers/KMappedMarker");

            ClassId mutableClassId = platformMutabilityMapping.getKotlinMutable();
            KOTLIN_MARKER_INTERFACES.put(
                    mutableClassId.asSingleFqName(),
                    "kotlin/jvm/internal/markers/K" + mutableClassId.getRelativeClassName().asString()
                            .replace("MutableEntry", "Entry") // kotlin.jvm.internal.markers.KMutableMap.Entry for some reason
                            .replace(".", "$")
            );
        }
    }

    @NotNull
    private JvmClassSignature signature() {
        return signature(descriptor, classAsmType, superClassInfo, typeMapper);
    }

    @NotNull
    public static JvmClassSignature signature(
            @NotNull ClassDescriptor descriptor,
            @NotNull Type classAsmType,
            @NotNull SuperClassInfo superClassInfo,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);

        typeMapper.writeFormalTypeParameters(descriptor.getDeclaredTypeParameters(), sw);

        sw.writeSuperclass();
        if (superClassInfo.getKotlinType() == null) {
            sw.writeClassBegin(superClassInfo.getType());
            sw.writeClassEnd();
        }
        else {
            typeMapper.mapSupertype(superClassInfo.getKotlinType(), sw);
        }
        sw.writeSuperclassEnd();

        LinkedHashSet<String> superInterfaces = new LinkedHashSet<>();
        Set<String> kotlinMarkerInterfaces = new LinkedHashSet<>();

        for (KotlinType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            if (isJvmInterface(supertype.getConstructor().getDeclarationDescriptor())) {
                FqName kotlinInterfaceName = DescriptorUtils.getFqName(supertype.getConstructor().getDeclarationDescriptor()).toSafe();

                sw.writeInterface();
                Type jvmInterfaceType = typeMapper.mapSupertype(supertype, sw);
                sw.writeInterfaceEnd();
                String jvmInterfaceInternalName = jvmInterfaceType.getInternalName();

                superInterfaces.add(jvmInterfaceInternalName);

                String kotlinMarkerInterfaceInternalName = KOTLIN_MARKER_INTERFACES.get(kotlinInterfaceName);
                if (kotlinMarkerInterfaceInternalName != null) {
                    if (typeMapper.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) {
                        sw.writeInterface();
                        Type kotlinCollectionType = typeMapper.mapType(supertype, sw, TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS);
                        sw.writeInterfaceEnd();
                        superInterfaces.add(kotlinCollectionType.getInternalName());
                    }

                    kotlinMarkerInterfaces.add(kotlinMarkerInterfaceInternalName);
                }
            }
        }

        for (String kotlinMarkerInterface : kotlinMarkerInterfaces) {
            sw.writeInterface();
            sw.writeAsmType(getObjectType(kotlinMarkerInterface));
            sw.writeInterfaceEnd();
        }

        superInterfaces.addAll(kotlinMarkerInterfaces);

        return new JvmClassSignature(classAsmType.getInternalName(), superClassInfo.getType().getInternalName(),
                                     new ArrayList<>(superInterfaces), sw.makeJavaGenericSignature());
    }

    @Override
    protected void generateSyntheticPartsBeforeBody() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType);
    }

    @Override
    protected void generateSyntheticPartsAfterBody() {
        generateFieldForSingleton();

        initializeObjects();

        generateCompanionObjectBackingFieldCopies();

        generateDelegatesToDefaultImpl();

        generateDelegates(delegationFieldsInfo);

        generateSyntheticAccessors();

        generateEnumMethods();

        generateFunctionsForDataClasses();

        generateFunctionsFromAnyForInlineClasses();

        if (state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES) {
            new CollectionStubMethodGenerator(typeMapper, descriptor).generate(functionCodegen, v);

            generateToArray();
        }


        if (context.closure != null)
            genClosureFields(context.closure, v, typeMapper, state.getLanguageVersionSettings());

        for (ExpressionCodegenExtension extension : ExpressionCodegenExtension.Companion.getInstances(state.getProject())) {
            if (state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES
                || extension.getShouldGenerateClassSyntheticPartsInLightClassesMode()
            ) {
                extension.generateClassSyntheticParts(this);
            }
        }
    }

    @Override
    protected void generateConstructors() {
        try {
            lookupConstructorExpressionsInClosureIfPresent();
            constructorCodegen.generatePrimaryConstructor(delegationFieldsInfo, superClassAsmType);
            if (!descriptor.isInline()) {
                for (ClassConstructorDescriptor secondaryConstructor : DescriptorUtilsKt.getSecondaryConstructors(descriptor)) {
                    constructorCodegen.generateSecondaryConstructor(secondaryConstructor, superClassAsmType);
                }
            }
        }
        catch (CompilationException | ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Error generating constructors of class " + myClass.getName() + " with kind " + kind, e);
        }
    }

    private void generateToArray() {
        if (descriptor.getKind() == ClassKind.INTERFACE) return;

        KotlinBuiltIns builtIns = DescriptorUtilsKt.getBuiltIns(descriptor);
        if (!isSubclass(descriptor, builtIns.getCollection())) return;

        if (CollectionsKt.any(DescriptorUtilsKt.getAllSuperclassesWithoutAny(descriptor),
                              classDescriptor -> !(classDescriptor instanceof JavaClassDescriptor) &&
                                                 isSubclass(classDescriptor, builtIns.getCollection()))) {
            return;
        }

        Collection<? extends SimpleFunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getContributedFunctions(
                Name.identifier("toArray"), NoLookupLocation.FROM_BACKEND
        );
        boolean hasGenericToArray = false;
        boolean hasNonGenericToArray = false;
        for (FunctionDescriptor function : functions) {
            hasGenericToArray |= isGenericToArray(function);
            hasNonGenericToArray |= isNonGenericToArray(function);
        }

        if (!hasNonGenericToArray) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, ACC_PUBLIC, "toArray", "()[Ljava/lang/Object;", null, null);

            InstructionAdapter iv = new InstructionAdapter(mv);
            mv.visitCode();

            iv.load(0, classAsmType);
            iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray", "(Ljava/util/Collection;)[Ljava/lang/Object;", false);
            iv.areturn(Type.getType("[Ljava/lang/Object;"));

            FunctionCodegen.endVisit(mv, "toArray", myClass);
        }

        if (!hasGenericToArray) {
            MethodVisitor mv = v.newMethod(
                    NO_ORIGIN, ACC_PUBLIC, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", "<T:Ljava/lang/Object;>([TT;)[TT;", null);

            InstructionAdapter iv = new InstructionAdapter(mv);
            mv.visitCode();

            iv.load(0, classAsmType);
            iv.load(1, Type.getType("[Ljava/lang/Object;"));

            iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray",
                            "(Ljava/util/Collection;[Ljava/lang/Object;)[Ljava/lang/Object;", false);
            iv.areturn(Type.getType("[Ljava/lang/Object;"));

            FunctionCodegen.endVisit(mv, "toArray", myClass);
        }
    }

    public static JvmKotlinType genPropertyOnStack(
            InstructionAdapter iv,
            MethodContext context,
            @NotNull PropertyDescriptor propertyDescriptor,
            Type classAsmType,
            int index,
            GenerationState state
    ) {
        iv.load(index, classAsmType);
        if (couldUseDirectAccessToProperty(propertyDescriptor, /* forGetter = */ true,
                                               /* isDelegated = */ false, context, state.getShouldInlineConstVals())) {
            KotlinType kotlinType = propertyDescriptor.getType();
            Type type = state.getTypeMapper().mapType(kotlinType);
            String fieldName = ((FieldOwnerContext) context.getParentContext()).getFieldName(propertyDescriptor, false);
            iv.getfield(classAsmType.getInternalName(), fieldName, type.getDescriptor());
            return new JvmKotlinType(type, kotlinType);
        }
        else {
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();

            //noinspection ConstantConditions
            Method method = state.getTypeMapper().mapAsmMethod(getter);
            iv.invokevirtual(classAsmType.getInternalName(), method.getName(), method.getDescriptor(), false);
            return new JvmKotlinType(method.getReturnType(), getter.getReturnType());
        }
    }

    private void generateFunctionsForDataClasses() {
        if (!descriptor.isData()) return;
        if (!(myClass instanceof KtClassOrObject)) return;
        new DataClassMethodGeneratorImpl((KtClassOrObject)myClass, bindingContext).generate();
    }

    private void generateFunctionsFromAnyForInlineClasses() {
        if (!descriptor.isInline()) return;
        if (!(myClass instanceof KtClassOrObject)) return;
        new FunctionsFromAnyGeneratorImpl(
                (KtClassOrObject) myClass, bindingContext, descriptor, classAsmType, context, v, state
        ).generate();
    }

    private class DataClassMethodGeneratorImpl extends DataClassMethodGenerator {
        private final FunctionsFromAnyGeneratorImpl functionsFromAnyGenerator;

        DataClassMethodGeneratorImpl(
                KtClassOrObject klass,
                BindingContext bindingContext
        ) {
            super(klass, bindingContext);
            this.functionsFromAnyGenerator = new FunctionsFromAnyGeneratorImpl(
                    klass, bindingContext, descriptor, classAsmType, ImplementationBodyCodegen.this.context, v, state
            );
        }

        @Override
        public void generateEqualsMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            functionsFromAnyGenerator.generateEqualsMethod(function, properties);
        }

        @Override
        public void generateHashCodeMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            functionsFromAnyGenerator.generateHashCodeMethod(function, properties);
        }

        @Override
        public void generateToStringMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            functionsFromAnyGenerator.generateToStringMethod(function, properties);
        }

        @Override
        public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter) {
            PsiElement originalElement = DescriptorToSourceUtils.descriptorToDeclaration(parameter);
            functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(originalElement, function), function, new FunctionGenerationStrategy() {
                @Override
                public void generateBody(
                        @NotNull MethodVisitor mv,
                        @NotNull FrameMap frameMap,
                        @NotNull JvmMethodSignature signature,
                        @NotNull MethodContext context,
                        @NotNull MemberCodegen<?> parentCodegen
                ) {
                    Type componentType = signature.getReturnType();
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    if (!componentType.equals(Type.VOID_TYPE)) {
                        PropertyDescriptor property =
                                bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, descriptorToDeclaration(parameter));
                        assert property != null : "Property descriptor is not found for primary constructor parameter: " + parameter;

                        JvmKotlinType propertyType = genPropertyOnStack(
                                iv, context, property, ImplementationBodyCodegen.this.classAsmType, 0, state
                        );
                        StackValue.coerce(propertyType.getType(), componentType, iv);
                    }
                    iv.areturn(componentType);
                }

                @Override
                public boolean skipNotNullAssertionsForParameters() {
                    return false;
                }
            });
        }

        @Override
        public void generateCopyFunction(
                @NotNull FunctionDescriptor function,
                @NotNull List<? extends KtParameter> constructorParameters
        ) {
            Type thisDescriptorType = typeMapper.mapType(descriptor);

            functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOriginFromPure(myClass, function), function, new FunctionGenerationStrategy() {
                @Override
                public void generateBody(
                        @NotNull MethodVisitor mv,
                        @NotNull FrameMap frameMap,
                        @NotNull JvmMethodSignature signature,
                        @NotNull MethodContext context,
                        @NotNull MemberCodegen<?> parentCodegen
                ) {
                    InstructionAdapter iv = new InstructionAdapter(mv);

                    iv.anew(thisDescriptorType);
                    iv.dup();

                    ConstructorDescriptor constructor = getPrimaryConstructorOfDataClass(descriptor);
                    assert function.getValueParameters().size() == constructor.getValueParameters().size() :
                            "Number of parameters of copy function and constructor are different. " +
                            "Copy: " + function.getValueParameters().size() + ", " +
                            "constructor: " + constructor.getValueParameters().size();

                    MutableClosure closure = ImplementationBodyCodegen.this.context.closure;
                    if (closure != null) {
                        pushCapturedFieldsOnStack(iv, closure);
                    }

                    int parameterIndex = 1; // localVariable 0 = this
                    for (ValueParameterDescriptor parameterDescriptor : function.getValueParameters()) {
                        Type type = typeMapper.mapType(parameterDescriptor.getType());
                        iv.load(parameterIndex, type);
                        parameterIndex += type.getSize();
                    }

                    Method constructorAsmMethod = typeMapper.mapAsmMethod(constructor);
                    iv.invokespecial(thisDescriptorType.getInternalName(), "<init>", constructorAsmMethod.getDescriptor(), false);

                    iv.areturn(thisDescriptorType);
                }

                @Override
                public boolean skipNotNullAssertionsForParameters() {
                    return false;
                }

                private void pushCapturedFieldsOnStack(InstructionAdapter iv, MutableClosure closure) {
                    ClassDescriptor captureThis = closure.getCapturedOuterClassDescriptor();
                    if (captureThis != null) {
                        iv.load(0, classAsmType);
                        Type type = typeMapper.mapType(captureThis);
                        iv.getfield(classAsmType.getInternalName(), CAPTURED_THIS_FIELD, type.getDescriptor());
                    }

                    KotlinType captureReceiver = closure.getCapturedReceiverFromOuterContext();
                    if (captureReceiver != null) {
                        iv.load(0, classAsmType);
                        Type type = typeMapper.mapType(captureReceiver);
                        String fieldName = closure.getCapturedReceiverFieldName(bindingContext, state.getLanguageVersionSettings());
                        iv.getfield(classAsmType.getInternalName(), fieldName, type.getDescriptor());
                    }

                    for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closure.getCaptureVariables().entrySet()) {
                        DeclarationDescriptor declarationDescriptor = entry.getKey();
                        EnclosedValueDescriptor enclosedValueDescriptor = entry.getValue();
                        StackValue capturedValue = enclosedValueDescriptor.getInstanceValue();
                        Type sharedVarType = typeMapper.getSharedVarType(declarationDescriptor);
                        if (sharedVarType == null) {
                            sharedVarType = typeMapper.mapType((VariableDescriptor) declarationDescriptor);
                        }
                        capturedValue.put(sharedVarType, iv);
                    }
                }
            });

            functionCodegen.generateDefaultIfNeeded(
                    context.intoFunction(function), function, OwnerKind.IMPLEMENTATION,
                    (valueParameter, codegen) -> {
                        assert ((ClassDescriptor) function.getContainingDeclaration()).isData()
                                : "Function container must have [data] modifier: " + function;
                        PropertyDescriptor property = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter);
                        assert property != null : "Copy function doesn't correspond to any property: " + function;
                        return codegen.intermediateValueForProperty(property, false, null, StackValue.LOCAL_0);
                    },
                    null
            );
        }
    }

    @NotNull
    private static ConstructorDescriptor getPrimaryConstructorOfDataClass(@NotNull ClassDescriptor classDescriptor) {
        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        assert constructor != null : "Data class must have primary constructor: " + classDescriptor;
        return constructor;
    }

    private void generateEnumMethods() {
        if (isEnumClass(descriptor)) {
            generateEnumValuesMethod();
            generateEnumValueOfMethod();
        }
    }

    private void generateEnumValuesMethod() {
        Type type = typeMapper.mapType(DescriptorUtilsKt.getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));

        FunctionDescriptor valuesFunction =
                CollectionsKt.single(descriptor.getStaticScope().getContributedFunctions(ENUM_VALUES, NoLookupLocation.FROM_BACKEND));
        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.OtherOriginFromPure(myClass, valuesFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUES.asString(),
                "()" + type.getDescriptor(), null, null
        );
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, classAsmType.getInternalName(), ENUM_VALUES_FIELD_NAME, type.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, type.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "values()", myClass);
    }

    private void generateEnumValueOfMethod() {
        FunctionDescriptor valueOfFunction =
                CollectionsKt.single(descriptor.getStaticScope().getContributedFunctions(ENUM_VALUE_OF, NoLookupLocation.FROM_BACKEND),
                                     DescriptorUtilsKt::isEnumValueOfMethod);
        MethodVisitor mv =
                v.newMethod(JvmDeclarationOriginKt.OtherOriginFromPure(myClass, valueOfFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUE_OF.asString(),
                            "(Ljava/lang/String;)" + classAsmType.getDescriptor(), null, null);
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();
        mv.visitLdcInsn(classAsmType);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(CHECKCAST, classAsmType.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "valueOf()", myClass);
    }

    private void generateFieldForSingleton() {
        if (isCompanionObjectInInterfaceNotIntrinsic(descriptor)) {
            StackValue.Field field = StackValue.createSingletonViaInstance(descriptor, typeMapper, HIDDEN_INSTANCE_FIELD);
            //hidden instance in interface companion
            v.newField(JvmDeclarationOriginKt.OtherOrigin(descriptor),
                       ACC_SYNTHETIC | ACC_STATIC | ACC_FINAL, field.name, field.type.getDescriptor(), null, null);
        }

        if (isEnumEntry(descriptor) || isCompanionObject(descriptor)) return;

        if (isNonCompanionObject(descriptor)) {
            StackValue.Field field = StackValue.createSingletonViaInstance(descriptor, typeMapper, INSTANCE_FIELD);
            v.newField(JvmDeclarationOriginKt.OtherOriginFromPure(myClass),
                       ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                       field.name, field.type.getDescriptor(), null, null);

            return;
        }

        ClassDescriptor companionObjectDescriptor = descriptor.getCompanionObjectDescriptor();
        if (companionObjectDescriptor == null) {
            return;
        }

        @Nullable KtObjectDeclaration companionObject = CollectionsKt.firstOrNull(myClass.getCompanionObjects());

        int properFieldVisibilityFlag = getVisibilityAccessFlag(companionObjectDescriptor);
        boolean deprecatedFieldForInvisibleCompanionObject =
                state.getLanguageVersionSettings().supportsFeature(LanguageFeature.DeprecatedFieldForInvisibleCompanionObject);
        boolean properVisibilityForCompanionObjectInstanceField =
                state.getLanguageVersionSettings().supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField);
        boolean hasPrivateOrProtectedProperVisibility = (properFieldVisibilityFlag & (ACC_PRIVATE | ACC_PROTECTED)) != 0;
        boolean hasPackagePrivateProperVisibility = (properFieldVisibilityFlag & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED)) == 0;
        boolean fieldShouldBeDeprecated =
                deprecatedFieldForInvisibleCompanionObject &&
                !properVisibilityForCompanionObjectInstanceField &&
                (hasPrivateOrProtectedProperVisibility || hasPackagePrivateProperVisibility ||
                 isNonIntrinsicPrivateCompanionObjectInInterface(companionObjectDescriptor));
        boolean doNotGeneratePublic =
                properVisibilityForCompanionObjectInstanceField && hasPrivateOrProtectedProperVisibility;
        int fieldAccessFlags;
        if (doNotGeneratePublic) {
            fieldAccessFlags = ACC_STATIC | ACC_FINAL;
        }
        else {
            fieldAccessFlags = ACC_PUBLIC | ACC_STATIC | ACC_FINAL;
        }
        if (properVisibilityForCompanionObjectInstanceField) {
            fieldAccessFlags |= properFieldVisibilityFlag;
        }
        if (fieldShouldBeDeprecated) {
            fieldAccessFlags |= ACC_DEPRECATED;
        }
        if (properVisibilityForCompanionObjectInstanceField &&
            JvmCodegenUtil.isCompanionObjectInInterfaceNotIntrinsic(companionObjectDescriptor) &&
            Visibilities.isPrivate(companionObjectDescriptor.getVisibility())) {
            fieldAccessFlags |= ACC_SYNTHETIC;
        }
        StackValue.Field field = StackValue.singleton(companionObjectDescriptor, typeMapper);
        FieldVisitor fv = v.newField(JvmDeclarationOriginKt.OtherOrigin(companionObject == null ? myClass.getPsiOrParent() : companionObject),
                                     fieldAccessFlags, field.name, field.type.getDescriptor(), null, null);
        if (fieldShouldBeDeprecated) {
            AnnotationCodegen.forField(fv, this, state).visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
        }
    }

    private void initializeObjects() {
        if (!DescriptorUtils.isObject(descriptor)) return;
        if (!state.getClassBuilderMode().generateBodies) return;

        boolean isNonCompanionObject = isNonCompanionObject(descriptor);
        boolean isInterfaceCompanion = isCompanionObjectInInterfaceNotIntrinsic(descriptor);
        boolean isInterfaceCompanionWithBackingFieldsInOuter = isInterfaceCompanionWithBackingFieldsInOuter(descriptor);
        boolean isMappedIntrinsicCompanionObject = isMappedIntrinsicCompanionObject(descriptor);
        boolean isClassCompanionWithBackingFieldsInOuter = isClassCompanionObjectWithBackingFieldsInOuter(descriptor);
        if (isNonCompanionObject ||
            (isInterfaceCompanion && !isInterfaceCompanionWithBackingFieldsInOuter) ||
            isMappedIntrinsicCompanionObject
        ) {
            ExpressionCodegen clInitCodegen = createOrGetClInitCodegen();
            InstructionAdapter v = clInitCodegen.v;
            markLineNumberForElement(element.getPsiOrParent(), v);
            v.anew(classAsmType);
            v.dup();
            v.invokespecial(classAsmType.getInternalName(), "<init>", "()V", false);

            //local0 emulates this in object constructor
            int local0Index = clInitCodegen.getFrameMap().enterTemp(classAsmType);
            assert local0Index == 0 : "Local variable with index 0 in clInit should be used only for singleton instance keeping";
            StackValue.Local local0 = StackValue.local(0, classAsmType);
            local0.store(StackValue.onStack(classAsmType), clInitCodegen.v);
            StackValue.Field singleton =
                    StackValue.createSingletonViaInstance(
                            descriptor, typeMapper, isInterfaceCompanion ? HIDDEN_INSTANCE_FIELD : INSTANCE_FIELD
            );
            singleton.store(local0, clInitCodegen.v);

            generateInitializers(clInitCodegen);

            if (isInterfaceCompanion) {
                //initialize singleton instance in outer by hidden instance
                StackValue.singleton(descriptor, typeMapper).store(
                        singleton, getParentCodegen().createOrGetClInitCodegen().v, true
                );
            }
        }
        else if (isClassCompanionWithBackingFieldsInOuter || isInterfaceCompanionWithBackingFieldsInOuter) {
            ImplementationBodyCodegen parentCodegen = (ImplementationBodyCodegen) getParentCodegen();
            ExpressionCodegen parentClInitCodegen = parentCodegen.createOrGetClInitCodegen();
            InstructionAdapter parentVisitor = parentClInitCodegen.v;

            FunctionDescriptor constructor = (FunctionDescriptor) parentCodegen.context.accessibleDescriptor(
                    CollectionsKt.single(descriptor.getConstructors()), /* superCallExpression = */ null
            );
            parentCodegen.generateMethodCallTo(constructor, null, parentVisitor);
            StackValue instance = StackValue.onStack(parentCodegen.typeMapper.mapClass(descriptor));
            StackValue.singleton(descriptor, parentCodegen.typeMapper).store(instance, parentVisitor, true);

            generateInitializers(parentClInitCodegen);
        }
        else {
            assert false : "Unknown object type: " + descriptor;
        }
    }

    private static boolean isInterfaceCompanionWithBackingFieldsInOuter(@NotNull DeclarationDescriptor declarationDescriptor) {
        DeclarationDescriptor interfaceClass = declarationDescriptor.getContainingDeclaration();
        if (!isCompanionObject(declarationDescriptor) || !isJvmInterface(interfaceClass)) return false;

        Collection<DeclarationDescriptor> descriptors = ((ClassDescriptor) declarationDescriptor).getUnsubstitutedMemberScope()
                .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.Companion.getALL_NAME_FILTER());
        return CollectionsKt.any(descriptors, d -> d instanceof PropertyDescriptor && hasJvmFieldAnnotation((PropertyDescriptor) d));
    }

    private void generateCompanionObjectBackingFieldCopies() {
        if (companionObjectPropertiesToCopy == null) return;

        for (PropertyAndDefaultValue info : companionObjectPropertiesToCopy) {
            PropertyDescriptor property = info.descriptor;

            Type type = typeMapper.mapType(property);
            int modifiers = ACC_STATIC | ACC_FINAL | ACC_PUBLIC;
            FieldVisitor fv = v.newField(JvmDeclarationOriginKt.Synthetic(DescriptorToSourceUtils.descriptorToDeclaration(property), property),
                                         modifiers, context.getFieldName(property, false),
                                         type.getDescriptor(), typeMapper.mapFieldSignature(property.getType(), property),
                                         info.defaultValue);

            AnnotationCodegen.forField(fv, this, state).genAnnotations(property, type);

            //This field are always static and final so if it has constant initializer don't do anything in clinit,
            //field would be initialized via default value in v.newField(...) - see JVM SPEC Ch.4
            // TODO: test this code
            if (state.getClassBuilderMode().generateBodies && info.defaultValue == null) {
                ExpressionCodegen codegen = createOrGetClInitCodegen();
                int companionObjectIndex = putCompanionObjectInLocalVar(codegen);
                StackValue.local(companionObjectIndex, OBJECT_TYPE).put(OBJECT_TYPE, codegen.v);
                copyFieldFromCompanionObject(property);
            }
        }
    }

    private int putCompanionObjectInLocalVar(ExpressionCodegen codegen) {
        FrameMap frameMap = codegen.myFrameMap;
        ClassDescriptor companionObjectDescriptor = descriptor.getCompanionObjectDescriptor();
        int companionObjectIndex = frameMap.getIndex(companionObjectDescriptor);
        if (companionObjectIndex == -1) {
            companionObjectIndex = frameMap.enter(companionObjectDescriptor, OBJECT_TYPE);
            StackValue companionObject = StackValue.singleton(companionObjectDescriptor, typeMapper);
            StackValue.local(companionObjectIndex, companionObject.type).store(companionObject, codegen.v);
        }
        return companionObjectIndex;
    }

    private void copyFieldFromCompanionObject(PropertyDescriptor propertyDescriptor) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, false, null, StackValue.none());
        StackValue.Field field = StackValue.field(
                property.type, property.kotlinType, classAsmType, propertyDescriptor.getName().asString(),
                true, StackValue.none(), propertyDescriptor
        );
        field.store(property, codegen.v);
    }

    public void generateInitializers(@NotNull ExpressionCodegen codegen) {
        generateInitializers(() -> codegen);
    }

    private void lookupConstructorExpressionsInClosureIfPresent() {
        if (!state.getClassBuilderMode().generateBodies || descriptor.getConstructors().isEmpty()) return;

        KtVisitorVoid visitor = new KtVisitorVoid() {
            @Override
            public void visitKtElement(@NotNull KtElement e) {
                e.acceptChildren(this);
            }

            @Override
            public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expr) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr);

                if (isLocalFunction(descriptor)) {
                    lookupInContext(descriptor);
                }
                else if (descriptor instanceof CallableMemberDescriptor) {
                    ResolvedCall<? extends CallableDescriptor> call = CallUtilKt.getResolvedCall(expr, bindingContext);
                    if (call != null) {
                        lookupReceivers(call);
                    }
                    if (call instanceof VariableAsFunctionResolvedCall) {
                        lookupReceivers(((VariableAsFunctionResolvedCall) call).getVariableCall());
                    }
                }
                else if (descriptor instanceof VariableDescriptor) {
                    DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
                    if (containingDeclaration instanceof ConstructorDescriptor) {
                        ClassDescriptor classDescriptor = ((ConstructorDescriptor) containingDeclaration).getConstructedClass();
                        if (classDescriptor == ImplementationBodyCodegen.this.descriptor) return;
                    }
                    lookupInContext(descriptor);
                }
            }

            private void lookupReceivers(@NotNull ResolvedCall<? extends CallableDescriptor> call) {
                lookupReceiver(call.getDispatchReceiver());
                lookupReceiver(call.getExtensionReceiver());
            }

            private void lookupReceiver(@Nullable ReceiverValue value) {
                if (value instanceof ImplicitReceiver) {
                    if (value instanceof ExtensionReceiver) {
                        ReceiverParameterDescriptor parameter =
                                ((ExtensionReceiver) value).getDeclarationDescriptor().getExtensionReceiverParameter();
                        assert parameter != null : "Extension receiver should exist: " + ((ExtensionReceiver) value).getDeclarationDescriptor();
                        lookupInContext(parameter);
                    }
                    else {
                        lookupInContext(((ImplicitReceiver) value).getDeclarationDescriptor());
                    }
                }
            }

            private void lookupInContext(@NotNull DeclarationDescriptor toLookup) {
                context.lookupInContext(toLookup, StackValue.LOCAL_0, state, true);
            }

            @Override
            public void visitThisExpression(@NotNull KtThisExpression expression) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
                assert descriptor instanceof CallableDescriptor ||
                       descriptor instanceof ClassDescriptor : "'This' reference target should be class or callable descriptor but was " + descriptor;
                if (descriptor instanceof ClassDescriptor) {
                    lookupInContext(descriptor);
                }

                if (descriptor instanceof CallableDescriptor) {
                    ReceiverParameterDescriptor parameter = ((CallableDescriptor) descriptor).getExtensionReceiverParameter();
                    if (parameter != null) {
                        lookupInContext(parameter);
                    }
                }
            }

            @Override
            public void visitSuperExpression(@NotNull KtSuperExpression expression) {
                lookupInContext(ExpressionCodegen.getSuperCallLabelTarget(context, expression));
            }
        };

        for (KtDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof KtProperty) {
                KtProperty property = (KtProperty) declaration;
                KtExpression initializer = property.getDelegateExpressionOrInitializer();
                if (initializer != null) {
                    initializer.accept(visitor);
                }
            }
            else if (declaration instanceof KtAnonymousInitializer) {
                KtAnonymousInitializer initializer = (KtAnonymousInitializer) declaration;
                initializer.accept(visitor);
            }
            else if (declaration instanceof KtSecondaryConstructor) {
                KtSecondaryConstructor constructor = (KtSecondaryConstructor) declaration;
                constructor.accept(visitor);
            }
        }

        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                KtExpression delegateExpression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                assert delegateExpression != null;
                delegateExpression.accept(visitor);
            }
            else if (specifier instanceof KtSuperTypeCallEntry) {
                specifier.accept(visitor);
            }
        }
    }

    private void generateEnumEntries() {
        if (descriptor.getKind() != ClassKind.ENUM_CLASS) return;

        List<KtEnumEntry> enumEntries = CollectionsKt.filterIsInstance(element.getDeclarations(), KtEnumEntry.class);

        for (KtEnumEntry enumEntry : enumEntries) {
            ClassDescriptor descriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
            int isDeprecated = KotlinBuiltIns.isDeprecated(descriptor) ? ACC_DEPRECATED : 0;
            FieldVisitor fv = v.newField(JvmDeclarationOriginKt.OtherOrigin(enumEntry, descriptor), ACC_PUBLIC | ACC_ENUM | ACC_STATIC | ACC_FINAL | isDeprecated,
                                         descriptor.getName().asString(), classAsmType.getDescriptor(), null, null);
            AnnotationCodegen.forField(fv, this, state).genAnnotations(descriptor, null);
        }

        initializeEnumConstants(enumEntries);
    }

    private void initializeEnumConstants(@NotNull List<KtEnumEntry> enumEntries) {
        if (!state.getClassBuilderMode().generateBodies) return;

        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;

        Type arrayAsmType = typeMapper.mapType(DescriptorUtilsKt.getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));
        v.newField(JvmDeclarationOriginKt.OtherOriginFromPure(myClass), ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, ENUM_VALUES_FIELD_NAME,
                   arrayAsmType.getDescriptor(), null, null);

        iv.iconst(enumEntries.size());
        iv.newarray(classAsmType);

        if (!enumEntries.isEmpty()) {
            iv.dup();
            for (int ordinal = 0, size = enumEntries.size(); ordinal < size; ordinal++) {
                initializeEnumConstant(enumEntries, ordinal);
            }
        }

        iv.putstatic(classAsmType.getInternalName(), ENUM_VALUES_FIELD_NAME, arrayAsmType.getDescriptor());
    }

    private void initializeEnumConstant(@NotNull List<KtEnumEntry> enumEntries, int ordinal) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;
        KtEnumEntry enumEntry = enumEntries.get(ordinal);

        iv.dup();
        iv.iconst(ordinal);

        ClassDescriptor classDescriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
        Type implClass = typeMapper.mapClass(classDescriptor);

        iv.anew(implClass);
        iv.dup();

        iv.aconst(enumEntry.getName());
        iv.iconst(ordinal);

        List<KtSuperTypeListEntry> delegationSpecifiers = enumEntry.getSuperTypeListEntries();
        ResolvedCall<?> defaultArgumentsConstructorCall = CallUtilKt.getResolvedCall(enumEntry, bindingContext);
        boolean enumEntryHasSubclass = CodegenBinding.enumEntryNeedSubclass(bindingContext, classDescriptor);
        if (delegationSpecifiers.size() == 1 && !enumEntryNeedSubclass(bindingContext, enumEntry)) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCallWithAssert(delegationSpecifiers.get(0), bindingContext);

            CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) resolvedCall.getResultingDescriptor(), false);

            codegen.invokeMethodWithArguments(method, resolvedCall, StackValue.none());
        }
        else if (defaultArgumentsConstructorCall != null && !enumEntryHasSubclass) {
            codegen.invokeFunction(defaultArgumentsConstructorCall, StackValue.none()).put(Type.VOID_TYPE, iv);
        }
        else {
            iv.invokespecial(implClass.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
        }

        iv.dup();
        iv.putstatic(classAsmType.getInternalName(), enumEntry.getName(), classAsmType.getDescriptor());
        iv.astore(OBJECT_TYPE);
    }

    private void generateDelegates(DelegationFieldsInfo delegationFieldsInfo) {
        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                DelegationFieldsInfo.Field field = delegationFieldsInfo.getInfo((KtDelegatedSuperTypeEntry) specifier);
                if (field == null) continue;

                generateDelegateField(field);
                KtExpression delegateExpression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                KotlinType delegateExpressionType = delegateExpression != null ? bindingContext.getType(delegateExpression) : null;
                ClassDescriptor superClass = JvmCodegenUtil.getSuperClass(specifier, state, bindingContext);
                if (superClass == null) continue;

                generateDelegates(superClass, delegateExpressionType, field);
            }
        }
    }

    private void generateDelegateField(DelegationFieldsInfo.Field fieldInfo) {
        if (!fieldInfo.generateField) return;

        v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC,
                   fieldInfo.name, fieldInfo.type.getDescriptor(), /*TODO*/null, null);
    }

    private void generateDelegates(
            @NotNull ClassDescriptor toInterface,
            @Nullable KotlinType delegateExpressionType,
            @NotNull DelegationFieldsInfo.Field field
    ) {
        for (Map.Entry<CallableMemberDescriptor, CallableMemberDescriptor> entry : DelegationResolver.Companion.getDelegates(
                descriptor, toInterface, delegateExpressionType).entrySet()
        ) {
            CallableMemberDescriptor member = entry.getKey();
            CallableMemberDescriptor delegateTo = entry.getValue();
            if (member instanceof PropertyDescriptor) {
                propertyCodegen.genDelegate((PropertyDescriptor) member, (PropertyDescriptor) delegateTo, field.getStackValue());
            }
            else if (member instanceof FunctionDescriptor) {
                functionCodegen.genDelegate((FunctionDescriptor) member, (FunctionDescriptor) delegateTo, field.getStackValue());
            }
        }
    }

    public void addCompanionObjectPropertyToCopy(@NotNull PropertyDescriptor descriptor, Object defaultValue) {
        if (companionObjectPropertiesToCopy == null) {
            companionObjectPropertiesToCopy = new ArrayList<>();
        }
        companionObjectPropertiesToCopy.add(new PropertyAndDefaultValue(descriptor, defaultValue));
    }

    @Override
    protected void done() {
        for (Function2<ImplementationBodyCodegen, ClassBuilder, Unit> task : additionalTasks) {
            task.invoke(this, v);
        }

        super.done();
    }

    private static class PropertyAndDefaultValue {
        public final PropertyDescriptor descriptor;
        public final Object defaultValue;

        public PropertyAndDefaultValue(@NotNull PropertyDescriptor descriptor, Object defaultValue) {
            this.descriptor = descriptor;
            this.defaultValue = defaultValue;
        }
    }

    public void addAdditionalTask(Function2<ImplementationBodyCodegen, ClassBuilder, Unit> additionalTask) {
        additionalTasks.add(additionalTask);
    }
}
