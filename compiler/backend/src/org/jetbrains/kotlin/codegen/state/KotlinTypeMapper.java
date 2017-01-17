/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.state;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
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
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinsPackageFragmentDescriptor;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion;
import org.jetbrains.kotlin.load.java.SpecialBuiltinMembers;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment;
import org.jetbrains.kotlin.load.kotlin.*;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider.IncrementalMultifileClassPackageFragment;
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
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.isStaticMethod;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.DEFAULT_CONSTRUCTOR_MARKER;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class KotlinTypeMapper {
    private final BindingContext bindingContext;
    private final ClassBuilderMode classBuilderMode;
    private final JvmFileClassesProvider fileClassesProvider;
    private final IncompatibleClassTracker incompatibleClassTracker;
    private final String moduleName;
    private final boolean isJvm8Target;
    private final boolean isJvm8TargetWithDefaults;

    private final TypeMappingConfiguration<Type> typeMappingConfiguration = new TypeMappingConfiguration<Type>() {
        private final Function2<String, String, String> defaultClassNameFactory
                = TypeMappingConfiguration.Companion.getDEFAULT_INNER_CLASS_NAME_FACTORY();

        private final Function2<String, String, String> innerClassNameFactory = new Function2<String, String, String>() {
            @Override
            public String invoke(String outer, String inner) {
                if (classBuilderMode == ClassBuilderMode.KAPT3) {
                    return outer + '/' + inner;
                }

                return defaultClassNameFactory.invoke(outer, inner);
            }
        };

        @NotNull
        @Override
        public KotlinType commonSupertype(@NotNull Collection<KotlinType> types) {
            return CommonSupertypes.commonSupertype(types);
        }

        @NotNull
        @Override
        public Function2<String, String, String> getInnerClassNameFactory() {
            return innerClassNameFactory;
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
    };

    public KotlinTypeMapper(
            @NotNull BindingContext bindingContext,
            @NotNull ClassBuilderMode classBuilderMode,
            @NotNull JvmFileClassesProvider fileClassesProvider,
            @NotNull IncompatibleClassTracker incompatibleClassTracker,
            @NotNull String moduleName,
            boolean isJvm8Target,
            boolean isJvm8TargetWithDefaults
    ) {
        this.bindingContext = bindingContext;
        this.classBuilderMode = classBuilderMode;
        this.fileClassesProvider = fileClassesProvider;
        this.incompatibleClassTracker = incompatibleClassTracker;
        this.moduleName = moduleName;
        this.isJvm8Target = isJvm8Target;
        this.isJvm8TargetWithDefaults = isJvm8TargetWithDefaults;
    }

    @NotNull
    public TypeMappingConfiguration<Type> getTypeMappingConfiguration() {
        return typeMappingConfiguration;
    }

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
            return asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
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
    private String internalNameForPackageMemberOwner(@NotNull CallableMemberDescriptor descriptor, boolean publicFacade) {
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
                return FileClasses.getFileClassInternalName(fileClassesProvider, file);
            }
            else {
                return FileClasses.getFacadeClassInternalName(fileClassesProvider, file);
            }
        }

        CallableMemberDescriptor directMember = DescriptorUtils.getDirectMember(descriptor);

        if (directMember instanceof DeserializedCallableMemberDescriptor) {
            String facadeFqName = getPackageMemberOwnerInternalName((DeserializedCallableMemberDescriptor) directMember, publicFacade);
            if (facadeFqName != null) return facadeFqName;
        }

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

        @Nullable
        private static ContainingClassesInfo forPackageMember(
                @NotNull FqName packageFqName,
                @NotNull String facadeClassName,
                @NotNull String implClassName
        ) {
            return new ContainingClassesInfo(ClassId.topLevel(packageFqName.child(Name.identifier(facadeClassName))),
                                             ClassId.topLevel(packageFqName.child(Name.identifier(implClassName))));
        }

        @NotNull
        private static ContainingClassesInfo forClassMember(@NotNull ClassId classId) {
            return new ContainingClassesInfo(classId, classId);
        }
    }

    @NotNull
    public ContainingClassesInfo getContainingClassesForDeserializedCallable(
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
    private ClassId getContainerClassIdForClassDescriptor(@NotNull ClassDescriptor classDescriptor) {
        ClassId classId = DescriptorUtilsKt.getClassId(classDescriptor);
        assert classId != null : "Deserialized class should have a ClassId: " + classDescriptor;

        if (isInterface(classDescriptor)) {
            FqName relativeClassName = classId.getRelativeClassName();
            //TODO test nested trait fun inlining
            String defaultImplsClassName = typeMappingConfiguration.getInnerClassNameFactory()
                    .invoke(relativeClassName.shortName().asString(), JvmAbi.DEFAULT_IMPLS_CLASS_NAME);
            return new ClassId(classId.getPackageFqName(), Name.identifier(defaultImplsClassName));
        }

        return classId;
    }

    @Nullable
    private String getPackageMemberOwnerInternalName(@NotNull DeserializedCallableMemberDescriptor descriptor, boolean publicFacade) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        assert containingDeclaration instanceof PackageFragmentDescriptor : "Not a top-level member: " + descriptor;

        ContainingClassesInfo containingClasses = getPackageMemberContainingClassesInfo(descriptor);
        if (containingClasses == null) {
            return null;
        }

        ClassId ownerClassId = publicFacade ? containingClasses.getFacadeClassId()
                                            : containingClasses.getImplClassId();
        return JvmClassName.byClassId(ownerClassId, typeMappingConfiguration).getInternalName();
    }

    private static final ClassId FAKE_CLASS_ID_FOR_BUILTINS = ClassId.topLevel(new FqName("kotlin.KotlinPackage"));

    @Nullable
    private static ContainingClassesInfo getPackageMemberContainingClassesInfo(@NotNull DeserializedCallableMemberDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof BuiltInsPackageFragment) {
            return new ContainingClassesInfo(FAKE_CLASS_ID_FOR_BUILTINS, FAKE_CLASS_ID_FOR_BUILTINS);
        }

        Name implClassName = JvmFileClassUtil.getImplClassName(descriptor);
        assert implClassName != null : "No implClassName for " + descriptor;
        String implSimpleName = implClassName.asString();

        String facadeSimpleName;

        if (containingDeclaration instanceof LazyJavaPackageFragment) {
            facadeSimpleName = ((LazyJavaPackageFragment) containingDeclaration).getFacadeSimpleNameForPartSimpleName(implSimpleName);
            if (facadeSimpleName == null) return null;
        }
        else if (containingDeclaration instanceof IncrementalMultifileClassPackageFragment) {
            facadeSimpleName = ((IncrementalMultifileClassPackageFragment) containingDeclaration).getMultifileClassName().asString();
        }
        else {
            throw new AssertionError("Unexpected package fragment for " + descriptor + ": " +
                                     containingDeclaration + " (" + containingDeclaration.getClass().getSimpleName() + ")");
        }

        return ContainingClassesInfo.forPackageMember(
                ((PackageFragmentDescriptor) containingDeclaration).getFqName(), facadeSimpleName, implSimpleName
        );
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
            return mapReturnType(CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView((SimpleFunctionDescriptor) descriptor), sw);
        }

        if (TypeSignatureMappingKt.hasVoidReturnType(descriptor)) {
            if (sw != null) {
                sw.writeAsmType(Type.VOID_TYPE);
            }
            return Type.VOID_TYPE;
        }
        else if (descriptor instanceof FunctionDescriptor && forceBoxedReturnType((FunctionDescriptor) descriptor)) {
            // GENERIC_TYPE is a hack to automatically box the return type
            //noinspection ConstantConditions
            return mapType(descriptor.getReturnType(), sw, TypeMappingMode.GENERIC_ARGUMENT);
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
        return mapType(classifier.getDefaultType(), null, TypeMappingMode.DEFAULT);
    }

    @NotNull
    public Type mapType(@NotNull KotlinType jetType) {
        return mapType(jetType, null, TypeMappingMode.DEFAULT);
    }

    @NotNull
    public Type mapType(@NotNull CallableDescriptor descriptor) {
        //noinspection ConstantConditions
        return mapType(descriptor.getReturnType());
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
            @Nullable final JvmSignatureWriter signatureVisitor,
            @NotNull TypeMappingMode mode
    ) {
        return TypeSignatureMappingKt.mapType(
                kotlinType, AsmTypeFactory.INSTANCE, mode, typeMappingConfiguration, signatureVisitor,
                new Function3<KotlinType, Type, TypeMappingMode, Unit>() {
                    @Override
                    public Unit invoke(KotlinType kotlinType, Type type, TypeMappingMode mode) {
                        writeGenericType(kotlinType, type, signatureVisitor, mode);
                        return Unit.INSTANCE;
                    }
                });
    }

    @NotNull
    public Type mapDefaultImpls(@NotNull ClassDescriptor descriptor) {
        String defaultImplsClassName = typeMappingConfiguration.getInnerClassNameFactory().invoke(
                mapType(descriptor).getInternalName(), JvmAbi.DEFAULT_IMPLS_CLASS_NAME);
        return Type.getObjectType(defaultImplsClassName);
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

        if (innerTypesAsList.size() == 1) {
            signatureVisitor.writeClassBegin(asmType);
        }
        else {
            ClassDescriptor outermostClass = innerTypesAsList.get(0).getClassDescriptor();
            signatureVisitor.writeOuterClassBegin(asmType, mapType(outermostClass.getDefaultType()).getInternalName());
        }

        for (int i = 0; i < innerTypesAsList.size(); i++) {
            PossiblyInnerType innerPart = innerTypesAsList.get(i);
            if (i > 0) {
                signatureVisitor.writeInnerClass(getJvmShortName(innerPart.getClassDescriptor()));
            }
            writeGenericArguments(signatureVisitor, innerPart, mode);
        }

        signatureVisitor.writeClassEnd();
    }

    @Nullable
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
                                TypeMappingUtil.getEffectiveVariance(parameter.getVariance(), argument.getProjectionKind())));

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
    private static Variance getVarianceForWildcard(
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
            else if (isSuperCall && !isJvm8TargetWithDefaults && !isInterface(descriptor.getContainingDeclaration())) {
                //Don't unwrap fake overrides from class to interface cause substituted override would be implicitly generated for target 1.6
                return descriptor;
            }

            descriptor = overridden.iterator().next();
        }
        return descriptor;
    }

    @NotNull
    public CallableMethod mapToCallableMethod(@NotNull FunctionDescriptor descriptor, boolean superCall) {
        if (descriptor instanceof TypeAliasConstructorDescriptor) {
            return mapToCallableMethod(((TypeAliasConstructorDescriptor) descriptor).getUnderlyingConstructorDescriptor(), superCall);
        }

        if (descriptor instanceof ClassConstructorDescriptor) {
            JvmMethodSignature method = mapSignatureSkipGeneric(descriptor);
            Type owner = mapClass(((ClassConstructorDescriptor) descriptor).getContainingDeclaration());
            String defaultImplDesc = mapDefaultMethod(descriptor, OwnerKind.IMPLEMENTATION).getDescriptor();
            return new CallableMethod(owner, owner, defaultImplDesc, method, INVOKESPECIAL, null, null, null, false);
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
        Type owner;
        Type ownerForDefaultImpl;
        FunctionDescriptor baseMethodDescriptor;
        int invokeOpcode;
        Type thisClass;
        boolean isInterfaceMember = false;

        if (functionParent instanceof ClassDescriptor) {
            FunctionDescriptor declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor);

            ClassDescriptor currentOwner = (ClassDescriptor) functionParent;
            ClassDescriptor declarationOwner = (ClassDescriptor) declarationFunctionDescriptor.getContainingDeclaration();

            boolean originalIsInterface = isJvmInterface(declarationOwner);
            boolean currentIsInterface = isJvmInterface(currentOwner);

            boolean isInterface = currentIsInterface && originalIsInterface;

            baseMethodDescriptor = findBaseDeclaration(functionDescriptor).getOriginal();
            ClassDescriptor ownerForDefault = (ClassDescriptor) baseMethodDescriptor.getContainingDeclaration();
            ownerForDefaultImpl =
                    isJvmInterface(ownerForDefault) && !isJvm8InterfaceWithDefaults(ownerForDefault) ?
                    mapDefaultImpls(ownerForDefault) : mapClass(ownerForDefault);

            if (isInterface && (superCall || descriptor.getVisibility() == Visibilities.PRIVATE || isAccessor(descriptor))) {
                thisClass = mapClass(currentOwner);
                if (declarationOwner instanceof JavaClassDescriptor || isJvm8InterfaceWithDefaults(declarationOwner)) {
                    invokeOpcode = INVOKESPECIAL;
                    signature = mapSignatureSkipGeneric(functionDescriptor);
                    owner = thisClass;
                    isInterfaceMember = true;
                }
                else {
                    invokeOpcode = INVOKESTATIC;
                    signature = mapSignatureSkipGeneric(descriptor.getOriginal(), OwnerKind.DEFAULT_IMPLS);
                    owner = mapDefaultImpls(currentOwner);
                }
            }
            else {
                boolean isStaticInvocation = (isStaticDeclaration(functionDescriptor) &&
                                              !(functionDescriptor instanceof ImportedFromObjectCallableDescriptor)) ||
                                             isStaticAccessor(functionDescriptor) ||
                                             CodegenUtilKt.isJvmStaticInObjectOrClass(functionDescriptor);
                if (isStaticInvocation) {
                    invokeOpcode = INVOKESTATIC;
                    isInterfaceMember = currentIsInterface && currentOwner instanceof JavaClassDescriptor;
                }
                else if (isInterface) {
                    invokeOpcode = INVOKEINTERFACE;
                    isInterfaceMember = true;
                }
                else {
                    boolean isPrivateFunInvocation = Visibilities.isPrivate(functionDescriptor.getVisibility());
                    invokeOpcode = superCall || isPrivateFunInvocation ? INVOKESPECIAL : INVOKEVIRTUAL;
                    isInterfaceMember = superCall && currentIsInterface;
                }

                FunctionDescriptor overriddenSpecialBuiltinFunction =
                        SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(functionDescriptor.getOriginal());
                FunctionDescriptor functionToCall = overriddenSpecialBuiltinFunction != null && !superCall
                                                    ? overriddenSpecialBuiltinFunction.getOriginal()
                                                    : functionDescriptor.getOriginal();

                signature = mapSignatureSkipGeneric(functionToCall);

                ClassDescriptor receiver = (currentIsInterface && !originalIsInterface) || currentOwner instanceof FunctionClassDescriptor
                                           ? declarationOwner
                                           : currentOwner;
                owner = mapClass(receiver);
                thisClass = owner;
            }
        }
        else {
            signature = mapSignatureSkipGeneric(functionDescriptor.getOriginal());
            owner = mapOwner(functionDescriptor);
            ownerForDefaultImpl = owner;
            baseMethodDescriptor = functionDescriptor;
            if (functionParent instanceof PackageFragmentDescriptor) {
                invokeOpcode = INVOKESTATIC;
                thisClass = null;
            }
            else if (functionDescriptor instanceof ConstructorDescriptor) {
                invokeOpcode = INVOKESPECIAL;
                thisClass = null;
            }
            else {
                invokeOpcode = INVOKEVIRTUAL;
                thisClass = owner;
            }
        }

        Type calleeType = isLocalFunction(functionDescriptor) ? owner : null;

        Type receiverParameterType;
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getOriginal().getExtensionReceiverParameter();
        if (receiverParameter != null) {
            receiverParameterType = mapType(receiverParameter.getType());
        }
        else {
            receiverParameterType = null;
        }

        String defaultImplDesc = mapDefaultMethod(baseMethodDescriptor, getKindForDefaultImplCall(baseMethodDescriptor)).getDescriptor();

        return new CallableMethod(
                owner, ownerForDefaultImpl, defaultImplDesc, signature, invokeOpcode,
                thisClass, receiverParameterType, calleeType,
                isJvm8Target ? isInterfaceMember : invokeOpcode == INVOKEINTERFACE );
    }

    private boolean isJvm8InterfaceWithDefaults(@NotNull ClassDescriptor ownerForDefault) {
        return isJvmInterface(ownerForDefault) &&
               JvmCodegenUtil.isJvm8InterfaceWithDefaults(ownerForDefault, isJvm8Target, isJvm8TargetWithDefaults);
    }

    public static boolean isAccessor(@NotNull CallableMemberDescriptor descriptor) {
        return descriptor instanceof AccessorForCallableDescriptor<?>;
    }

    public static boolean isStaticAccessor(@NotNull CallableMemberDescriptor descriptor) {
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
                        return samType.getAbstractMethod().getName().asString();
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
            !descriptor.getAnnotations().hasAnnotation(KotlinBuiltIns.FQ_NAMES.publishedApi)) {
            return name + "$" + NameUtils.sanitizeAsJavaIdentifier(moduleName);
        }

        return name;
    }

    @Nullable
    private String getPartSimpleNameForMangling(@NotNull CallableMemberDescriptor descriptor) {
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
        return mapSignature(descriptor, true).getAsmMethod();
    }

    @NotNull
    public Method mapAsmMethod(@NotNull FunctionDescriptor descriptor, @NotNull OwnerKind kind) {
        return mapSignature(descriptor, kind, true).getAsmMethod();
    }

    @NotNull
    private JvmMethodGenericSignature mapSignature(@NotNull FunctionDescriptor f, boolean skipGenericSignature) {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, skipGenericSignature);
    }

    @NotNull
    public JvmMethodSignature mapSignatureSkipGeneric(@NotNull FunctionDescriptor f) {
        return mapSignatureSkipGeneric(f, OwnerKind.IMPLEMENTATION);
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
    private JvmMethodGenericSignature mapSignature(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind, boolean skipGenericSignature) {
        if (f.getInitialSignatureDescriptor() != null && f != f.getInitialSignatureDescriptor()) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if (SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(f) == null ||
                f.getContainingDeclaration().getOriginal() instanceof JavaClassDescriptor) {
                return mapSignature(f.getInitialSignatureDescriptor(), kind, skipGenericSignature);
            }
        }

        if (CoroutineCodegenUtilKt.isSuspendFunctionNotSuspensionView(f)) {
            return mapSignature(CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(f), kind, skipGenericSignature);
        }

        if (f instanceof ConstructorDescriptor) {
            return mapSignature(f, kind, f.getOriginal().getValueParameters(), skipGenericSignature);
        }

        return mapSignature(f, kind, f.getValueParameters(), skipGenericSignature);
    }

    @NotNull
    public JvmMethodGenericSignature mapSignature(
            @NotNull FunctionDescriptor f,
            @NotNull OwnerKind kind,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            boolean skipGenericSignature
    ) {
        if (f instanceof FunctionImportedFromObject) {
            return mapSignature(((FunctionImportedFromObject) f).getCallableFromObject(), kind, skipGenericSignature);
        }
        else if (f instanceof TypeAliasConstructorDescriptor) {
            return mapSignature(((TypeAliasConstructorDescriptor) f).getUnderlyingConstructorDescriptor(), kind, valueParameters, skipGenericSignature);
        }

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

        if (kind != OwnerKind.DEFAULT_IMPLS) {
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
        if (!(descriptor instanceof DeserializedCallableMemberDescriptor)) return;

        KotlinJvmBinaryClass ownerClass = null;

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container instanceof DeserializedClassDescriptor) {
            SourceElement source = ((DeserializedClassDescriptor) container).getSource();
            if (source instanceof KotlinJvmBinarySourceElement) {
                ownerClass = ((KotlinJvmBinarySourceElement) source).getBinaryClass();
            }
        }
        else if (container instanceof LazyJavaPackageFragment) {
            SourceElement source = ((LazyJavaPackageFragment) container).getSource();
            if (source instanceof KotlinJvmBinaryPackageSourceElement) {
                ownerClass = ((KotlinJvmBinaryPackageSourceElement) source).getRepresentativeBinaryClass();
            }
        }

        if (ownerClass != null) {
            JvmBytecodeBinaryVersion version = ownerClass.getClassHeader().getBytecodeVersion();
            if (!version.isCompatible()) {
                incompatibleClassTracker.record(ownerClass);
            }
        }
    }

    @NotNull
    private static String getDefaultDescriptor(@NotNull Method method, @Nullable String dispatchReceiverDescriptor, boolean isExtension) {
        String descriptor = method.getDescriptor();
        int argumentsCount = Type.getArgumentTypes(descriptor).length;
        if (isExtension) {
            argumentsCount--;
        }
        int maskArgumentsCount = (argumentsCount + Integer.SIZE - 1) / Integer.SIZE;
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
                functionDescriptor.getExtensionReceiverParameter() != null
        );

        return new Method(isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, descriptor);
    }

    /**
     * @return true iff a given function descriptor should be compiled to a method with boxed return type regardless of whether return type
     * of that descriptor is nullable or not. This happens when a function returning a value of a primitive type overrides another function
     * with a non-primitive return type. In that case the generated method's return type should be boxed: otherwise it's not possible to use
     * this class from Java since javac issues errors when loading the class (incompatible return types)
     */
    private static boolean forceBoxedReturnType(@NotNull FunctionDescriptor descriptor) {
        //noinspection ConstantConditions
        if (!KotlinBuiltIns.isPrimitiveType(descriptor.getReturnType())) return false;

        for (FunctionDescriptor overridden : getAllOverriddenDescriptors(descriptor)) {
            //noinspection ConstantConditions
            if (!KotlinBuiltIns.isPrimitiveType(overridden.getReturnType())) return true;
        }

        return false;
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
                        VariableDescriptor delegateVariableDescriptor = bindingContext.get(LOCAL_VARIABLE_DELEGATE,
                                                                                           (VariableDescriptor) variableDescriptor);
                        assert delegateVariableDescriptor != null :
                                "Local delegated property " + variableDescriptor + " delegate descriptor should be not null";
                        sharedVarType = mapType(delegateVariableDescriptor.getType());
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
        List<ResolvedValueArgument> valueArguments = superCall.getValueArgumentsByIndex();
        assert valueArguments != null : "Failed to arrange value arguments by index: " + superDescriptor;

        List<JvmMethodParameterSignature> parameters = mapSignatureSkipGeneric(superDescriptor).getValueParameters();

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
            for (int i = 0; i < args; i++) {
                ResolvedValueArgument valueArgument = valueArguments.get(i);
                JvmMethodParameterSignature parameter = parameters.get(params - args + i);
                if (!(valueArgument instanceof DefaultValueArgument)) {
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
            descriptor = next.getResultingDescriptor();
            if (descriptor.getContainingDeclaration() != constructorOwner) return next;
        }
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        sw.writeParametersStart();

        for (ScriptDescriptor importedScript : importedScripts) {
            writeParameter(sw, importedScript.getDefaultType(), /* callableDescriptor = */ null);
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
}
