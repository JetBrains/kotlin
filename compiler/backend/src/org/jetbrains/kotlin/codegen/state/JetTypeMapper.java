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

package org.jetbrains.kotlin.codegen.state;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.BuiltinsPackageFragment;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.load.java.BuiltinsPropertiesUtilKt;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.SpecialSignatureInfo;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageScope;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.name.*;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetFunctionLiteral;
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.AbstractScopeAdapter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.serialization.deserialization.DeserializedType;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isUnit;
import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.DEFAULT_CONSTRUCTOR_MARKER;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class JetTypeMapper {
    private final BindingContext bindingContext;
    private final ClassBuilderMode classBuilderMode;
    private final JvmFileClassesProvider fileClassesProvider;
    private final IncrementalCache incrementalCache;
    private final String moduleName;

    public JetTypeMapper(
            @NotNull BindingContext bindingContext,
            @NotNull ClassBuilderMode classBuilderMode,
            @NotNull JvmFileClassesProvider fileClassesProvider,
            @Nullable IncrementalCache incrementalCache,
            @NotNull String moduleName
    ) {
        this.bindingContext = bindingContext;
        this.classBuilderMode = classBuilderMode;
        this.fileClassesProvider = fileClassesProvider;
        this.incrementalCache = incrementalCache;
        this.moduleName = moduleName;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    private enum JetTypeMapperMode {
        /**
         * foo.Bar is mapped to Lfoo/Bar;
         */
        IMPL,
        /**
         * kotlin.Int is mapped to I
         */
        VALUE,
        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         */
        TYPE_PARAMETER,
        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         * No projections allowed in immediate arguments
         */
        SUPER_TYPE,
        /**
         * kotlin.reflect.KClass mapped to java.lang.Class
         * Other types mapped as VALUE
         */
        VALUE_FOR_ANNOTATION,
        /**
         * kotlin.reflect.KClass mapped to java.lang.Class
         * Other types mapped as TYPE_PARAMETER
         */
        TYPE_PARAMETER_FOR_ANNOTATION;

        boolean isForAnnotation() {
            return this == VALUE_FOR_ANNOTATION || this == TYPE_PARAMETER_FOR_ANNOTATION;
        }
    }

    @NotNull
    public Type mapOwner(@NotNull DeclarationDescriptor descriptor) {
        return mapOwner(descriptor, false);
    }

    @NotNull
    private Type mapOwner(@NotNull DeclarationDescriptor descriptor, boolean isImplementation) {
        if (isLocalFunction(descriptor)) {
            return asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container instanceof PackageFragmentDescriptor) {
            return Type.getObjectType(internalNameForPackageMemberOwner((CallableMemberDescriptor) descriptor, isImplementation));
        }
        else if (container instanceof ClassDescriptor) {
            return mapClass((ClassDescriptor) container);
        }
        else if (container instanceof ScriptDescriptor) {
            return asmTypeForScriptDescriptor(bindingContext, (ScriptDescriptor) container);
        }
        else {
            throw new UnsupportedOperationException("Don't know how to map owner for " + descriptor);
        }
    }

    @NotNull
    private String internalNameForPackageMemberOwner(@NotNull CallableMemberDescriptor descriptor, boolean isImplementation) {
        JetFile file = DescriptorToSourceUtils.getContainingFile(descriptor);
        if (file != null) {
            Visibility visibility = descriptor.getVisibility();
            if (isImplementation || descriptor instanceof PropertyDescriptor || Visibilities.isPrivate(visibility)) {
                return FileClasses.getFileClassInternalName(fileClassesProvider, file);
            }
            else {
                return FileClasses.getFacadeClassInternalName(fileClassesProvider, file);
            }
        }

        CallableMemberDescriptor directMember = getDirectMember(descriptor);

        if (directMember instanceof DeserializedCallableMemberDescriptor) {
            String facadeFqName = getPackageMemberOwnerInternalName((DeserializedCallableMemberDescriptor) directMember);
            if (facadeFqName != null) return facadeFqName;
        }

        throw new RuntimeException("Unreachable state");
        //return PackageClassUtils.getPackageClassInternalName(packageFragment.getFqName());
    }

    public static class ContainingClassesInfo {
        private final ClassId facadeClassId;
        private final ClassId implClassId;

        public ContainingClassesInfo(ClassId facadeClassId, ClassId implClassId) {
            this.facadeClassId = facadeClassId;
            this.implClassId = implClassId;
        }

        public ClassId getFacadeClassId() {
            return facadeClassId;
        }

        public ClassId getImplClassId() {
            return implClassId;
        }

        private static @Nullable ContainingClassesInfo forPackageMemberOrNull(
                @NotNull FqName packageFqName,
                @Nullable String facadeClassName,
                @Nullable String implClassName
        ) {
            if (facadeClassName == null || implClassName == null) {
                return null;
            }
            return new ContainingClassesInfo(ClassId.topLevel(packageFqName.child(Name.identifier(facadeClassName))),
                                             ClassId.topLevel(packageFqName.child(Name.identifier(implClassName))));
        }

        private static @Nullable ContainingClassesInfo forClassMemberOrNull(@Nullable ClassId classId) {
            if (classId == null) {
                return null;
            }
            return new ContainingClassesInfo(classId, classId);
        }
    }

    public ContainingClassesInfo getContainingClassesForDeserializedCallable(DeserializedCallableMemberDescriptor deserializedDescriptor) {
        DeclarationDescriptor parentDeclaration = deserializedDescriptor.getContainingDeclaration();
        ContainingClassesInfo containingClassesInfo;
        if (parentDeclaration instanceof PackageFragmentDescriptor) {
            containingClassesInfo = getPackageMemberContainingClassesInfo(deserializedDescriptor);
        } else {
            containingClassesInfo = ContainingClassesInfo.forClassMemberOrNull(
                    InlineCodegenUtil.getContainerClassId(deserializedDescriptor));
        }
        if (containingClassesInfo == null) {
            throw new IllegalStateException("Couldn't find container for " + deserializedDescriptor.getName());
        }
        return containingClassesInfo;
    }

    @Nullable
    private String getPackageMemberOwnerInternalName(@NotNull DeserializedCallableMemberDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        assert containingDeclaration instanceof PackageFragmentDescriptor : "Not a top-level member: " + descriptor;

        ContainingClassesInfo containingClasses = getPackageMemberContainingClassesInfo(descriptor);
        if (containingClasses == null) {
            return null;
        }

        return JvmClassName.byClassId(containingClasses.getFacadeClassId()).getInternalName();
    }

    @Nullable
    private ContainingClassesInfo getPackageMemberContainingClassesInfo(@NotNull DeserializedCallableMemberDescriptor descriptor) {
        // XXX This method (and getPackageMemberOwnerShortName) is a dirty hack.
        // We need some safe, concise way to identify multifile facade and multifile part
        // from a deserialized package member descriptor.
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        assert containingDeclaration instanceof PackageFragmentDescriptor
                : "Package member expected, got " + descriptor + " in " + containingDeclaration;
        PackageFragmentDescriptor packageFragmentDescriptor = (PackageFragmentDescriptor) containingDeclaration;

        if (packageFragmentDescriptor instanceof BuiltinsPackageFragment) {
            ClassId builtinsFacadeClassId = ClassId.topLevel(PackageClassUtils.getPackageClassFqName(packageFragmentDescriptor.getFqName()));
            return new ContainingClassesInfo(builtinsFacadeClassId, builtinsFacadeClassId);
        }

        Name implClassName = JvmFileClassUtil.getImplClassName(descriptor);
        assert implClassName != null : "No implClassName for " + descriptor;
        String implSimpleName = implClassName.asString();

        String facadeSimpleName;

        JetScope scope = packageFragmentDescriptor.getMemberScope();
        if (scope instanceof AbstractScopeAdapter) {
            scope = ((AbstractScopeAdapter) scope).getActualScope();
        }
        if (scope instanceof LazyJavaPackageScope) {
            facadeSimpleName = ((LazyJavaPackageScope) scope).getFacadeSimpleNameForPartSimpleName(implClassName.asString());
        }
        else if (packageFragmentDescriptor instanceof IncrementalPackageFragmentProvider.IncrementalPackageFragment) {
            assert incrementalCache != null
                    : "IncrementalPackageFragment found outside of incremental compilation context " +
                      "for " + descriptor + " in package " + packageFragmentDescriptor;

            String implClassInternalName = internalNameByFqNameWithoutInnerClasses(
                    packageFragmentDescriptor.getFqName().child(implClassName));
            String facadeClassInternalName = incrementalCache.getMultifileFacade(implClassInternalName);
            if (facadeClassInternalName == null) {
                facadeSimpleName = implClassName.asString();
            }
            else {
                facadeSimpleName = getSimpleInternalName(facadeClassInternalName);
            }
        }
        else {
            throw new AssertionError("Unexpected package member scope for " + descriptor + ": " +
                                     scope + " :" + scope.getClass().getSimpleName());
        }
        return ContainingClassesInfo.forPackageMemberOrNull(packageFragmentDescriptor.getFqName(), facadeSimpleName, implSimpleName);
    }

    @NotNull
    public Type mapReturnType(@NotNull CallableDescriptor descriptor) {
        return mapReturnType(descriptor, null);
    }

    @NotNull
    private Type mapReturnType(@NotNull CallableDescriptor descriptor, @Nullable BothSignatureWriter sw) {
        JetType returnType = descriptor.getReturnType();
        assert returnType != null : "Function has no return type: " + descriptor;

        if (descriptor instanceof ConstructorDescriptor) {
            return Type.VOID_TYPE;
        }

        if (isUnit(returnType) && !TypeUtils.isNullableType(returnType) && !(descriptor instanceof PropertyGetterDescriptor)) {
            if (sw != null) {
                sw.writeAsmType(Type.VOID_TYPE);
            }
            return Type.VOID_TYPE;
        }
        else if (descriptor instanceof FunctionDescriptor && forceBoxedReturnType((FunctionDescriptor) descriptor)) {
            // TYPE_PARAMETER is a hack to automatically box the return type
            //noinspection ConstantConditions
            return mapType(descriptor.getReturnType(), sw, JetTypeMapperMode.TYPE_PARAMETER);
        }
        else if (DescriptorUtils.isAnnotationClass(descriptor.getContainingDeclaration())) {
            //noinspection ConstantConditions
            return mapType(descriptor.getReturnType(), sw, JetTypeMapperMode.VALUE_FOR_ANNOTATION);
        }
        else {
            return mapType(returnType, sw, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE);
        }
    }

    @NotNull
    private Type mapType(@NotNull JetType jetType, @NotNull JetTypeMapperMode mode) {
        return mapType(jetType, null, mode);
    }

    @NotNull
    public Type mapSupertype(@NotNull JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        return mapType(jetType, signatureVisitor, JetTypeMapperMode.SUPER_TYPE);
    }

    @NotNull
    public Type mapTypeParameter(@NotNull JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        return mapType(jetType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
    }

    @NotNull
    public Type mapClass(@NotNull ClassifierDescriptor classifier) {
        return mapType(classifier.getDefaultType(), null, JetTypeMapperMode.IMPL);
    }

    @NotNull
    public Type mapType(@NotNull JetType jetType) {
        return mapType(jetType, null, JetTypeMapperMode.VALUE);
    }

    @NotNull
    public Type mapType(@NotNull CallableDescriptor descriptor) {
        //noinspection ConstantConditions
        return mapType(descriptor.getReturnType());
    }

    @NotNull
    public JvmMethodSignature mapAnnotationParameterSignature(@NotNull PropertyDescriptor descriptor) {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);
        sw.writeReturnType();
        mapType(descriptor.getType(), sw, JetTypeMapperMode.VALUE_FOR_ANNOTATION);
        sw.writeReturnTypeEnd();
        return sw.makeJvmMethodSignature(descriptor.getName().asString());
    }

    @NotNull
    public Type mapType(@NotNull ClassifierDescriptor descriptor) {
        return mapType(descriptor.getDefaultType());
    }

    @NotNull
    private Type mapType(@NotNull JetType jetType, @Nullable BothSignatureWriter signatureVisitor, @NotNull JetTypeMapperMode mode) {
        return mapType(jetType, signatureVisitor, mode, Variance.INVARIANT);
    }

    @NotNull
    private Type mapType(
            @NotNull JetType jetType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull JetTypeMapperMode kind,
            @NotNull Variance howThisTypeIsUsed
    ) {
        Type known = mapBuiltinType(jetType);

        boolean projectionsAllowed = kind != JetTypeMapperMode.SUPER_TYPE;
        if (known != null) {
            if (kind == JetTypeMapperMode.VALUE || kind == JetTypeMapperMode.VALUE_FOR_ANNOTATION) {
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            }
            else if (kind == JetTypeMapperMode.TYPE_PARAMETER || kind == JetTypeMapperMode.SUPER_TYPE ||
                     kind == JetTypeMapperMode.TYPE_PARAMETER_FOR_ANNOTATION) {
                return mapKnownAsmType(jetType, boxType(known), signatureVisitor, howThisTypeIsUsed, projectionsAllowed);
            }
            else if (kind == JetTypeMapperMode.IMPL) {
                // TODO: enable and fix tests
                //throw new IllegalStateException("must not map known type to IMPL when not compiling builtins: " + jetType);
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            }
            else {
                throw new IllegalStateException("unknown kind: " + kind);
            }
        }

        TypeConstructor constructor = jetType.getConstructor();
        DeclarationDescriptor descriptor = constructor.getDeclarationDescriptor();
        if (constructor instanceof IntersectionTypeConstructor) {
            jetType = CommonSupertypes.commonSupertype(new ArrayList<JetType>(constructor.getSupertypes()));
        }

        if (descriptor == null) {
            throw new UnsupportedOperationException("no descriptor for type constructor of " + jetType);
        }

        if (ErrorUtils.isError(descriptor)) {
            if (classBuilderMode != ClassBuilderMode.LIGHT_CLASSES) {
                throw new IllegalStateException(generateErrorMessageForErrorType(jetType, descriptor));
            }
            Type asmType = Type.getObjectType("error/NonExistentClass");
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(asmType);
            }
            return asmType;
        }

        if (descriptor instanceof ClassDescriptor && KotlinBuiltIns.isArray(jetType)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberProjection = jetType.getArguments().get(0);
            JetType memberType = memberProjection.getType();

            Type arrayElementType;
            if (memberProjection.getProjectionKind() == Variance.IN_VARIANCE) {
                arrayElementType = AsmTypes.OBJECT_TYPE;
                if (signatureVisitor != null) {
                    signatureVisitor.writeArrayType();
                    signatureVisitor.writeAsmType(arrayElementType);
                    signatureVisitor.writeArrayEnd();
                }
            }
            else {
                arrayElementType = boxType(mapType(memberType, kind));
                if (signatureVisitor != null) {
                    signatureVisitor.writeArrayType();
                    JetTypeMapperMode newMode = kind.isForAnnotation() ?
                                                JetTypeMapperMode.TYPE_PARAMETER_FOR_ANNOTATION :
                                                JetTypeMapperMode.TYPE_PARAMETER;
                    mapType(memberType, signatureVisitor, newMode, memberProjection.getProjectionKind());
                    signatureVisitor.writeArrayEnd();
                }
            }

            return Type.getType("[" + arrayElementType.getDescriptor());
        }

        if (descriptor instanceof ClassDescriptor) {
            Type asmType = kind.isForAnnotation() && KotlinBuiltIns.isKClass((ClassDescriptor) descriptor) ?
                           AsmTypes.JAVA_CLASS_TYPE :
                           computeAsmType((ClassDescriptor) descriptor.getOriginal());
            writeGenericType(signatureVisitor, asmType, jetType, howThisTypeIsUsed, projectionsAllowed);
            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            Type type = mapType(typeParameterDescriptor.getUpperBounds().iterator().next(), kind);
            if (signatureVisitor != null) {
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), type);
            }
            return type;
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    @Nullable
    private static Type mapBuiltinType(@NotNull JetType type) {
        DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(descriptor instanceof ClassDescriptor)) return null;

        FqNameUnsafe fqName = DescriptorUtils.getFqName(descriptor);

        PrimitiveType primitiveType = KotlinBuiltIns.getPrimitiveTypeByFqName(fqName);
        if (primitiveType != null) {
            Type asmType = Type.getType(JvmPrimitiveType.get(primitiveType).getDesc());
            return TypeUtils.isNullableType(type) ? boxType(asmType) : asmType;
        }

        PrimitiveType arrayElementType = KotlinBuiltIns.getPrimitiveTypeByArrayClassFqName(fqName);
        if (arrayElementType != null) {
            return Type.getType("[" + JvmPrimitiveType.get(arrayElementType).getDesc());
        }

        ClassId classId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqName);
        if (classId != null) {
            return Type.getObjectType(JvmClassName.byClassId(classId).getInternalName());
        }

        return null;
    }

    @NotNull
    private Type computeAsmType(@NotNull ClassDescriptor klass) {
        Type alreadyComputedType = bindingContext.get(ASM_TYPE, klass);
        if (alreadyComputedType != null) {
            return alreadyComputedType;
        }

        Type asmType = Type.getObjectType(computeAsmTypeImpl(klass));
        assert PsiCodegenPredictor.checkPredictedNameFromPsi(klass, asmType, fileClassesProvider);
        return asmType;
    }

    @NotNull
    private String computeAsmTypeImpl(@NotNull ClassDescriptor klass) {
        DeclarationDescriptor container = klass.getContainingDeclaration();

        String name = SpecialNames.safeIdentifier(klass.getName()).getIdentifier();
        if (container instanceof PackageFragmentDescriptor) {
            FqName fqName = ((PackageFragmentDescriptor) container).getFqName();
            return fqName.isRoot() ? name : fqName.asString().replace('.', '/') + '/' + name;
        }

        if (container instanceof ScriptDescriptor) {
            return asmTypeForScriptDescriptor(bindingContext, (ScriptDescriptor) container).getInternalName() + "$" + name;
        }

        assert container instanceof ClassDescriptor : "Unexpected container: " + container + " for " + klass;

        String containerInternalName = computeAsmTypeImpl((ClassDescriptor) container);
        return klass.getKind() == ClassKind.ENUM_ENTRY ? containerInternalName : containerInternalName + "$" + name;
    }

    @NotNull
    public Type mapDefaultImpls(@NotNull ClassDescriptor descriptor) {
        return Type.getObjectType(mapType(descriptor).getInternalName() + JvmAbi.DEFAULT_IMPLS_SUFFIX);
    }

    @NotNull
    private static String generateErrorMessageForErrorType(@NotNull JetType type, @NotNull DeclarationDescriptor descriptor) {
        PsiElement declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);

        if (declarationElement == null) {
            String message = "Error type encountered: %s (%s).";
            if (TypesPackage.upperIfFlexible(type) instanceof DeserializedType) {
                message +=
                        " One of the possible reasons may be that this type is not directly accessible from this module. " +
                        "To workaround this error, try adding an explicit dependency on the module or library which contains this type " +
                        "to the classpath";
            }
            return String.format(message, type, type.getClass().getSimpleName());
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
            BothSignatureWriter signatureVisitor,
            Type asmType,
            JetType jetType,
            Variance howThisTypeIsUsed,
            boolean projectionsAllowed
    ) {
        if (signatureVisitor != null) {
            signatureVisitor.writeClassBegin(asmType);

            List<TypeProjection> arguments = jetType.getArguments();
            for (TypeParameterDescriptor parameter : jetType.getConstructor().getParameters()) {
                TypeProjection argument = arguments.get(parameter.getIndex());

                if (projectionsAllowed && argument.isStarProjection()) {
                    signatureVisitor.writeUnboundedWildcard();
                }
                else {
                    Variance projectionKind = projectionsAllowed
                                              ? getEffectiveVariance(
                            parameter.getVariance(),
                            argument.getProjectionKind(),
                            howThisTypeIsUsed
                    )
                                              : Variance.INVARIANT;
                    signatureVisitor.writeTypeArgument(projectionKind);

                    mapType(argument.getType(), signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                    signatureVisitor.writeTypeArgumentEnd();
                }
            }
            signatureVisitor.writeClassEnd();
        }
    }

    private static Variance getEffectiveVariance(Variance parameterVariance, Variance projectionKind, Variance howThisTypeIsUsed) {
        // Return type must not contain wildcards
        if (howThisTypeIsUsed == Variance.OUT_VARIANCE) return projectionKind;

        if (parameterVariance == Variance.INVARIANT) {
            return projectionKind;
        }
        if (projectionKind == Variance.INVARIANT) {
            return parameterVariance;
        }
        if (parameterVariance == projectionKind) {
            return parameterVariance;
        }

        // In<out X> = In<*>
        // Out<in X> = Out<*>
        return Variance.OUT_VARIANCE;
    }

    private Type mapKnownAsmType(
            JetType jetType,
            Type asmType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull Variance howThisTypeIsUsed
    ) {
        return mapKnownAsmType(jetType, asmType, signatureVisitor, howThisTypeIsUsed, true);
    }

    private Type mapKnownAsmType(
            JetType jetType,
            Type asmType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull Variance howThisTypeIsUsed,
            boolean allowProjections
    ) {
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                signatureVisitor.writeAsmType(asmType);
            }
            else {
                writeGenericType(signatureVisitor, asmType, jetType, howThisTypeIsUsed, allowProjections);
            }
        }
        return asmType;
    }

    @NotNull
    public CallableMethod mapToCallableMethod(@NotNull FunctionDescriptor descriptor, boolean superCall) {
        if (descriptor instanceof ConstructorDescriptor) {
            JvmMethodSignature method = mapSignature(descriptor);
            Type owner = mapClass(((ConstructorDescriptor) descriptor).getContainingDeclaration());
            return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
        }

        DeclarationDescriptor functionParent = descriptor.getOriginal().getContainingDeclaration();

        FunctionDescriptor functionDescriptor = unwrapFakeOverride(descriptor.getOriginal());

        JvmMethodSignature signature;
        Type owner;
        Type ownerForDefaultImpl;
        Type ownerForDefaultParam;
        int invokeOpcode;
        Type thisClass;

        if (functionParent instanceof ClassDescriptor) {
            FunctionDescriptor declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor);

            ClassDescriptor currentOwner = (ClassDescriptor) functionParent;
            ClassDescriptor declarationOwner = (ClassDescriptor) declarationFunctionDescriptor.getContainingDeclaration();

            boolean originalIsInterface = isJvmInterface(declarationOwner);
            boolean currentIsInterface = isJvmInterface(currentOwner);

            boolean isInterface = currentIsInterface && originalIsInterface;

            ClassDescriptor ownerForDefault = (ClassDescriptor) findBaseDeclaration(functionDescriptor).getContainingDeclaration();
            ownerForDefaultParam = mapClass(ownerForDefault);
            ownerForDefaultImpl = isJvmInterface(ownerForDefault) ? mapDefaultImpls(ownerForDefault) : ownerForDefaultParam;

            if (isInterface && (superCall || descriptor.getVisibility() == Visibilities.PRIVATE)) {
                thisClass = mapClass(currentOwner);
                if (declarationOwner instanceof JavaClassDescriptor) {
                    invokeOpcode = INVOKESPECIAL;
                    signature = mapSignature(functionDescriptor);
                    owner = thisClass;
                }
                else {
                    invokeOpcode = INVOKESTATIC;
                    signature = mapSignature(descriptor.getOriginal(), OwnerKind.DEFAULT_IMPLS);
                    owner = mapDefaultImpls(currentOwner);
                }
            }
            else {
                boolean isStaticInvocation = (isStaticDeclaration(functionDescriptor) &&
                                              !(functionDescriptor instanceof ImportedFromObjectCallableDescriptor)) ||
                                             isStaticAccessor(functionDescriptor) ||
                                             AnnotationsPackage.isPlatformStaticInObjectOrClass(functionDescriptor);
                if (isStaticInvocation) {
                    invokeOpcode = INVOKESTATIC;
                }
                else if (isInterface) {
                    invokeOpcode = INVOKEINTERFACE;
                }
                else {
                    boolean isPrivateFunInvocation = Visibilities.isPrivate(functionDescriptor.getVisibility());
                    invokeOpcode = superCall || isPrivateFunInvocation ? INVOKESPECIAL : INVOKEVIRTUAL;
                }

                FunctionDescriptor overriddenSpecialBuiltinFunction =
                        BuiltinsPropertiesUtilKt.<FunctionDescriptor>getBuiltinSpecialOverridden(functionDescriptor.getOriginal());
                FunctionDescriptor functionToCall = overriddenSpecialBuiltinFunction != null
                                                    ? overriddenSpecialBuiltinFunction.getOriginal()
                                                    : functionDescriptor.getOriginal();

                signature = mapSignature(functionToCall);

                ClassDescriptor receiver = (currentIsInterface && !originalIsInterface) || currentOwner instanceof FunctionClassDescriptor
                                           ? declarationOwner
                                           : currentOwner;
                owner = mapClass(receiver);
                thisClass = owner;
            }
        }
        else {
            signature = mapSignature(functionDescriptor.getOriginal());
            owner = mapOwner(functionDescriptor);
            ownerForDefaultParam = owner;
            ownerForDefaultImpl = owner;
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
        return new CallableMethod(
                owner, ownerForDefaultImpl, ownerForDefaultParam, signature, invokeOpcode,
                thisClass, receiverParameterType, calleeType);
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

        String nameForSpecialFunction = BuiltinsPropertiesUtilKt.getJvmMethodNameIfSpecial(descriptor);
        if (nameForSpecialFunction != null) return nameForSpecialFunction;

        if (descriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
            if (isAnnotationClass(property.getContainingDeclaration())) {
                return property.getName().asString();
            }

            boolean isAccessor = property instanceof AccessorForPropertyDescriptor;
            String propertyName = isAccessor
                                  ? ((AccessorForPropertyDescriptor) property).getIndexedAccessorSuffix()
                                  : property.getName().asString();

            String accessorName = descriptor instanceof PropertyGetterDescriptor
                                  ? JvmAbi.getterName(propertyName)
                                  : JvmAbi.setterName(propertyName);

            return updateMemberNameIfInternal(isAccessor ? "access$" + accessorName : accessorName, descriptor);
        }
        else if (isFunctionLiteral(descriptor)) {
            PsiElement element = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor);
            if (element instanceof JetFunctionLiteral) {
                PsiElement expression = element.getParent();
                if (expression instanceof JetFunctionLiteralExpression) {
                    SamType samType = bindingContext.get(SAM_VALUE, (JetExpression) expression);
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
            return updateMemberNameIfInternal(descriptor.getName().asString(), descriptor);
        }
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
    private String updateMemberNameIfInternal(@NotNull String name, @NotNull  CallableMemberDescriptor descriptor) {
        if (descriptor.getContainingDeclaration() instanceof ScriptDescriptor) {
            //script properties should be public
            return name;
        }

        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            return name;
        }

        if (!(descriptor instanceof ConstructorDescriptor) && descriptor.getVisibility() == Visibilities.INTERNAL) {
            return name + "$" + JvmCodegenUtil.sanitizeAsJavaIdentifier(moduleName);
        }

        return name;
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor descriptor) {
        return mapSignature(descriptor, OwnerKind.IMPLEMENTATION);
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind) {
        if (f instanceof ConstructorDescriptor) {
            return mapSignature(f, kind, f.getOriginal().getValueParameters());
        }
        return mapSignature(f, kind, f.getValueParameters());
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind,
            List<ValueParameterDescriptor> valueParameters) {
        if (f instanceof FunctionImportedFromObject) {
            return mapSignature(((FunctionImportedFromObject) f).getFunctionFromObject());
        }

        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        if (f instanceof ConstructorDescriptor) {
            sw.writeParametersStart();
            writeAdditionalConstructorParameters((ConstructorDescriptor) f, sw);

            for (ValueParameterDescriptor parameter : valueParameters) {
                writeParameter(sw, parameter.getType());
            }

            if (f instanceof AccessorForConstructorDescriptor) {
                writeParameter(sw, JvmMethodParameterKind.CONSTRUCTOR_MARKER, DEFAULT_CONSTRUCTOR_MARKER);
            }

            writeVoidReturn(sw);
        }
        else {
            writeFormalTypeParameters(getDirectMember(f).getTypeParameters(), sw);

            sw.writeParametersStart();
            writeThisIfNeeded(f, kind, sw);

            ReceiverParameterDescriptor receiverParameter = f.getExtensionReceiverParameter();
            if (receiverParameter != null) {
                writeParameter(sw, JvmMethodParameterKind.RECEIVER, receiverParameter.getType());
            }

            for (ValueParameterDescriptor parameter : valueParameters) {
                writeParameter(sw, parameter.getType());
            }

            sw.writeReturnType();
            mapReturnType(f, sw);
            sw.writeReturnTypeEnd();
        }

        JvmMethodSignature signature = sw.makeJvmMethodSignature(mapFunctionName(f));


        if (kind != OwnerKind.DEFAULT_IMPLS) {
            SpecialSignatureInfo specialSignatureInfo = BuiltinsPropertiesUtilKt.getSpecialSignatureInfo(f);

            if (specialSignatureInfo != null) {
                return new JvmMethodSignature(
                        signature.getAsmMethod(), specialSignatureInfo.getSignature(), signature.getValueParameters());
            }
        }

        return signature;
    }

    @NotNull
    public static String getDefaultDescriptor(@NotNull Method method, @Nullable String dispatchReceiverDescriptor, boolean isExtension) {
        String descriptor = method.getDescriptor();
        int argumentsCount = Type.getArgumentTypes(descriptor).length;
        if (isExtension) {
            argumentsCount--;
        }
        int maskArgumentsCount = (argumentsCount + Integer.SIZE - 1) / Integer.SIZE;
        String additionalArgs = StringUtil.repeat(Type.INT_TYPE.getDescriptor(), maskArgumentsCount);
        if (isConstructor(method)) {
            additionalArgs += DEFAULT_CONSTRUCTOR_MARKER.getDescriptor();
        }
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
        Method jvmSignature = mapSignature(functionDescriptor, kind).getAsmMethod();
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
            if (!KotlinBuiltIns.isPrimitiveType(overridden.getOriginal().getReturnType())) return true;
        }

        return false;
    }

    private static void writeVoidReturn(@NotNull BothSignatureWriter sw) {
        sw.writeReturnType();
        sw.writeAsmType(Type.VOID_TYPE);
        sw.writeReturnTypeEnd();
    }

    @Nullable
    public String mapFieldSignature(@NotNull JetType backingFieldType) {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.TYPE);
        mapType(backingFieldType, sw, JetTypeMapperMode.VALUE);
        return sw.makeJavaGenericSignature();
    }

    private void writeThisIfNeeded(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull OwnerKind kind,
            @NotNull BothSignatureWriter sw
    ) {
        ClassDescriptor thisType;
        if (kind == OwnerKind.DEFAULT_IMPLS) {
            thisType = getTraitImplThisParameterClass((ClassDescriptor) descriptor.getContainingDeclaration());
        }
        else if (isAccessor(descriptor) && descriptor.getDispatchReceiverParameter() != null) {
            thisType = (ClassDescriptor) descriptor.getContainingDeclaration();
        }
        else return;

        writeParameter(sw, JvmMethodParameterKind.THIS, thisType.getDefaultType());
    }

    @NotNull
    private static ClassDescriptor getTraitImplThisParameterClass(@NotNull ClassDescriptor traitDescriptor) {
        for (ClassDescriptor descriptor : DescriptorUtils.getSuperclassDescriptors(traitDescriptor)) {
            if (descriptor.getKind() != ClassKind.INTERFACE) {
                return descriptor;
            }
        }
        return traitDescriptor;
    }

    public void writeFormalTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull BothSignatureWriter sw) {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            writeFormalTypeParameter(typeParameter, sw);
        }
    }

    private void writeFormalTypeParameter(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull BothSignatureWriter sw) {
        if (classBuilderMode == ClassBuilderMode.LIGHT_CLASSES && typeParameterDescriptor.getName().isSpecial()) {
            // If a type parameter has no name, the code below fails, but it should recover in case of light classes
            return;
        }

        sw.writeFormalTypeParameter(typeParameterDescriptor.getName().asString());

        classBound:
        {
            sw.writeClassBound();

            for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
                if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                    if (!isJvmInterface(jetType)) {
                        mapType(jetType, sw, JetTypeMapperMode.TYPE_PARAMETER);
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

        for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
            ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
            if (classifier instanceof ClassDescriptor) {
                if (isJvmInterface(jetType)) {
                    sw.writeInterfaceBound();
                    mapType(jetType, sw, JetTypeMapperMode.TYPE_PARAMETER);
                    sw.writeInterfaceBoundEnd();
                }
            }
            else if (classifier instanceof TypeParameterDescriptor) {
                sw.writeInterfaceBound();
                mapType(jetType, sw, JetTypeMapperMode.TYPE_PARAMETER);
                sw.writeInterfaceBoundEnd();
            }
            else {
                throw new UnsupportedOperationException("Unknown classifier: " + classifier);
            }
        }
    }

    private void writeParameter(@NotNull BothSignatureWriter sw, @NotNull JetType type) {
        writeParameter(sw, JvmMethodParameterKind.VALUE, type);
    }

    private void writeParameter(@NotNull BothSignatureWriter sw, @NotNull JvmMethodParameterKind kind, @NotNull JetType type) {
        sw.writeParameterType(kind);
        mapType(type, sw, JetTypeMapperMode.VALUE);
        sw.writeParameterTypeEnd();
    }

    private static void writeParameter(@NotNull BothSignatureWriter sw, @NotNull JvmMethodParameterKind kind, @NotNull Type type) {
        sw.writeParameterType(kind);
        sw.writeAsmType(type);
        sw.writeParameterTypeEnd();
    }

    private void writeAdditionalConstructorParameters(@NotNull ConstructorDescriptor descriptor, @NotNull BothSignatureWriter sw) {
        MutableClosure closure = bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration());

        ClassDescriptor captureThis = getDispatchReceiverParameterForConstructorCall(descriptor, closure);
        if (captureThis != null) {
            writeParameter(sw, JvmMethodParameterKind.OUTER, captureThis.getDefaultType());
        }

        JetType captureReceiverType = closure != null ? closure.getCaptureReceiverType() : null;
        if (captureReceiverType != null) {
            writeParameter(sw, JvmMethodParameterKind.RECEIVER, captureReceiverType);
        }

        ClassDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration.getKind() == ClassKind.ENUM_CLASS || containingDeclaration.getKind() == ClassKind.ENUM_ENTRY) {
            writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, getBuiltIns(descriptor).getStringType());
            writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, getBuiltIns(descriptor).getIntType());
        }

        if (closure == null) return;

        for (DeclarationDescriptor variableDescriptor : closure.getCaptureVariables().keySet()) {
            Type type;
            if (variableDescriptor instanceof VariableDescriptor && !(variableDescriptor instanceof PropertyDescriptor)) {
                Type sharedVarType = getSharedVarType(variableDescriptor);
                if (sharedVarType == null) {
                    sharedVarType = mapType(((VariableDescriptor) variableDescriptor).getType());
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
        if (classBuilderMode == ClassBuilderMode.FULL) {
            ResolvedCall<ConstructorDescriptor> superCall = findFirstDelegatingSuperCall(descriptor);
            if (superCall == null) return;
            writeSuperConstructorCallParameters(sw, descriptor, superCall, captureThis != null);
        }
    }

    private void writeSuperConstructorCallParameters(
            @NotNull BothSignatureWriter sw,
            @NotNull ConstructorDescriptor descriptor,
            @NotNull ResolvedCall<ConstructorDescriptor> superCall,
            boolean hasOuter
    ) {
        ConstructorDescriptor superDescriptor = SamCodegenUtil.resolveSamAdapter(superCall.getResultingDescriptor());
        List<ResolvedValueArgument> valueArguments = superCall.getValueArgumentsByIndex();
        assert valueArguments != null : "Failed to arrange value arguments by index: " + superDescriptor;

        List<JvmMethodParameterSignature> parameters = mapSignature(superDescriptor).getValueParameters();

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
        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
        while (true) {
            ResolvedCall<ConstructorDescriptor> next = getDelegationConstructorCall(bindingContext, descriptor);
            if (next == null) return null;
            descriptor = next.getResultingDescriptor();
            if (descriptor.getContainingDeclaration() != classDescriptor) return next;
        }
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        sw.writeParametersStart();

        for (ScriptDescriptor importedScript : importedScripts) {
            ClassDescriptor descriptor = bindingContext.get(CLASS_FOR_SCRIPT, importedScript);
            assert descriptor != null : "Script not found: " + importedScript;
            writeParameter(sw, descriptor.getDefaultType());
        }

        for (ValueParameterDescriptor valueParameter : script.getScriptCodeDescriptor().getValueParameters()) {
            writeParameter(sw, valueParameter.getType());
        }

        writeVoidReturn(sw);

        return sw.makeJvmMethodSignature("<init>");
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            return asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
        }
        else if (descriptor instanceof PropertyDescriptor || descriptor instanceof FunctionDescriptor) {
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) descriptor).getExtensionReceiverParameter();
            assert receiverParameter != null : "Callable should have a receiver parameter: " + descriptor;
            return StackValue.sharedTypeForType(mapType(receiverParameter.getType()));
        }
        else if (descriptor instanceof VariableDescriptor && isVarCapturedInClosure(bindingContext, descriptor)) {
            return StackValue.sharedTypeForType(mapType(((VariableDescriptor) descriptor).getType()));
        }
        return null;
    }
}
