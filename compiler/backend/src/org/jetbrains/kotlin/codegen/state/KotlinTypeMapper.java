/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.reflect.KType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.SpecialBuiltinMembers;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.UtilKt;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment;
import org.jetbrains.kotlin.load.kotlin.*;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider.IncrementalMultifileClassPackageFragment;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion;
import org.jetbrains.kotlin.name.*;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.codegen.AsmUtil.isStaticMethod;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt.hasJvmDefaultAnnotation;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.DEFAULT_CONSTRUCTOR_MARKER;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class KotlinTypeMapper {
    private final BindingContext bindingContext;
    private final ClassBuilderMode classBuilderMode;
    private final IncompatibleClassTracker incompatibleClassTracker;
    private final String moduleName;
    private final boolean isJvm8Target;
    private final boolean isReleaseCoroutines;

    private final TypeMappingConfiguration<Type> typeMappingConfiguration = new TypeMappingConfiguration<Type>() {
        @NotNull
        @Override
        public KotlinType commonSupertype(@NotNull Collection<KotlinType> types) {
            return CommonSupertypes.commonSupertype(types);
        }

        @Nullable
        @Override
        public Type getPredefinedTypeForClass(@NotNull ClassDescriptor classDescriptor) {
            return bindingContext.get(ASM_TYPE, classDescriptor);
        }

        @Nullable
        @Override
        public String getPredefinedInternalNameForClass(@NotNull ClassDescriptor classDescriptor) {
            Type type = getPredefinedTypeForClass(classDescriptor);
            return type == null ? null : type.getInternalName();
        }

        @Override
        public void processErrorType(@NotNull KotlinType kotlinType, @NotNull ClassDescriptor descriptor) {
            if (classBuilderMode.generateBodies) {
                throw new IllegalStateException(generateErrorMessageForErrorType(kotlinType, descriptor));
            }
        }

        @Override
        public boolean releaseCoroutines() {
            return isReleaseCoroutines;
        }
    };

    private static final TypeMappingConfiguration<Type> staticTypeMappingConfiguration = new TypeMappingConfiguration<Type>() {
        @NotNull
        @Override
        public KotlinType commonSupertype(@NotNull Collection<KotlinType> types) {
            return CommonSupertypes.commonSupertype(types);
        }

        @Nullable
        @Override
        public Type getPredefinedTypeForClass(@NotNull ClassDescriptor classDescriptor) {
            return null;
        }

        @Nullable
        @Override
        public String getPredefinedInternalNameForClass(@NotNull ClassDescriptor classDescriptor) {
            return null;
        }

        @Override
        public void processErrorType(@NotNull KotlinType kotlinType, @NotNull ClassDescriptor descriptor) {
            throw new IllegalStateException(generateErrorMessageForErrorType(kotlinType, descriptor));
        }

        @Override
        public boolean releaseCoroutines() {
            return false;
        }
    };

    public KotlinTypeMapper(
            @NotNull BindingContext bindingContext,
            @NotNull ClassBuilderMode classBuilderMode,
            @NotNull IncompatibleClassTracker incompatibleClassTracker,
            @NotNull String moduleName,
            boolean isJvm8Target
    ) {
        this(bindingContext, classBuilderMode, incompatibleClassTracker, moduleName, isJvm8Target, false);
    }

    public KotlinTypeMapper(
            @NotNull BindingContext bindingContext,
            @NotNull ClassBuilderMode classBuilderMode,
            @NotNull IncompatibleClassTracker incompatibleClassTracker,
            @NotNull String moduleName,
            boolean isJvm8Target,
            boolean isReleaseCoroutines
    ) {
        this.bindingContext = bindingContext;
        this.classBuilderMode = classBuilderMode;
        this.incompatibleClassTracker = incompatibleClassTracker;
        this.moduleName = moduleName;
        this.isJvm8Target = isJvm8Target;
        this.isReleaseCoroutines = isReleaseCoroutines;
    }

    public static final boolean RELEASE_COROUTINES_DEFAULT = false;

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public Type mapOwner(@NotNull DeclarationDescriptor descriptor) {
        return mapOwner(descriptor, true);
    }

    public Type mapImplementationOwner(@NotNull DeclarationDescriptor descriptor) {
        return mapOwner(descriptor, false);
    }

    @NotNull
    private Type mapOwner(@NotNull DeclarationDescriptor descriptor, boolean publicFacade) {
        if (isLocalFunction(descriptor)) {
            return asmTypeForAnonymousClass(bindingContext,
                                            CoroutineCodegenUtilKt.unwrapInitialDescriptorForSuspendFunction((FunctionDescriptor) descriptor));
        }

        if (descriptor instanceof ConstructorDescriptor) {
            return mapClass(((ConstructorDescriptor) descriptor).getConstructedClass());
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container instanceof PackageFragmentDescriptor) {
            String packageMemberOwner = internalNameForPackageMemberOwner((CallableMemberDescriptor) descriptor, publicFacade);
            return Type.getObjectType(packageMemberOwner);
        }
        else if (container instanceof ClassDescriptor) {
            return mapClass((ClassDescriptor) container);
        }
        else {
            throw new UnsupportedOperationException("Don't know how to map owner for " + descriptor);
        }
    }

    @NotNull
    private static String internalNameForPackageMemberOwner(@NotNull CallableMemberDescriptor descriptor, boolean publicFacade) {
        boolean isAccessor = descriptor instanceof AccessorForCallableDescriptor;
        if (isAccessor) {
            descriptor = ((AccessorForCallableDescriptor) descriptor).getCalleeDescriptor();
        }
        KtFile file = DescriptorToSourceUtils.getContainingFile(descriptor);
        if (file != null) {
            Visibility visibility = descriptor.getVisibility();
            if (!publicFacade ||
                isNonConstProperty(descriptor) ||
                Visibilities.isPrivate(visibility) ||
                isAccessor/*Cause of KT-9603*/
            ) {
                return JvmFileClassUtil.getFileClassInternalName(file);
            }
            else {
                return JvmFileClassUtil.getFacadeClassInternalName(file);
            }
        }

        CallableMemberDescriptor directMember = DescriptorUtils.getDirectMember(descriptor);

        if (directMember instanceof DeserializedCallableMemberDescriptor) {
            String facadeFqName = getPackageMemberOwnerInternalName((DeserializedCallableMemberDescriptor) directMember, publicFacade);
            if (facadeFqName != null) return facadeFqName;
        }

        // TODO: drop this usage and move IrBuiltinsPackageFragmentDescriptor to IR modules; it shouldn't be used here
        if (descriptor.getContainingDeclaration() instanceof IrBuiltinsPackageFragmentDescriptor) {
            return descriptor.getContainingDeclaration().getName().asString();
        }

        throw new RuntimeException("Could not find package member for " + descriptor +
                                   " in package fragment " + descriptor.getContainingDeclaration());
    }

    private static boolean isNonConstProperty(@NotNull CallableMemberDescriptor descriptor) {
        if (!(descriptor instanceof PropertyDescriptor)) return false;
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        return !propertyDescriptor.isConst();
    }

    public static class ContainingClassesInfo {
        private final ClassId facadeClassId;
        private final ClassId implClassId;

        public ContainingClassesInfo(@NotNull ClassId facadeClassId, @NotNull ClassId implClassId) {
            this.facadeClassId = facadeClassId;
            this.implClassId = implClassId;
        }

        @NotNull
        public ClassId getFacadeClassId() {
            return facadeClassId;
        }

        @NotNull
        public ClassId getImplClassId() {
            return implClassId;
        }

        @NotNull
        private static ContainingClassesInfo forPackageMember(
                @NotNull JvmClassName facadeName,
                @NotNull JvmClassName partName
        ) {
            return new ContainingClassesInfo(
                    ClassId.topLevel(facadeName.getFqNameForTopLevelClassMaybeWithDollars()),
                    ClassId.topLevel(partName.getFqNameForTopLevelClassMaybeWithDollars())
            );
        }

        @NotNull
        private static ContainingClassesInfo forClassMember(@NotNull ClassId classId) {
            return new ContainingClassesInfo(classId, classId);
        }
    }

    @NotNull
    public static ContainingClassesInfo getContainingClassesForDeserializedCallable(
            @NotNull DeserializedCallableMemberDescriptor deserializedDescriptor
    ) {
        DeclarationDescriptor parentDeclaration = deserializedDescriptor.getContainingDeclaration();
        ContainingClassesInfo containingClassesInfo;
        if (parentDeclaration instanceof PackageFragmentDescriptor) {
            containingClassesInfo = getPackageMemberContainingClassesInfo(deserializedDescriptor);
        }
        else {
            ClassId classId = getContainerClassIdForClassDescriptor((ClassDescriptor) parentDeclaration);
            containingClassesInfo = ContainingClassesInfo.forClassMember(classId);
        }
        if (containingClassesInfo == null) {
            throw new IllegalStateException("Couldn't find container for " + deserializedDescriptor.getName());
        }
        return containingClassesInfo;
    }

    @NotNull
    private static ClassId getContainerClassIdForClassDescriptor(@NotNull ClassDescriptor classDescriptor) {
        ClassId classId = DescriptorUtilsKt.getClassId(classDescriptor);
        assert classId != null : "Deserialized class should have a ClassId: " + classDescriptor;

        String nestedClass;
        if (isInterface(classDescriptor)) {
            nestedClass = JvmAbi.DEFAULT_IMPLS_SUFFIX;
        }
        else if (classDescriptor.isInline()) {
            nestedClass = JvmAbi.ERASED_INLINE_CLASS_SUFFIX;
        }
        else {
            nestedClass = null;
        }

        if (nestedClass != null) {
            FqName relativeClassName = classId.getRelativeClassName();
            //TODO test nested trait fun inlining
            String defaultImplsClassName = relativeClassName.shortName().asString() + nestedClass;
            return new ClassId(classId.getPackageFqName(), Name.identifier(defaultImplsClassName));
        }

        return classId;
    }

    @Nullable
    private static String getPackageMemberOwnerInternalName(@NotNull DeserializedCallableMemberDescriptor descriptor, boolean publicFacade) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        assert containingDeclaration instanceof PackageFragmentDescriptor : "Not a top-level member: " + descriptor;

        ContainingClassesInfo containingClasses = getPackageMemberContainingClassesInfo(descriptor);
        if (containingClasses == null) {
            return null;
        }

        ClassId ownerClassId = publicFacade ? containingClasses.getFacadeClassId()
                                            : containingClasses.getImplClassId();
        return JvmClassName.byClassId(ownerClassId).getInternalName();
    }

    private static final ClassId FAKE_CLASS_ID_FOR_BUILTINS = ClassId.topLevel(new FqName("kotlin.KotlinPackage"));

    @Nullable
    private static ContainingClassesInfo getPackageMemberContainingClassesInfo(@NotNull DeserializedCallableMemberDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof BuiltInsPackageFragment) {
            return new ContainingClassesInfo(FAKE_CLASS_ID_FOR_BUILTINS, FAKE_CLASS_ID_FOR_BUILTINS);
        }

        JvmClassName implClassName = UtilKt.getImplClassNameForDeserialized(descriptor);
        assert implClassName != null : "No implClassName for " + descriptor;

        JvmClassName facadeName;

        if (containingDeclaration instanceof LazyJavaPackageFragment) {
            facadeName = ((LazyJavaPackageFragment) containingDeclaration).getFacadeNameForPartName(implClassName);
            if (facadeName == null) return null;
        }
        else if (containingDeclaration instanceof IncrementalMultifileClassPackageFragment) {
            facadeName = ((IncrementalMultifileClassPackageFragment) containingDeclaration).getFacadeName();
        }
        else {
            throw new AssertionError("Unexpected package fragment for " + descriptor + ": " +
                                     containingDeclaration + " (" + containingDeclaration.getClass().getSimpleName() + ")");
        }

        return ContainingClassesInfo.forPackageMember(facadeName, implClassName);
    }

    @NotNull
    public Type mapReturnType(@NotNull CallableDescriptor descriptor) {
        return mapReturnType(descriptor, null);
    }

    @NotNull
    private Type mapReturnType(@NotNull CallableDescriptor descriptor, @Nullable JvmSignatureWriter sw) {
        KotlinType returnType = descriptor.getReturnType();
        assert returnType != null : "Function has no return type: " + descriptor;

        if (descriptor instanceof ConstructorDescriptor) {
            return Type.VOID_TYPE;
        }

        if (CoroutineCodegenUtilKt.isSuspendFunctionNotSuspensionView(descriptor)) {
            return mapReturnType(
                    CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView((SimpleFunctionDescriptor) descriptor, isReleaseCoroutines),
                    sw);
        }

        if (TypeSignatureMappingKt.hasVoidReturnType(descriptor)) {
            if (sw != null) {
                sw.writeAsmType(Type.VOID_TYPE);
            }
            return Type.VOID_TYPE;
        }
        else if (descriptor instanceof FunctionDescriptor && forceBoxedReturnType((FunctionDescriptor) descriptor)) {
            //noinspection ConstantConditions
            return mapType(descriptor.getReturnType(), sw, TypeMappingMode.RETURN_TYPE_BOXED);
        }

        return mapReturnType(descriptor, sw, returnType);
    }

    @NotNull
    private Type mapReturnType(@NotNull CallableDescriptor descriptor, @Nullable JvmSignatureWriter sw, @NotNull KotlinType returnType) {
        boolean isAnnotationMethod = DescriptorUtils.isAnnotationClass(descriptor.getContainingDeclaration());
        if (sw == null || sw.skipGenericSignature()) {
            return mapType(returnType, sw, TypeMappingMode.getModeForReturnTypeNoGeneric(isAnnotationMethod));
        }

        TypeMappingMode typeMappingModeFromAnnotation =
                TypeMappingUtil.extractTypeMappingModeFromAnnotation(descriptor, returnType, isAnnotationMethod);
        if (typeMappingModeFromAnnotation != null) {
            return mapType(returnType, sw, typeMappingModeFromAnnotation);
        }

        TypeMappingMode mappingMode = TypeMappingMode.getOptimalModeForReturnType(
                returnType,
                /* isAnnotationMethod = */ isAnnotationMethod);

        return mapType(returnType, sw, mappingMode);
    }

    @NotNull
    public Type mapSupertype(@NotNull KotlinType jetType, @Nullable JvmSignatureWriter signatureVisitor) {
        return mapType(jetType, signatureVisitor, TypeMappingMode.SUPER_TYPE);
    }

    @NotNull
    public Type mapTypeParameter(@NotNull KotlinType jetType, @Nullable JvmSignatureWriter signatureVisitor) {
        return mapType(jetType, signatureVisitor, TypeMappingMode.GENERIC_ARGUMENT);
    }

    @NotNull
    public Type mapClass(@NotNull ClassifierDescriptor classifier) {
        return mapType(classifier.getDefaultType(), null, TypeMappingMode.CLASS_DECLARATION);
    }

    @NotNull
    public Type mapType(@NotNull KotlinType jetType) {
        return mapType(jetType, null, TypeMappingMode.DEFAULT);
    }

    @NotNull
    public Type mapTypeAsDeclaration(@NotNull KotlinType kotlinType) {
        return mapType(kotlinType, null, TypeMappingMode.CLASS_DECLARATION);
    }

    @NotNull
    public Type mapType(@NotNull CallableDescriptor descriptor) {
        //noinspection ConstantConditions
        return mapType(descriptor.getReturnType());
    }

    @NotNull
    public Type mapTypeAsDeclaration(@NotNull CallableDescriptor descriptor) {
        //noinspection ConstantConditions
        return mapTypeAsDeclaration(descriptor.getReturnType());
    }

    public Type mapErasedInlineClass(@NotNull ClassDescriptor descriptor) {
        return Type.getObjectType(mapClass(descriptor).getInternalName() + JvmAbi.ERASED_INLINE_CLASS_SUFFIX);
    }

    @NotNull
    public JvmMethodGenericSignature mapAnnotationParameterSignature(@NotNull PropertyDescriptor descriptor) {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);
        sw.writeReturnType();
        mapType(descriptor.getType(), sw, TypeMappingMode.VALUE_FOR_ANNOTATION);
        sw.writeReturnTypeEnd();
        return sw.makeJvmMethodSignature(descriptor.getName().asString());
    }

    @NotNull
    public Type mapType(@NotNull ClassifierDescriptor descriptor) {
        return mapType(descriptor.getDefaultType());
    }

    @NotNull
    public Type mapType(
            @NotNull KotlinType kotlinType,
            @Nullable JvmSignatureWriter signatureVisitor,
            @NotNull TypeMappingMode mode
    ) {
        return TypeSignatureMappingKt.mapType(
                kotlinType, AsmTypeFactory.INSTANCE, mode, typeMappingConfiguration, signatureVisitor,
                (ktType, asmType, typeMappingMode) -> {
                    writeGenericType(ktType, asmType, signatureVisitor, typeMappingMode);
                    return Unit.INSTANCE;
                }
        );
    }

    @NotNull
    public static Type mapInlineClassTypeAsDeclaration(@NotNull KotlinType kotlinType) {
        return mapInlineClassType(kotlinType, TypeMappingMode.CLASS_DECLARATION);
    }

    @NotNull
    public static Type mapUnderlyingTypeOfInlineClassType(@NotNull KotlinType kotlinType) {
        KotlinType underlyingType = InlineClassesUtilsKt.unsubstitutedUnderlyingType(kotlinType);
        if (underlyingType == null) {
            throw new IllegalStateException("There should be underlying type for inline class type: " + kotlinType);
        }
        return mapInlineClassType(underlyingType, TypeMappingMode.DEFAULT);
    }

    @NotNull
    public static Type mapToErasedInlineClassType(@NotNull KotlinType kotlinType) {
        return Type.getObjectType(
                mapInlineClassTypeAsDeclaration(kotlinType).getInternalName() + JvmAbi.ERASED_INLINE_CLASS_SUFFIX
        );
    }

    @NotNull
    public static Type mapInlineClassType(@NotNull KotlinType kotlinType) {
        return mapInlineClassType(kotlinType, TypeMappingMode.DEFAULT);
    }

    private static Type mapInlineClassType(
            @NotNull KotlinType kotlinType,
            @NotNull TypeMappingMode mode
    ) {
        return TypeSignatureMappingKt.mapType(
                kotlinType, AsmTypeFactory.INSTANCE, mode, staticTypeMappingConfiguration, null,
                (ktType, asmType, typeMappingMode) -> Unit.INSTANCE
        );
    }

    @NotNull
    public Type mapDefaultImpls(@NotNull ClassDescriptor descriptor) {
        return Type.getObjectType(mapType(descriptor).getInternalName() + JvmAbi.DEFAULT_IMPLS_SUFFIX);
    }

    @NotNull
    private static String generateErrorMessageForErrorType(@NotNull KotlinType type, @NotNull DeclarationDescriptor descriptor) {
        PsiElement declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);

        if (declarationElement == null) {
            return String.format("Error type encountered: %s (%s).", type, type.getClass().getSimpleName());
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        PsiElement parentDeclarationElement =
                containingDeclaration != null ? DescriptorToSourceUtils.descriptorToDeclaration(containingDeclaration) : null;

        return String.format(
                "Error type encountered: %s (%s). Descriptor: %s. For declaration %s:%s in %s:%s",
                type,
                type.getClass().getSimpleName(),
                descriptor,
                declarationElement,
                declarationElement.getText(),
                parentDeclarationElement,
                parentDeclarationElement != null ? parentDeclarationElement.getText() : "null"
        );
    }

    private void writeGenericType(
            @NotNull KotlinType type,
            @NotNull Type asmType,
            @Nullable JvmSignatureWriter signatureVisitor,
            @NotNull TypeMappingMode mode
    ) {
        if (signatureVisitor == null) return;

        // Nothing mapping rules:
        //  Map<Nothing, Foo> -> Map
        //  Map<Foo, List<Nothing>> -> Map<Foo, List>
        //  In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
        //  In<Nothing, Nothing> -> In
        //  Inv<in Nothing, Foo> -> Inv
        if (signatureVisitor.skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.getArguments().isEmpty()) {
            signatureVisitor.writeAsmType(asmType);
            return;
        }

        PossiblyInnerType possiblyInnerType = TypeParameterUtilsKt.buildPossiblyInnerType(type);
        assert possiblyInnerType != null : "possiblyInnerType with arguments should not be null";

        List<PossiblyInnerType> innerTypesAsList = possiblyInnerType.segments();

        int indexOfParameterizedType = CollectionsKt.indexOfFirst(innerTypesAsList, innerPart -> !innerPart.getArguments().isEmpty());
        if (indexOfParameterizedType < 0 || innerTypesAsList.size() == 1) {
            signatureVisitor.writeClassBegin(asmType);
            writeGenericArguments(signatureVisitor, possiblyInnerType, mode);
        }
        else {
            PossiblyInnerType outerType = innerTypesAsList.get(indexOfParameterizedType);

            signatureVisitor.writeOuterClassBegin(asmType, mapType(outerType.getClassDescriptor()).getInternalName());
            writeGenericArguments(signatureVisitor, outerType, mode);

            writeInnerParts(innerTypesAsList, signatureVisitor, mode, indexOfParameterizedType + 1); // inner parts separated by `.`
        }

        signatureVisitor.writeClassEnd();
    }

    private void writeInnerParts(
            @NotNull List<PossiblyInnerType> innerTypesAsList,
            @NotNull JvmSignatureWriter signatureVisitor,
            @NotNull TypeMappingMode mode,
            int index
    ) {
        for (PossiblyInnerType innerPart : innerTypesAsList.subList(index, innerTypesAsList.size())) {
            signatureVisitor.writeInnerClass(getJvmShortName(innerPart.getClassDescriptor()));
            writeGenericArguments(signatureVisitor, innerPart, mode);
        }
    }

    @NotNull
    private static String getJvmShortName(@NotNull ClassDescriptor klass) {
        ClassId classId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(DescriptorUtils.getFqName(klass));
        if (classId != null) {
            return classId.getShortClassName().asString();
        }

        return SpecialNames.safeIdentifier(klass.getName()).getIdentifier();
    }

    private void writeGenericArguments(
            @NotNull JvmSignatureWriter signatureVisitor,
            @NotNull PossiblyInnerType type,
            @NotNull TypeMappingMode mode
    ) {
        ClassDescriptor classDescriptor = type.getClassDescriptor();
        List<TypeParameterDescriptor> parameters = classDescriptor.getDeclaredTypeParameters();
        List<TypeProjection> arguments = type.getArguments();

        if (classDescriptor instanceof FunctionClassDescriptor &&
            ((FunctionClassDescriptor) classDescriptor).getFunctionKind() == FunctionClassDescriptor.Kind.KFunction) {
            // kotlin.reflect.KFunction{n}<P1, ... Pn, R> is mapped to kotlin.reflect.KFunction<R> on JVM (see JavaToKotlinClassMap).
            // So for these classes, we need to skip all type arguments except the very last one
            writeGenericArguments(
                    signatureVisitor,
                    Collections.singletonList(CollectionsKt.last(arguments)),
                    Collections.singletonList(CollectionsKt.last(parameters)),
                    mode
            );
            return;
        }

        writeGenericArguments(signatureVisitor, arguments, parameters, mode);
    }

    private void writeGenericArguments(
            @NotNull JvmSignatureWriter signatureVisitor,
            @NotNull List<? extends TypeProjection> arguments,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull TypeMappingMode mode
    ) {
        for (Pair<? extends TypeParameterDescriptor, ? extends TypeProjection> item : CollectionsKt.zip(parameters, arguments)) {
            TypeParameterDescriptor parameter = item.getFirst();
            TypeProjection argument = item.getSecond();

            if (
                argument.isStarProjection() ||
                // In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
                KotlinBuiltIns.isNothing(argument.getType()) && parameter.getVariance() == Variance.IN_VARIANCE
            ) {
                signatureVisitor.writeUnboundedWildcard();
            }
            else {
                TypeMappingMode argumentMode = TypeMappingUtil.updateArgumentModeFromAnnotations(mode, argument.getType());
                Variance projectionKind = getVarianceForWildcard(parameter, argument, argumentMode);

                signatureVisitor.writeTypeArgument(projectionKind);

                mapType(argument.getType(), signatureVisitor,
                        argumentMode.toGenericArgumentMode(
                                UtilsKt.getEffectiveVariance(parameter.getVariance(), argument.getProjectionKind())));

                signatureVisitor.writeTypeArgumentEnd();
            }
        }
    }

    private static boolean hasNothingInNonContravariantPosition(KotlinType kotlinType) {
        List<TypeParameterDescriptor> parameters = kotlinType.getConstructor().getParameters();
        List<TypeProjection> arguments = kotlinType.getArguments();

        for (int i = 0; i < arguments.size(); i++) {
            TypeProjection projection = arguments.get(i);

            if (projection.isStarProjection()) continue;

            KotlinType type = projection.getType();

            if (KotlinBuiltIns.isNullableNothing(type) ||
                KotlinBuiltIns.isNothing(type) && parameters.get(i).getVariance() != Variance.IN_VARIANCE) return true;
        }

        return false;
    }

    @NotNull
    public static Variance getVarianceForWildcard(
            @NotNull TypeParameterDescriptor parameter,
            @NotNull TypeProjection projection,
            @NotNull TypeMappingMode mode
    ) {
        Variance projectionKind = projection.getProjectionKind();
        Variance parameterVariance = parameter.getVariance();

        if (parameterVariance == Variance.INVARIANT) {
            return projectionKind;
        }

        if (mode.getSkipDeclarationSiteWildcards()) {
            return Variance.INVARIANT;
        }

        if (projectionKind == Variance.INVARIANT || projectionKind == parameterVariance) {
            if (mode.getSkipDeclarationSiteWildcardsIfPossible() && !projection.isStarProjection()) {
                if (parameterVariance == Variance.OUT_VARIANCE && TypeMappingUtil.isMostPreciseCovariantArgument(projection.getType())){
                    return Variance.INVARIANT;
                }

                if (parameterVariance == Variance.IN_VARIANCE
                    && TypeMappingUtil.isMostPreciseContravariantArgument(projection.getType(), parameter)) {
                    return Variance.INVARIANT;
                }
            }
            return parameterVariance;
        }

        // In<out X> = In<*>
        // Out<in X> = Out<*>
        return Variance.OUT_VARIANCE;
    }

    //NB: similar platform agnostic code in DescriptorUtils.unwrapFakeOverride
    private FunctionDescriptor findSuperDeclaration(@NotNull FunctionDescriptor descriptor, boolean isSuperCall) {
        while (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            Collection<? extends FunctionDescriptor> overridden = descriptor.getOverriddenDescriptors();
            if (overridden.isEmpty()) {
                throw new IllegalStateException("Fake override should have at least one overridden descriptor: " + descriptor);
            }

            FunctionDescriptor classCallable = null;
            for (FunctionDescriptor overriddenFunction : overridden) {
                if (!isInterface(overriddenFunction.getContainingDeclaration())) {
                    classCallable = overriddenFunction;
                    break;
                }
            }

            if (classCallable != null) {
                //prefer class callable cause of else branch
                descriptor = classCallable;
                continue;
            }
            else if (isSuperCall && !hasJvmDefaultAnnotation(descriptor) && !isInterface(descriptor.getContainingDeclaration())) {
                //Don't unwrap fake overrides from class to interface cause substituted override would be implicitly generated
                return descriptor;
            }

            descriptor = overridden.iterator().next();
        }
        return descriptor;
    }

    @NotNull
    public CallableMethod mapToCallableMethod(@NotNull FunctionDescriptor descriptor, boolean superCall) {
        if (descriptor instanceof ConstructorDescriptor) {
            JvmMethodSignature method = mapSignatureSkipGeneric(descriptor.getOriginal());
            Type owner = mapOwner(descriptor);
            FunctionDescriptor originalDescriptor = descriptor.getOriginal();
            String defaultImplDesc = mapDefaultMethod(originalDescriptor, OwnerKind.IMPLEMENTATION).getDescriptor();
            return new CallableMethod(
                    owner, owner, defaultImplDesc, method, INVOKESPECIAL,
                    null, null, null, null, null, originalDescriptor.getReturnType(), false, false
            );
        }

        if (descriptor instanceof LocalVariableAccessorDescriptor) {
            ResolvedCall<FunctionDescriptor> delegateAccessorResolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, (VariableAccessorDescriptor) descriptor);
            //noinspection ConstantConditions
            return mapToCallableMethod(delegateAccessorResolvedCall.getResultingDescriptor(), false);
        }

        DeclarationDescriptor functionParent = descriptor.getOriginal().getContainingDeclaration();

        FunctionDescriptor functionDescriptor = findSuperDeclaration(descriptor.getOriginal(), superCall);

        JvmMethodSignature signature;
        KotlinType returnKotlinType;
        Type owner;
        Type ownerForDefaultImpl;
        FunctionDescriptor baseMethodDescriptor;
        int invokeOpcode;
        Type thisClass;
        KotlinType dispatchReceiverKotlinType;
        boolean isInterfaceMember = false;
        boolean isDefaultMethodInInterface = false;

        if (functionParent instanceof ClassDescriptor) {
            FunctionDescriptor declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor);

            ClassDescriptor currentOwner = (ClassDescriptor) functionParent;
            ClassDescriptor declarationOwner = (ClassDescriptor) declarationFunctionDescriptor.getContainingDeclaration();

            boolean originalIsInterface = isJvmInterface(declarationOwner);
            boolean currentIsInterface = isJvmInterface(currentOwner);

            boolean isInterface = currentIsInterface && originalIsInterface;

            baseMethodDescriptor = findBaseDeclaration(functionDescriptor).getOriginal();
            ClassDescriptor ownerForDefault = (ClassDescriptor) baseMethodDescriptor.getContainingDeclaration();
            isDefaultMethodInInterface = isJvmInterface(ownerForDefault) && hasJvmDefaultAnnotation(baseMethodDescriptor);
            ownerForDefaultImpl =
                    isJvmInterface(ownerForDefault) && !hasJvmDefaultAnnotation(baseMethodDescriptor) ?
                    mapDefaultImpls(ownerForDefault) : mapClass(ownerForDefault);

            if (isInterface && (superCall || descriptor.getVisibility() == Visibilities.PRIVATE || isAccessor(descriptor))) {
                thisClass = mapClass(currentOwner);
                dispatchReceiverKotlinType = currentOwner.getDefaultType();
                if (declarationOwner instanceof JavaClassDescriptor || hasJvmDefaultAnnotation(declarationFunctionDescriptor)) {
                    invokeOpcode = INVOKESPECIAL;
                    signature = mapSignatureSkipGeneric(functionDescriptor);
                    returnKotlinType = functionDescriptor.getReturnType();
                    owner = thisClass;
                    isInterfaceMember = true;
                }
                else {
                    invokeOpcode = INVOKESTATIC;
                    FunctionDescriptor originalDescriptor = descriptor.getOriginal();
                    signature = mapSignatureSkipGeneric(originalDescriptor, OwnerKind.DEFAULT_IMPLS);
                    returnKotlinType = originalDescriptor.getReturnType();
                    if (descriptor instanceof AccessorForCallableDescriptor &&
                        hasJvmDefaultAnnotation(((AccessorForCallableDescriptor) descriptor).getCalleeDescriptor())) {
                        owner = mapClass(currentOwner);
                        isInterfaceMember = true;
                    }
                    else {
                        owner = mapDefaultImpls(currentOwner);
                    }
                }
            }
            else {
                boolean toInlinedErasedClass = currentOwner.isInline() && !isAccessor(functionDescriptor);

                boolean isStaticInvocation = (isStaticDeclaration(functionDescriptor) &&
                                              !(functionDescriptor instanceof ImportedFromObjectCallableDescriptor)) ||
                                             isStaticAccessor(functionDescriptor) ||
                                             CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(functionDescriptor) ||
                                             toInlinedErasedClass;
                if (isStaticInvocation) {
                    invokeOpcode = INVOKESTATIC;
                    isInterfaceMember = currentIsInterface && currentOwner instanceof JavaClassDescriptor;
                }
                else if (isInterface) {
                    invokeOpcode = INVOKEINTERFACE;
                    isInterfaceMember = true;
                }
                else {
                    boolean isPrivateFunInvocation =
                            Visibilities.isPrivate(functionDescriptor.getVisibility()) && !functionDescriptor.isSuspend();
                    invokeOpcode = superCall || isPrivateFunInvocation ? INVOKESPECIAL : INVOKEVIRTUAL;
                    isInterfaceMember = superCall && currentIsInterface;
                }

                FunctionDescriptor overriddenSpecialBuiltinFunction =
                        SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(functionDescriptor.getOriginal());
                FunctionDescriptor functionToCall = overriddenSpecialBuiltinFunction != null && !superCall && !toInlinedErasedClass
                                                    ? overriddenSpecialBuiltinFunction.getOriginal()
                                                    : functionDescriptor.getOriginal();

                signature = toInlinedErasedClass
                            ? mapSignatureForInlineErasedClassSkipGeneric(functionToCall)
                            : mapSignatureSkipGeneric(functionToCall);
                returnKotlinType = functionToCall.getReturnType();

                ClassDescriptor receiver = (currentIsInterface && !originalIsInterface) || currentOwner instanceof FunctionClassDescriptor
                                           ? declarationOwner
                                           : currentOwner;
                owner = toInlinedErasedClass ? mapErasedInlineClass(receiver) : mapClass(receiver);
                thisClass = owner;
                dispatchReceiverKotlinType = receiver.getDefaultType();
            }
        }
        else {
            FunctionDescriptor originalDescriptor = functionDescriptor.getOriginal();
            signature = mapSignatureSkipGeneric(originalDescriptor);
            returnKotlinType = originalDescriptor.getReturnType();
            owner = mapOwner(functionDescriptor);
            ownerForDefaultImpl = owner;
            baseMethodDescriptor = functionDescriptor;
            if (functionParent instanceof PackageFragmentDescriptor) {
                invokeOpcode = INVOKESTATIC;
                thisClass = null;
                dispatchReceiverKotlinType = null;
            }
            else if (functionDescriptor instanceof ConstructorDescriptor) {
                invokeOpcode = INVOKESPECIAL;
                thisClass = null;
                dispatchReceiverKotlinType = null;
            }
            else {
                invokeOpcode = INVOKEVIRTUAL;
                thisClass = owner;
                DeclarationDescriptor ownerDescriptor = functionDescriptor.getContainingDeclaration();
                dispatchReceiverKotlinType = ownerDescriptor instanceof ClassDescriptor ?
                                             ((ClassDescriptor) ownerDescriptor).getDefaultType() : null;
            }
        }

        Type calleeType = isLocalFunction(functionDescriptor) ? owner : null;

        Type receiverParameterType;
        KotlinType extensionReceiverKotlinType;
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getOriginal().getExtensionReceiverParameter();
        if (receiverParameter != null) {
            extensionReceiverKotlinType = receiverParameter.getType();
            receiverParameterType = mapType(extensionReceiverKotlinType);
        }
        else {
            extensionReceiverKotlinType = null;
            receiverParameterType = null;
        }

        String defaultImplDesc = mapDefaultMethod(baseMethodDescriptor, getKindForDefaultImplCall(baseMethodDescriptor)).getDescriptor();

        return new CallableMethod(
                owner, ownerForDefaultImpl, defaultImplDesc, signature, invokeOpcode,
                thisClass, dispatchReceiverKotlinType, receiverParameterType, extensionReceiverKotlinType, calleeType, returnKotlinType,
                isJvm8Target ? isInterfaceMember : invokeOpcode == INVOKEINTERFACE, isDefaultMethodInInterface
        );
    }

    public static boolean isAccessor(@Nullable CallableMemberDescriptor descriptor) {
        return descriptor instanceof AccessorForCallableDescriptor<?>;
    }

    public static boolean isStaticAccessor(@Nullable CallableMemberDescriptor descriptor) {
        if (descriptor instanceof AccessorForConstructorDescriptor) return false;
        return isAccessor(descriptor);
    }

    @NotNull
    private static FunctionDescriptor findAnyDeclaration(@NotNull FunctionDescriptor function) {
        if (function.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            return function;
        }
        return findBaseDeclaration(function);
    }

    @NotNull
    private static FunctionDescriptor findBaseDeclaration(@NotNull FunctionDescriptor function) {
        if (function.getOverriddenDescriptors().isEmpty()) {
            return function;
        }
        else {
            // TODO: prefer class to interface
            return findBaseDeclaration(function.getOverriddenDescriptors().iterator().next());
        }
    }

    @NotNull
    private String mapFunctionName(@NotNull FunctionDescriptor descriptor) {
        if (!(descriptor instanceof JavaCallableMemberDescriptor)) {
            String platformName = getJvmName(descriptor);
            if (platformName != null) return platformName;
        }

        String nameForSpecialFunction = SpecialBuiltinMembers.getJvmMethodNameIfSpecial(descriptor);
        if (nameForSpecialFunction != null) return nameForSpecialFunction;

        if (descriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
            if (isAnnotationClass(property.getContainingDeclaration())) {
                return property.getName().asString();
            }

            boolean isAccessor = property instanceof AccessorForPropertyDescriptor;
            String propertyName = isAccessor
                                  ? ((AccessorForPropertyDescriptor) property).getAccessorSuffix()
                                  : property.getName().asString();

            String accessorName = descriptor instanceof PropertyGetterDescriptor
                                  ? JvmAbi.getterName(propertyName)
                                  : JvmAbi.setterName(propertyName);

            return mangleMemberNameIfRequired(isAccessor ? "access$" + accessorName : accessorName, descriptor);
        }
        else if (isFunctionLiteral(descriptor)) {
            PsiElement element = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor);
            if (element instanceof KtFunctionLiteral) {
                PsiElement expression = element.getParent();
                if (expression instanceof KtLambdaExpression) {
                    SamType samType = bindingContext.get(SAM_VALUE, (KtExpression) expression);
                    if (samType != null) {
                        return samType.getOriginalAbstractMethod().getName().asString();
                    }
                }
            }

            return OperatorNameConventions.INVOKE.asString();
        }
        else if (isLocalFunction(descriptor) || isFunctionExpression(descriptor)) {
            return OperatorNameConventions.INVOKE.asString();
        }
        else {
            return mangleMemberNameIfRequired(descriptor.getName().asString(), descriptor);
        }
    }

    @NotNull
    private static OwnerKind getKindForDefaultImplCall(@NotNull FunctionDescriptor baseMethodDescriptor) {
        DeclarationDescriptor containingDeclaration = baseMethodDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return OwnerKind.PACKAGE;
        }
        else if (isInterface(containingDeclaration)) {
            return OwnerKind.DEFAULT_IMPLS;
        }
        return OwnerKind.IMPLEMENTATION;
    }

    @NotNull
    public static String mapDefaultFieldName(@NotNull PropertyDescriptor propertyDescriptor, boolean isDelegated) {
        String name;
        if (propertyDescriptor instanceof AccessorForPropertyDescriptor) {
            name = ((AccessorForPropertyDescriptor) propertyDescriptor).getCalleeDescriptor().getName().asString();
        }
        else {
            name = propertyDescriptor.getName().asString();
        }
        return isDelegated ? name + JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX : name;
    }

    @NotNull
    private String mangleMemberNameIfRequired(@NotNull String name, @NotNull CallableMemberDescriptor descriptor) {
        if (descriptor.getContainingDeclaration() instanceof ScriptDescriptor) {
            //script properties should be public
            return name;
        }

        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            if (Visibilities.isPrivate(descriptor.getVisibility()) && !(descriptor instanceof ConstructorDescriptor) && !"<clinit>".equals(name)) {
                String partName = getPartSimpleNameForMangling(descriptor);
                if (partName != null) return name + "$" + partName;
            }
            return name;
        }

        if (!(descriptor instanceof ConstructorDescriptor) &&
            descriptor.getVisibility() == Visibilities.INTERNAL &&
            !DescriptorUtilsKt.isPublishedApi(descriptor)) {
            return InternalNameMapper.mangleInternalName(name, moduleName);
        }

        return name;
    }

    @Nullable
    private static String getPartSimpleNameForMangling(@NotNull CallableMemberDescriptor descriptor) {
        KtFile containingFile = DescriptorToSourceUtils.getContainingFile(descriptor);
        if (containingFile != null) {
            JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(containingFile);
            if (fileClassInfo.getWithJvmMultifileClass()) {
                return fileClassInfo.getFileClassFqName().shortName().asString();
            }
            return null;
        }

        descriptor = DescriptorUtils.getDirectMember(descriptor);
        assert descriptor instanceof DeserializedCallableMemberDescriptor :
                "Descriptor without sources should be instance of DeserializedCallableMemberDescriptor, but: " +
                descriptor;
        ContainingClassesInfo containingClassesInfo =
                getContainingClassesForDeserializedCallable((DeserializedCallableMemberDescriptor) descriptor);
        String facadeShortName = containingClassesInfo.getFacadeClassId().getShortClassName().asString();
        String implShortName = containingClassesInfo.getImplClassId().getShortClassName().asString();
        return !facadeShortName.equals(implShortName) ? implShortName : null;
    }

    @NotNull
    public Method mapAsmMethod(@NotNull FunctionDescriptor descriptor) {
        return mapSignature(descriptor).getAsmMethod();
    }

    @NotNull
    public Method mapAsmMethod(@NotNull FunctionDescriptor descriptor, @NotNull OwnerKind kind) {
        return mapSignature(descriptor, kind, true).getAsmMethod();
    }

    @NotNull
    private JvmMethodGenericSignature mapSignature(@NotNull FunctionDescriptor f) {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, true);
    }

    @NotNull
    public JvmMethodSignature mapSignatureSkipGeneric(@NotNull FunctionDescriptor f) {
        return mapSignatureSkipGeneric(f, OwnerKind.IMPLEMENTATION);
    }

    @NotNull
    public JvmMethodSignature mapSignatureForInlineErasedClassSkipGeneric(@NotNull FunctionDescriptor f) {
        return mapSignatureSkipGeneric(f, OwnerKind.ERASED_INLINE_CLASS);
    }

    @NotNull
    public JvmMethodGenericSignature mapSignatureForBoxMethodOfInlineClass(@NotNull FunctionDescriptor f) {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, true);
    }

    @NotNull
    public JvmMethodSignature mapSignatureSkipGeneric(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind) {
        return mapSignature(f, kind, true);
    }

    @NotNull
    public JvmMethodGenericSignature mapSignatureWithGeneric(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind) {
        return mapSignature(f, kind, false);
    }

    @NotNull
    public JvmMethodGenericSignature mapSignatureWithGeneric(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind, boolean hasSpecialBridge) {
        return mapSignature(f, kind, false, hasSpecialBridge);
    }

    private JvmMethodGenericSignature mapSignature(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind, boolean skipGenericSignature) {
        return mapSignature(f, kind, skipGenericSignature, false);
    }

    @NotNull
    private JvmMethodGenericSignature mapSignature(
            @NotNull FunctionDescriptor f,
            @NotNull OwnerKind kind,
            boolean skipGenericSignature,
            boolean hasSpecialBridge
    ) {
        if (f.getInitialSignatureDescriptor() != null && f != f.getInitialSignatureDescriptor()) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if (SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(f) == null ||
                f.getContainingDeclaration().getOriginal() instanceof JavaClassDescriptor) {
                return mapSignature(f.getInitialSignatureDescriptor(), kind, skipGenericSignature);
            }
        }

        if (f instanceof TypeAliasConstructorDescriptor) {
            return mapSignature(((TypeAliasConstructorDescriptor) f).getUnderlyingConstructorDescriptor().getOriginal(), kind, skipGenericSignature);
        }

        if (f instanceof FunctionImportedFromObject) {
            return mapSignature(((FunctionImportedFromObject) f).getCallableFromObject(), kind, skipGenericSignature);
        }

        if (CoroutineCodegenUtilKt.isSuspendFunctionNotSuspensionView(f)) {
            return mapSignature(CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(f, isReleaseCoroutines), kind,
                                skipGenericSignature);
        }

        return mapSignatureWithCustomParameters(f, kind, f.getValueParameters(), skipGenericSignature, hasSpecialBridge);
    }

    @NotNull
    public JvmMethodGenericSignature mapSignatureWithCustomParameters(
            @NotNull FunctionDescriptor f,
            @NotNull OwnerKind kind,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            boolean skipGenericSignature
    ) {
        return mapSignatureWithCustomParameters(f, kind, valueParameters, skipGenericSignature, false);
    }

    @NotNull
    private JvmMethodGenericSignature mapSignatureWithCustomParameters(
            @NotNull FunctionDescriptor f,
            @NotNull OwnerKind kind,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            boolean skipGenericSignature,
            boolean hasSpecialBridge
    ) {
        checkOwnerCompatibility(f);

        JvmSignatureWriter sw = skipGenericSignature || f instanceof AccessorForCallableDescriptor
                                 ? new JvmSignatureWriter()
                                 : new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        if (f instanceof ClassConstructorDescriptor) {
            sw.writeParametersStart();
            writeAdditionalConstructorParameters((ClassConstructorDescriptor) f, sw);

            for (ValueParameterDescriptor parameter : valueParameters) {
                writeParameter(sw, parameter.getType(), f);
            }

            if (f instanceof AccessorForConstructorDescriptor) {
                writeParameter(sw, JvmMethodParameterKind.CONSTRUCTOR_MARKER, DEFAULT_CONSTRUCTOR_MARKER);
            }

            writeVoidReturn(sw);
        }
        else {
            CallableMemberDescriptor directMember = DescriptorUtils.getDirectMember(f);
            KotlinType thisIfNeeded = null;
            if (OwnerKind.DEFAULT_IMPLS == kind) {
                ReceiverTypeAndTypeParameters receiverTypeAndTypeParameters = TypeMapperUtilsKt.patchTypeParametersForDefaultImplMethod(directMember);
                writeFormalTypeParameters(CollectionsKt.plus(receiverTypeAndTypeParameters.getTypeParameters(), directMember.getTypeParameters()), sw);
                thisIfNeeded = receiverTypeAndTypeParameters.getReceiverType();
            }
            else if (OwnerKind.ERASED_INLINE_CLASS == kind) {
                ClassDescriptor classDescriptor = (ClassDescriptor) directMember.getContainingDeclaration();
                thisIfNeeded = classDescriptor.getDefaultType();
            }
            else {
                writeFormalTypeParameters(directMember.getTypeParameters(), sw);
                if (isAccessor(f) && f.getDispatchReceiverParameter() != null) {
                    thisIfNeeded = ((ClassDescriptor) f.getContainingDeclaration()).getDefaultType();
                }
            }

            sw.writeParametersStart();
            if (thisIfNeeded != null) {
                writeParameter(sw, JvmMethodParameterKind.THIS, thisIfNeeded, f);
            }

            ReceiverParameterDescriptor receiverParameter = f.getExtensionReceiverParameter();
            if (receiverParameter != null) {
                writeParameter(sw, JvmMethodParameterKind.RECEIVER, receiverParameter.getType(), f);
            }

            for (ValueParameterDescriptor parameter : valueParameters) {
                boolean forceBoxing = MethodSignatureMappingKt.forceSingleValueParameterBoxing(f);
                writeParameter(
                        sw,
                        forceBoxing ? TypeUtils.makeNullable(parameter.getType()) : parameter.getType(),
                        f
                );
            }

            sw.writeReturnType();
            mapReturnType(f, sw);
            sw.writeReturnTypeEnd();
        }

        JvmMethodGenericSignature signature = sw.makeJvmMethodSignature(mapFunctionName(f));

        if (kind != OwnerKind.DEFAULT_IMPLS && !hasSpecialBridge) {
            SpecialSignatureInfo specialSignatureInfo = BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo(f);

            if (specialSignatureInfo != null) {
                String newGenericSignature = CodegenUtilKt.replaceValueParametersIn(
                        specialSignatureInfo, signature.getGenericsSignature());
                return new JvmMethodGenericSignature(signature.getAsmMethod(), signature.getValueParameters(), newGenericSignature);
            }
        }

        return signature;
    }

    private void checkOwnerCompatibility(@NotNull FunctionDescriptor descriptor) {
        KotlinJvmBinaryClass ownerClass = KotlinJvmBinaryClassUtilKt.getContainingKotlinJvmBinaryClass(descriptor);
        if (ownerClass == null) return;

        JvmBytecodeBinaryVersion version = ownerClass.getClassHeader().getBytecodeVersion();
        if (!version.isCompatible()) {
            incompatibleClassTracker.record(ownerClass);
        }
    }

    @NotNull
    private static String getDefaultDescriptor(
            @NotNull Method method,
            @Nullable String dispatchReceiverDescriptor,
            @NotNull CallableDescriptor callableDescriptor
    ) {
        String descriptor = method.getDescriptor();
        int maskArgumentsCount = (callableDescriptor.getValueParameters().size() + Integer.SIZE - 1) / Integer.SIZE;
        String additionalArgs = StringUtil.repeat(Type.INT_TYPE.getDescriptor(), maskArgumentsCount);
        additionalArgs += (isConstructor(method) ? DEFAULT_CONSTRUCTOR_MARKER : OBJECT_TYPE).getDescriptor();
        String result = descriptor.replace(")", additionalArgs + ")");
        if (dispatchReceiverDescriptor != null && !isConstructor(method)) {
            return result.replace("(", "(" + dispatchReceiverDescriptor);
        }
        return result;
    }

    public ClassBuilderMode getClassBuilderMode() {
        return classBuilderMode;
    }

    private static boolean isConstructor(@NotNull Method method) {
        return "<init>".equals(method.getName());
    }

    @NotNull
    public Method mapDefaultMethod(@NotNull FunctionDescriptor functionDescriptor, @NotNull OwnerKind kind) {
        Method jvmSignature = mapAsmMethod(functionDescriptor, kind);
        Type ownerType = mapOwner(functionDescriptor);
        boolean isConstructor = isConstructor(jvmSignature);
        String descriptor = getDefaultDescriptor(
                jvmSignature,
                isStaticMethod(kind, functionDescriptor) || isConstructor ? null : ownerType.getDescriptor(),
                CodegenUtilKt.unwrapFrontendVersion(functionDescriptor)
        );

        return new Method(isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, descriptor);
    }

    /**
     * @return true iff a given function descriptor should be compiled to a method with boxed return type regardless of whether return type
     * of that descriptor is nullable or not. This happens in two cases:
     * - when a target function is a synthetic box method of erased inline class;
     * - when a function returning a value of a primitive type overrides another function with a non-primitive return type.
     * In that case the generated method's return type should be boxed: otherwise it's not possible to use
     * this class from Java since javac issues errors when loading the class (incompatible return types)
     */
    private static boolean forceBoxedReturnType(@NotNull FunctionDescriptor descriptor) {
        if (isBoxMethodForInlineClass(descriptor)) return true;

        //noinspection ConstantConditions
        if (!KotlinBuiltIns.isPrimitiveType(descriptor.getReturnType())) return false;

        for (FunctionDescriptor overridden : getAllOverriddenDescriptors(descriptor)) {
            //noinspection ConstantConditions
            if (!KotlinBuiltIns.isPrimitiveType(overridden.getReturnType())) return true;
        }

        return false;
    }

    private static boolean isBoxMethodForInlineClass(@NotNull FunctionDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!InlineClassesUtilsKt.isInlineClass(containingDeclaration)) return false;

        return CallableMemberDescriptor.Kind.SYNTHESIZED == descriptor.getKind() &&
               InlineClassDescriptorResolver.BOX_METHOD_NAME.equals(descriptor.getName());
    }

    private static void writeVoidReturn(@NotNull JvmSignatureWriter sw) {
        sw.writeReturnType();
        sw.writeAsmType(Type.VOID_TYPE);
        sw.writeReturnTypeEnd();
    }

    @Nullable
    public String mapFieldSignature(@NotNull KotlinType backingFieldType, @NotNull PropertyDescriptor propertyDescriptor) {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.TYPE);

        if (!propertyDescriptor.isVar()) {
            mapReturnType(propertyDescriptor, sw, backingFieldType);
        }
        else {
            writeParameterType(sw, backingFieldType, propertyDescriptor);
        }

        return sw.makeJavaGenericSignature();
    }

    public void writeFormalTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull JvmSignatureWriter sw) {
        if (sw.skipGenericSignature()) return;
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            writeFormalTypeParameter(typeParameter, sw);
        }
    }

    private void writeFormalTypeParameter(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull JvmSignatureWriter sw) {
        if (!classBuilderMode.generateBodies && typeParameterDescriptor.getName().isSpecial()) {
            // If a type parameter has no name, the code below fails, but it should recover in case of light classes
            return;
        }

        sw.writeFormalTypeParameter(typeParameterDescriptor.getName().asString());

        classBound:
        {
            sw.writeClassBound();

            for (KotlinType jetType : typeParameterDescriptor.getUpperBounds()) {
                if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                    if (!isJvmInterface(jetType)) {
                        mapType(jetType, sw, TypeMappingMode.GENERIC_ARGUMENT);
                        break classBound;
                    }
                }
            }

            // "extends Object" is optional according to ClassFileFormat-Java5.pdf
            // but javac complaints to signature:
            // <P:>Ljava/lang/Object;
            // TODO: avoid writing java/lang/Object if interface list is not empty
        }
        sw.writeClassBoundEnd();

        for (KotlinType jetType : typeParameterDescriptor.getUpperBounds()) {
            ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
            if (classifier instanceof ClassDescriptor) {
                if (isJvmInterface(jetType)) {
                    sw.writeInterfaceBound();
                    mapType(jetType, sw, TypeMappingMode.GENERIC_ARGUMENT);
                    sw.writeInterfaceBoundEnd();
                }
            }
            else if (classifier instanceof TypeParameterDescriptor) {
                sw.writeInterfaceBound();
                mapType(jetType, sw, TypeMappingMode.GENERIC_ARGUMENT);
                sw.writeInterfaceBoundEnd();
            }
            else {
                throw new UnsupportedOperationException("Unknown classifier: " + classifier);
            }
        }
    }

    private void writeParameter(
            @NotNull JvmSignatureWriter sw,
            @NotNull KotlinType type,
            @Nullable CallableDescriptor callableDescriptor
    ) {
        writeParameter(sw, JvmMethodParameterKind.VALUE, type, callableDescriptor);
    }

    private void writeParameter(
            @NotNull JvmSignatureWriter sw,
            @NotNull JvmMethodParameterKind kind,
            @NotNull KotlinType type,
            @Nullable CallableDescriptor callableDescriptor
    ) {
        sw.writeParameterType(kind);

        writeParameterType(sw, type, callableDescriptor);

        sw.writeParameterTypeEnd();
    }

    private void writeParameterType(
            @NotNull JvmSignatureWriter sw,
            @NotNull KotlinType type,
            @Nullable CallableDescriptor callableDescriptor
    ) {
        if (sw.skipGenericSignature()) {
            mapType(type, sw, TypeMappingMode.DEFAULT);
            return;
        }

        TypeMappingMode typeMappingMode;

        TypeMappingMode typeMappingModeFromAnnotation =
                TypeMappingUtil.extractTypeMappingModeFromAnnotation(callableDescriptor, type, /* isForAnnotationParameter = */ false);

        if (typeMappingModeFromAnnotation != null) {
            typeMappingMode = typeMappingModeFromAnnotation;
        }
        else if (TypeMappingUtil.isMethodWithDeclarationSiteWildcards(callableDescriptor) && !type.getArguments().isEmpty()) {
            typeMappingMode = TypeMappingMode.GENERIC_ARGUMENT; // Render all wildcards
        }
        else {
            typeMappingMode = TypeMappingMode.getOptimalModeForValueParameter(type);
        }

        mapType(type, sw, typeMappingMode);
    }

    private static void writeParameter(@NotNull JvmSignatureWriter sw, @NotNull JvmMethodParameterKind kind, @NotNull Type type) {
        sw.writeParameterType(kind);
        sw.writeAsmType(type);
        sw.writeParameterTypeEnd();
    }

    private void writeAdditionalConstructorParameters(@NotNull ClassConstructorDescriptor descriptor, @NotNull JvmSignatureWriter sw) {
        boolean isSynthesized = descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED;
        //if (isSynthesized) return;

        MutableClosure closure = bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration());

        ClassDescriptor captureThis = getDispatchReceiverParameterForConstructorCall(descriptor, closure);
        if (!isSynthesized && captureThis != null) {
            writeParameter(sw, JvmMethodParameterKind.OUTER, captureThis.getDefaultType(), descriptor);
        }

        KotlinType captureReceiverType = closure != null ? closure.getCaptureReceiverType() : null;
        if (captureReceiverType != null) {
            writeParameter(sw, JvmMethodParameterKind.RECEIVER, captureReceiverType, descriptor);
        }

        ClassDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        if (!isSynthesized) {
            if (containingDeclaration.getKind() == ClassKind.ENUM_CLASS || containingDeclaration.getKind() == ClassKind.ENUM_ENTRY) {
                writeParameter(
                        sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, DescriptorUtilsKt.getBuiltIns(descriptor).getStringType(),
                        descriptor);
                writeParameter(
                        sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, DescriptorUtilsKt.getBuiltIns(descriptor).getIntType(),
                        descriptor);
            }
        }

        if (closure == null) return;

        for (DeclarationDescriptor variableDescriptor : closure.getCaptureVariables().keySet()) {
            Type type;
            if (variableDescriptor instanceof VariableDescriptor && !(variableDescriptor instanceof PropertyDescriptor)) {
                Type sharedVarType = getSharedVarType(variableDescriptor);
                if (sharedVarType == null) {
                    if (isDelegatedLocalVariable(variableDescriptor)) {
                        //noinspection CastConflictsWithInstanceof
                        KotlinType delegateType =
                                JvmCodegenUtil.getPropertyDelegateType((LocalVariableDescriptor) variableDescriptor, bindingContext);
                        assert delegateType != null : "Local delegated property type should not be null: " + variableDescriptor;
                        sharedVarType = mapType(delegateType);
                    }
                    else {
                        sharedVarType = mapType(((VariableDescriptor) variableDescriptor).getType());
                    }
                }
                type = sharedVarType;
            }
            else if (isLocalFunction(variableDescriptor)) {
                //noinspection CastConflictsWithInstanceof
                type = asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) variableDescriptor);
            }
            else {
                type = null;
            }

            if (type != null) {
                closure.setCapturedParameterOffsetInConstructor(variableDescriptor, sw.getCurrentSignatureSize() + 1);
                writeParameter(sw, JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE, type);
            }
        }

        // We may generate a slightly wrong signature for a local class / anonymous object in light classes mode but we don't care,
        // because such classes are not accessible from the outside world
        if (classBuilderMode.generateBodies) {
            ResolvedCall<ConstructorDescriptor> superCall = findFirstDelegatingSuperCall(descriptor);
            if (superCall == null) return;
            writeSuperConstructorCallParameters(sw, descriptor, superCall, captureThis != null);
        }
    }

    private void writeSuperConstructorCallParameters(
            @NotNull JvmSignatureWriter sw,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull ResolvedCall<ConstructorDescriptor> superCall,
            boolean hasOuter
    ) {
        ConstructorDescriptor superDescriptor = SamCodegenUtil.resolveSamAdapter(superCall.getResultingDescriptor());
        Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = superCall.getValueArguments();

        List<JvmMethodParameterSignature> parameters = mapSignatureSkipGeneric(superDescriptor.getOriginal()).getValueParameters();

        int params = parameters.size();
        int args = valueArguments.size();

        // Mapped parameters should consist of captured values plus all of valueArguments
        assert params >= args :
                String.format("Incorrect number of mapped parameters vs arguments: %d < %d for %s", params, args, descriptor);

        // Include all captured values, i.e. those parameters for which there are no resolved value arguments
        for (int i = 0; i < params - args; i++) {
            JvmMethodParameterSignature parameter = parameters.get(i);
            JvmMethodParameterKind kind = parameter.getKind();
            if (kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL) continue;
            if (hasOuter && kind == JvmMethodParameterKind.OUTER) continue;

            writeParameter(sw, JvmMethodParameterKind.SUPER_CALL_PARAM, parameter.getAsmType());
        }

        if (isAnonymousObject(descriptor.getContainingDeclaration())) {
            // For anonymous objects, also add all real non-default value arguments passed to the super constructor
            for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> argumentAndValue : valueArguments.entrySet()) {

                ResolvedValueArgument valueArgument = argumentAndValue.getValue();
                if (!(valueArgument instanceof DefaultValueArgument)) {
                    JvmMethodParameterSignature parameter = parameters.get(params - args + argumentAndValue.getKey().getIndex());
                    writeParameter(sw, JvmMethodParameterKind.SUPER_CALL_PARAM, parameter.getAsmType());
                }
            }
        }
    }

    @Nullable
    private ResolvedCall<ConstructorDescriptor> findFirstDelegatingSuperCall(@NotNull ConstructorDescriptor descriptor) {
        ClassifierDescriptorWithTypeParameters constructorOwner = descriptor.getContainingDeclaration();
        while (true) {
            ResolvedCall<ConstructorDescriptor> next = getDelegationConstructorCall(bindingContext, descriptor);
            if (next == null) return null;
            descriptor = next.getResultingDescriptor().getOriginal();
            if (descriptor.getContainingDeclaration() != constructorOwner) return next;
        }
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(
            @NotNull ScriptDescriptor script,
            @NotNull List<ScriptDescriptor> importedScripts,
            List<? extends KType> implicitReceivers,
            List<? extends Pair<String, ? extends KType>> environmentVariables
    ) {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        sw.writeParametersStart();

        if (importedScripts.size() > 0) {
            writeParameter(sw, DescriptorUtilsKt.getModule(script).getBuiltIns().getArray().getDefaultType(), null);
        }

        if (implicitReceivers.size() > 0) {
            writeParameter(sw, DescriptorUtilsKt.getModule(script).getBuiltIns().getArray().getDefaultType(), null);
        }

        if (environmentVariables.size() > 0) {
            writeParameter(sw, DescriptorUtilsKt.getModule(script).getBuiltIns().getMap().getDefaultType(), null);
        }

        for (ValueParameterDescriptor valueParameter : script.getUnsubstitutedPrimaryConstructor().getValueParameters()) {
            writeParameter(sw, valueParameter.getType(), /* callableDescriptor = */ null);
        }

        writeVoidReturn(sw);

        return sw.makeJvmMethodSignature("<init>");
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            return asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
        }

        if (descriptor instanceof PropertyDescriptor || descriptor instanceof FunctionDescriptor) {
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) descriptor).getExtensionReceiverParameter();
            assert receiverParameter != null : "Callable should have a receiver parameter: " + descriptor;
            return StackValue.sharedTypeForType(mapType(receiverParameter.getType()));
        }

        if (descriptor instanceof LocalVariableDescriptor && ((LocalVariableDescriptor) descriptor).isDelegated()) {
            return null;
        }

        if (descriptor instanceof VariableDescriptor && isVarCapturedInClosure(bindingContext, descriptor)) {
            return StackValue.sharedTypeForType(mapType(((VariableDescriptor) descriptor).getType()));
        }

        return null;
    }

    @NotNull
    public String classInternalName(@NotNull ClassDescriptor classDescriptor) {
        Type recordedType = typeMappingConfiguration.getPredefinedTypeForClass(classDescriptor);
        if (recordedType != null) {
            return recordedType.getInternalName();
        }
        return TypeSignatureMappingKt.computeInternalName(classDescriptor, typeMappingConfiguration);
    }

    public static class InternalNameMapper {
        public static String mangleInternalName(@NotNull String name, @NotNull String moduleName) {
            return name + "$" + NameUtils.sanitizeAsJavaIdentifier(moduleName);
        }

        public static boolean canBeMangledInternalName(@NotNull String name) {
            return name.indexOf('$') != -1;
        }

        @Nullable
        public static String demangleInternalName(@NotNull String name) {
            int indexOfDollar = name.indexOf('$');
            return indexOfDollar >= 0 ? name.substring(0, indexOfDollar) : null;
        }

        @Nullable
        public static String getModuleNameSuffix(@NotNull String name) {
            int indexOfDollar = name.indexOf('$');
            return indexOfDollar >= 0 ? name.substring(indexOfDollar + 1) : null;
        }

        @Nullable
        public static String internalNameWithoutModuleSuffix(@NotNull String name) {
            String demangledName = demangleInternalName(name);
            return demangledName != null ? demangledName + '$' : null;
        }
    }
}
