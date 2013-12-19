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

package org.jetbrains.jet.codegen.state;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.BindingTraceAware;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.mapping.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaFullPackageScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPurePackageScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.AsmUtil.getTraitImplThisParameterType;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.FunctionTypesUtil.getFunctionTraitClassName;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;

public class JetTypeMapper extends BindingTraceAware {

    private final ClassBuilderMode classBuilderMode;

    public JetTypeMapper(BindingTrace bindingTrace, ClassBuilderMode mode) {
        super(bindingTrace);
        classBuilderMode = mode;
    }

    private enum JetTypeMapperMode {
        /**
         * foo.Bar is mapped to Lfoo/Bar;
         */
        IMPL,
        /**
         * jet.Int is mapped to I
         */
        VALUE,
        /**
         * jet.Int is mapped to Ljava/lang/Integer;
         */
        TYPE_PARAMETER,
        /**
         * jet.Int is mapped to Ljava/lang/Integer;
         * No projections allowed in immediate arguments
         */
        SUPER_TYPE
    }

    @NotNull
    public Type getOwner(@NotNull DeclarationDescriptor descriptor, @NotNull OwnerKind kind, boolean isInsideModule) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return asmTypeForPackage((PackageFragmentDescriptor) containingDeclaration, descriptor, isInsideModule);
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            return kind == OwnerKind.TRAIT_IMPL ? mapTraitImpl(classDescriptor) : mapClass(classDescriptor);
        }
        else if (containingDeclaration instanceof ScriptDescriptor) {
            return asmTypeForScriptDescriptor(bindingContext, (ScriptDescriptor) containingDeclaration);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
    }

    @NotNull
    private Type asmTypeForPackage(
            @NotNull PackageFragmentDescriptor packageFragment,
            @NotNull DeclarationDescriptor descriptor,
            boolean insideModule
    ) {
        return Type.getObjectType(internalNameForPackage(packageFragment, descriptor, insideModule));
    }

    @NotNull
    private String internalNameForPackage(
            @NotNull PackageFragmentDescriptor packageFragment,
            @NotNull DeclarationDescriptor descriptor,
            boolean insideModule
    ) {
        JetScope packageScope = packageFragment.getMemberScope();
        if (!(packageFragment instanceof JavaPackageFragmentDescriptor)
            || packageScope instanceof DeserializedPackageMemberScope
            || packageScope instanceof JavaFullPackageScope) {
            JetFile file = BindingContextUtils.getContainingFile(bindingContext, descriptor);
            if (insideModule && file != null) {
                return NamespaceCodegen.getNamespacePartInternalName(file);
            }
            else {
                return PackageClassUtils.getPackageClassFqName(packageFragment.getFqName()).asString().replace('.', '/');
            }
        }

        if (!(packageScope instanceof JavaClassStaticMembersScope)) {
            throw new IllegalStateException("Unexpected scope: " + packageScope.getClass());
        }

        JavaPackageFragmentProvider javaFragmentProvider = ((JavaPackageFragmentDescriptor) packageFragment).getProvider();

        StringBuilder r = new StringBuilder();
        for (FqName pathItem : packageFragment.getFqName().parent().path()) {
            if (pathItem.isRoot()) {
                continue;
            }
            r.append(pathItem.shortName().asString());

            JetScope memberScope = javaFragmentProvider.getOrCreatePackage(pathItem).getMemberScope();
            if (memberScope instanceof JavaClassStaticMembersScope) {
                r.append("$");
            }
            else if (memberScope instanceof JavaPurePackageScope || memberScope instanceof JavaFullPackageScope) {
                r.append("/");
            }
            else {
                throw new IllegalStateException("Unexpected scope: " + memberScope.getClass());
            }
        }

        r.append(packageFragment.getName().asString());
        return r.toString();
    }

    @NotNull
    public Type mapReturnType(@NotNull JetType jetType) {
        return mapReturnType(jetType, null);
    }

    @NotNull
    private Type mapReturnType(@NotNull JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        if (jetType.equals(KotlinBuiltIns.getInstance().getUnitType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(Type.VOID_TYPE);
            }
            return Type.VOID_TYPE;
        }
        return mapType(jetType, signatureVisitor, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE, false);
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
    public Type mapType(@NotNull ClassifierDescriptor descriptor) {
        return mapType(descriptor.getDefaultType());
    }

    @NotNull
    private Type mapType(@NotNull JetType jetType, @Nullable BothSignatureWriter signatureVisitor, @NotNull JetTypeMapperMode mode) {
        return mapType(jetType, signatureVisitor, mode, Variance.INVARIANT, false);
    }

    @NotNull
    public Type mapType(
            @NotNull JetType jetType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull JetTypeMapperMode kind,
            @NotNull Variance howThisTypeIsUsed,
            boolean arrayParameter
    ) {
        Type known = null;
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();

        if (descriptor instanceof ClassDescriptor) {
            FqNameUnsafe className = DescriptorUtils.getFqName(descriptor);
            if (className.isSafe()) {
                known = KotlinToJavaTypesMap.getInstance().getJavaAnalog(className.toSafe(), jetType.isNullable());
            }
        }

        boolean projectionsAllowed = kind != JetTypeMapperMode.SUPER_TYPE;
        if (known != null) {
            if (kind == JetTypeMapperMode.VALUE) {
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            }
            else if (kind == JetTypeMapperMode.TYPE_PARAMETER || kind == JetTypeMapperMode.SUPER_TYPE) {
                return mapKnownAsmType(jetType, boxType(known), signatureVisitor, howThisTypeIsUsed, arrayParameter, projectionsAllowed);
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
        if (constructor instanceof IntersectionTypeConstructor) {
            jetType = CommonSupertypes.commonSupertype(new ArrayList<JetType>(constructor.getSupertypes()));
        }

        if (descriptor == null) {
            throw new UnsupportedOperationException("no descriptor for type constructor of " + jetType);
        }

        if (ErrorUtils.isError(descriptor)) {
            if (classBuilderMode != ClassBuilderMode.LIGHT_CLASSES) {
                throw new IllegalStateException(generateErrorMessageForErrorType(descriptor));
            }
            Type asmType = Type.getObjectType("error/NonExistentClass");
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(asmType);
            }
            return asmType;
        }

        if (descriptor instanceof ClassDescriptor && KotlinBuiltIns.getInstance().isArray(jetType)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberProjection = jetType.getArguments().get(0);
            JetType memberType = memberProjection.getType();

            if (signatureVisitor != null) {
                signatureVisitor.writeArrayType();
                mapType(memberType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER, memberProjection.getProjectionKind(), true);
                signatureVisitor.writeArrayEnd();
            }

            Type r;
            if (!isGenericsArray(jetType)) {
                r = Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
            }
            else {
                r = AsmTypeConstants.JAVA_ARRAY_GENERIC_TYPE;
            }
            return r;
        }

        if (descriptor instanceof ClassDescriptor) {
            Type asmType = getAsmType(bindingTrace, (ClassDescriptor) descriptor);
            writeGenericType(signatureVisitor, asmType, jetType, howThisTypeIsUsed, projectionsAllowed);
            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            Type type = mapType(typeParameterDescriptor.getUpperBoundsAsType(), kind);
            if (signatureVisitor != null) {
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), type);
            }
            return type;
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    @NotNull
    public Type mapTraitImpl(@NotNull ClassDescriptor descriptor) {
        return Type.getObjectType(getAsmType(bindingTrace, descriptor).getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
    }

    private String generateErrorMessageForErrorType(@NotNull DeclarationDescriptor descriptor) {
        PsiElement declarationElement = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        PsiElement parentDeclarationElement = null;
        if (declarationElement != null) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration != null) {
                parentDeclarationElement = BindingContextUtils.descriptorToDeclaration(bindingContext, containingDeclaration);
            }
        }

        return String.format("Error types are not allowed when classBuilderMode = %s. Descriptor: %s. For declaration %s:%s in %s:%s",
                      classBuilderMode,
                      descriptor,
                      declarationElement,
                      declarationElement != null ? declarationElement.getText() : "null",
                      parentDeclarationElement,
                      parentDeclarationElement != null ? parentDeclarationElement.getText() : "null");
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
        return mapKnownAsmType(jetType, asmType, signatureVisitor, howThisTypeIsUsed, false, true);
    }

    private Type mapKnownAsmType(
            JetType jetType,
            Type asmType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull Variance howThisTypeIsUsed,
            boolean arrayParameter,
            boolean allowProjections
    ) {
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                if (arrayParameter && howThisTypeIsUsed == Variance.IN_VARIANCE) {
                    asmType = AsmTypeConstants.OBJECT_TYPE;
                }
                signatureVisitor.writeAsmType(asmType);
            }
            else {
                writeGenericType(signatureVisitor, asmType, jetType, howThisTypeIsUsed, allowProjections);
            }
        }
        return asmType;
    }

    @NotNull
    public CallableMethod mapToCallableMethod(
            @NotNull FunctionDescriptor functionDescriptor,
            boolean superCall,
            boolean isInsideClass,
            boolean isInsideModule,
            OwnerKind kind
    ) {
        DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();

        functionDescriptor = unwrapFakeOverride(functionDescriptor.getOriginal());

        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal(), true, kind);
        Type owner;
        Type ownerForDefaultImpl;
        Type ownerForDefaultParam;
        int invokeOpcode;
        Type thisClass;
        Type calleeType = null;

        if (isLocalNamedFun(functionDescriptor) || functionDescriptor instanceof ExpressionAsFunctionDescriptor) {
            if (functionDescriptor instanceof ExpressionAsFunctionDescriptor) {
                JetExpression expression = JetPsiUtil.deparenthesize(((ExpressionAsFunctionDescriptor) functionDescriptor).getExpression());
                if (expression instanceof JetFunctionLiteralExpression) {
                    expression = ((JetFunctionLiteralExpression) expression).getFunctionLiteral();
                }
                functionDescriptor = bindingContext.get(BindingContext.FUNCTION, expression);
            }
            functionDescriptor = functionDescriptor.getOriginal();

            owner = asmTypeForAnonymousClass(bindingContext, functionDescriptor);
            ownerForDefaultImpl = ownerForDefaultParam = thisClass = owner;
            invokeOpcode = INVOKEVIRTUAL;
            descriptor = mapSignature("invoke", functionDescriptor, true, kind);
            calleeType = owner;
        }
        else if (functionParent instanceof PackageFragmentDescriptor) {
            assert !superCall;
            owner = asmTypeForPackage((PackageFragmentDescriptor) functionParent, functionDescriptor, isInsideModule);
            ownerForDefaultImpl = ownerForDefaultParam = owner;
            invokeOpcode = INVOKESTATIC;
            thisClass = null;
        }
        else if (functionDescriptor instanceof ConstructorDescriptor) {
            assert !superCall;
            owner = mapClass((ClassDescriptor) functionParent);
            ownerForDefaultImpl = ownerForDefaultParam = owner;
            invokeOpcode = INVOKESPECIAL;
            thisClass = null;
        }
        else if (functionParent instanceof ScriptDescriptor) {
            thisClass = owner = ownerForDefaultParam = ownerForDefaultImpl =
                    asmTypeForScriptDescriptor(bindingContext, (ScriptDescriptor) functionParent);
            invokeOpcode = INVOKEVIRTUAL;
        }
        else if (functionParent instanceof ClassDescriptor) {

            FunctionDescriptor declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor);

            ClassDescriptor currentOwner = (ClassDescriptor) functionParent;
            ClassDescriptor declarationOwner = (ClassDescriptor) declarationFunctionDescriptor.getContainingDeclaration();

            boolean originalIsInterface = isInterface(declarationOwner);
            boolean currentIsInterface = isInterface(currentOwner);

            boolean isAccessor = isAccessor(functionDescriptor);

            ClassDescriptor receiver;
            if (currentIsInterface && !originalIsInterface) {
                receiver = declarationOwner;
            }
            else {
                receiver = currentOwner;
            }

            // TODO: TYPE_PARAMETER is hack here

            boolean isInterface = originalIsInterface && currentIsInterface;
            owner = mapType(receiver.getDefaultType(), JetTypeMapperMode.TYPE_PARAMETER);

            ClassDescriptor declarationOwnerForDefault = (ClassDescriptor) findBaseDeclaration(functionDescriptor).getContainingDeclaration();
            ownerForDefaultParam = mapType(declarationOwnerForDefault.getDefaultType(), JetTypeMapperMode.TYPE_PARAMETER);
            ownerForDefaultImpl = Type.getObjectType(
                    ownerForDefaultParam.getInternalName() + (isInterface(declarationOwnerForDefault) ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
            if (isInterface) {
                invokeOpcode = superCall ? INVOKESTATIC : INVOKEINTERFACE;
            }
            else {
                if (isAccessor) {
                    invokeOpcode = INVOKESTATIC;
                }
                else {
                    boolean isPrivateFunInvocation = isInsideClass && functionDescriptor.getVisibility() == Visibilities.PRIVATE;
                    invokeOpcode = superCall || isPrivateFunInvocation ? INVOKESPECIAL : INVOKEVIRTUAL;
                }
            }

            if (isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor, false, OwnerKind.TRAIT_IMPL);
                owner = Type.getObjectType(owner.getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
            }
            thisClass = mapType(receiver.getDefaultType());
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }


        Type receiverParameterType;
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getOriginal().getReceiverParameter();
        if (receiverParameter != null) {
            receiverParameterType = mapType(receiverParameter.getType());
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(
                owner, ownerForDefaultImpl, ownerForDefaultParam, descriptor, invokeOpcode,
                thisClass, receiverParameterType, calleeType);
    }

    public static boolean isAccessor(@NotNull CallableMemberDescriptor descriptor) {
        return descriptor instanceof AccessorForFunctionDescriptor ||
               descriptor instanceof AccessorForPropertyDescriptor ||
               descriptor instanceof AccessorForPropertyDescriptor.Getter ||
               descriptor instanceof AccessorForPropertyDescriptor.Setter;
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
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor f, boolean needGenericSignature, @NotNull OwnerKind kind) {
        String name = f.getName().asString();
        if (f instanceof PropertyAccessorDescriptor) {
            boolean isGetter = f instanceof PropertyGetterDescriptor;
            name = getPropertyAccessorName(((PropertyAccessorDescriptor) f).getCorrespondingProperty(), isGetter);
        }
        return mapSignature(name, f, needGenericSignature, kind);
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull Name functionName, @NotNull FunctionDescriptor f) {
        return mapSignature(functionName.asString(), f, false, OwnerKind.IMPLEMENTATION);
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor f) {
        return mapSignature(f.getName(), f);
    }

    @NotNull
    private JvmMethodSignature mapSignature(
            @NotNull String methodName,
            @NotNull FunctionDescriptor f,
            boolean needGenericSignature,
            @NotNull OwnerKind kind
    ) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            needGenericSignature = false;
        }

        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, needGenericSignature);

        writeFormalTypeParameters(f.getTypeParameters(), signatureVisitor);

        signatureVisitor.writeParametersStart();
        writeThisIfNeeded(f, kind, signatureVisitor);
        writeReceiverIfNeeded(f.getReceiverParameter(), signatureVisitor);

        for (ValueParameterDescriptor parameter : f.getValueParameters()) {
            writeParameter(signatureVisitor, parameter.getType());
        }

        if (f instanceof ConstructorDescriptor) {
            writeVoidReturn(signatureVisitor);
        }
        else {
            signatureVisitor.writeReturnType();
            JetType returnType = f.getReturnType();
            assert returnType != null : "Function " + f + " has no return type";
            mapReturnType(returnType, signatureVisitor);
            signatureVisitor.writeReturnTypeEnd();
        }

        return signatureVisitor.makeJvmMethodSignature(methodName);
    }

    private static void writeVoidReturn(@NotNull BothSignatureWriter signatureVisitor) {
        signatureVisitor.writeReturnType();
        signatureVisitor.writeAsmType(Type.VOID_TYPE);
        signatureVisitor.writeReturnTypeEnd();
    }

    @Nullable
    public String mapFieldSignature(@NotNull JetType backingFieldType) {
        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.TYPE, true);
        mapType(backingFieldType, signatureVisitor, JetTypeMapperMode.VALUE);
        return signatureVisitor.makeJavaGenericSignature();
    }

    private void writeThisIfNeeded(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull OwnerKind kind,
            @NotNull BothSignatureWriter signatureVisitor
    ) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            Type type = getTraitImplThisParameterType(containingDeclaration, this);

            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            signatureVisitor.writeAsmType(type);
            signatureVisitor.writeParameterTypeEnd();
        }
        else {
            writeThisForAccessorIfNeeded(descriptor, signatureVisitor);
        }
    }

    private void writeThisForAccessorIfNeeded(@NotNull CallableMemberDescriptor descriptor, @NotNull BothSignatureWriter signatureVisitor) {
        if (isAccessor(descriptor) && descriptor.getExpectedThisObject() != null) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), signatureVisitor, JetTypeMapperMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }
    }


    public void writeFormalTypeParameters(List<TypeParameterDescriptor> typeParameters, BothSignatureWriter signatureVisitor) {
        if (signatureVisitor == null) return;

        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            writeFormalTypeParameter(typeParameterDescriptor, signatureVisitor);
        }
    }

    private void writeFormalTypeParameter(TypeParameterDescriptor typeParameterDescriptor, BothSignatureWriter signatureVisitor) {
        signatureVisitor.writeFormalTypeParameter(typeParameterDescriptor.getName().asString());

        classBound:
        {
            signatureVisitor.writeClassBound();

            for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
                if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                    if (!isInterface(jetType)) {
                        mapType(jetType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                        break classBound;
                    }
                }
            }

            // "extends Object" is optional according to ClassFileFormat-Java5.pdf
            // but javac complaints to signature:
            // <P:>Ljava/lang/Object;
            // TODO: avoid writing java/lang/Object if interface list is not empty
        }
        signatureVisitor.writeClassBoundEnd();

        for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                if (isInterface(jetType)) {
                    signatureVisitor.writeInterfaceBound();
                    mapType(jetType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                    signatureVisitor.writeInterfaceBoundEnd();
                }
            }
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                signatureVisitor.writeInterfaceBound();
                mapType(jetType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                signatureVisitor.writeInterfaceBoundEnd();
            }
        }
    }

    private void writeReceiverIfNeeded(@Nullable ReceiverParameterDescriptor receiver, BothSignatureWriter signatureWriter) {
        if (receiver != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiver.getType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
    }

    @NotNull
    public static String getPropertyAccessorName(@NotNull PropertyDescriptor descriptor, boolean isGetter) {
        DeclarationDescriptor parentDescriptor = descriptor.getContainingDeclaration();
        boolean isAnnotation = parentDescriptor instanceof ClassDescriptor &&
                               ((ClassDescriptor) parentDescriptor).getKind() == ClassKind.ANNOTATION_CLASS;
        return isAnnotation ? descriptor.getName().asString() :
               isGetter ? PropertyCodegen.getterName(descriptor.getName()) : PropertyCodegen.setterName(descriptor.getName());
    }

    @NotNull
    public JvmMethodSignature mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        // TODO: do not genClassOrObject generics if not needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();
        writeThisIfNeeded(descriptor, kind, signatureWriter);
        writeReceiverIfNeeded(descriptor.getReceiverParameter(), signatureWriter);

        signatureWriter.writeReturnType();
        mapType(descriptor.getType(), signatureWriter, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE, false);
        signatureWriter.writeReturnTypeEnd();

        String name = getPropertyAccessorName(descriptor, true);
        return signatureWriter.makeJvmMethodSignature(name);
    }


    @NotNull
    public JvmMethodSignature mapSetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        assert descriptor.isVar();

        // TODO: generics signature is not always needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();
        writeThisIfNeeded(descriptor, kind, signatureWriter);
        writeReceiverIfNeeded(descriptor.getReceiverParameter(), signatureWriter);
        writeParameter(signatureWriter, descriptor.getType());

        writeVoidReturn(signatureWriter);

        String name = getPropertyAccessorName(descriptor, false);
        return signatureWriter.makeJvmMethodSignature(name);
    }

    private void writeParameter(@NotNull BothSignatureWriter signatureWriter, @NotNull JetType outType) {
        signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
        mapType(outType, signatureWriter, JetTypeMapperMode.VALUE);
        signatureWriter.writeParameterTypeEnd();
    }

    @NotNull
    public JvmMethodSignature mapConstructorSignature(@NotNull ConstructorDescriptor descriptor) {
        return mapConstructorSignature(descriptor, bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration()));
    }

    @NotNull
    public JvmMethodSignature mapConstructorSignature(@NotNull ConstructorDescriptor descriptor, @Nullable CalculatedClosure closure) {

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        // constructor type parmeters are fake
        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        ClassDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        ClassDescriptor captureThis = getExpectedThisObjectForConstructorCall(descriptor, closure);
        if (captureThis != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.OUTER);
            mapType(captureThis.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        JetType captureReceiverType = closure != null ? closure.getCaptureReceiverType() : null;
        if (captureReceiverType != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(captureReceiverType, signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        if (containingDeclaration.getKind() == ClassKind.ENUM_CLASS || containingDeclaration.getKind() == ClassKind.ENUM_ENTRY) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_NAME);
            mapType(KotlinBuiltIns.getInstance().getStringType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_ORDINAL);
            mapType(KotlinBuiltIns.getInstance().getIntType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        if (closure != null) {
            for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closure.getCaptureVariables().entrySet()) {
                DeclarationDescriptor variableDescriptor = entry.getKey();
                Type type = null;
                if (variableDescriptor instanceof VariableDescriptor && !(variableDescriptor instanceof PropertyDescriptor)) {
                    Type sharedVarType = getSharedVarType(variableDescriptor);
                    if (sharedVarType == null) {
                        sharedVarType = mapType(((VariableDescriptor) variableDescriptor).getType());
                    }
                    type = sharedVarType;
                }
                else if (isLocalNamedFun(variableDescriptor)) {
                    type = asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) variableDescriptor);
                }

                if (type != null) {
                    signatureWriter.writeParameterType(JvmMethodParameterKind.SHARED_VAR);
                    signatureWriter.writeAsmType(type);
                    signatureWriter.writeParameterTypeEnd();
                }
            }

            JetDelegatorToSuperCall superCall = closure.getSuperCall();
            if (superCall != null) {
                DeclarationDescriptor superDescriptor = bindingContext
                        .get(BindingContext.REFERENCE_TARGET, superCall.getCalleeExpression().getConstructorReferenceExpression());

                if (superDescriptor instanceof ConstructorDescriptor) {
                    ConstructorDescriptor superConstructor = (ConstructorDescriptor) superDescriptor;

                    if (isObjectLiteral(bindingContext, descriptor.getContainingDeclaration())) {
                        List<JvmMethodParameterSignature> types = mapConstructorSignature(superConstructor).getKotlinParameterTypes();
                        for (JvmMethodParameterSignature type : types) {
                            signatureWriter.writeParameterType(JvmMethodParameterKind.SUPER_CALL_PARAM);
                            signatureWriter.writeAsmType(type.getAsmType());
                            signatureWriter.writeParameterTypeEnd();
                        }
                    }
                }
            }
        }

        for (ValueParameterDescriptor parameter : descriptor.getOriginal().getValueParameters()) {
            writeParameter(signatureWriter, parameter.getType());
        }

        writeVoidReturn(signatureWriter);

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        for (ScriptDescriptor importedScript : importedScripts) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            ClassDescriptor descriptor = bindingContext.get(CLASS_FOR_SCRIPT, importedScript);
            assert descriptor != null;
            mapType(descriptor.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor valueParameter : script.getValueParameters()) {
            writeParameter(signatureWriter, valueParameter.getType());
        }

        writeVoidReturn(signatureWriter);

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    @NotNull
    public CallableMethod mapToCallableMethod(@NotNull ConstructorDescriptor descriptor) {
        return mapToCallableMethod(descriptor, bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration()));
    }

    @NotNull
    public CallableMethod mapToCallableMethod(@NotNull ConstructorDescriptor descriptor, @Nullable CalculatedClosure closure) {
        JvmMethodSignature method = mapConstructorSignature(descriptor, closure);
        ClassDescriptor container = descriptor.getContainingDeclaration();
        Type owner = mapClass(container);
        if (owner.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + container.getDefaultType() + ", actual: " + owner);
        }
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
    }


    private static boolean isGenericsArray(JetType type) {
        return KotlinBuiltIns.getInstance().isArray(type) &&
               type.getArguments().get(0).getType().getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return StackValue.sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            return asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue.sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof VariableDescriptor && isVarCapturedInClosure(bindingContext, descriptor)) {
            JetType outType = ((VariableDescriptor) descriptor).getType();
            return StackValue.sharedTypeForType(mapType(outType));
        }
        return null;
    }

    @NotNull
    public CallableMethod mapToFunctionInvokeCallableMethod(@NotNull FunctionDescriptor fd) {
        JvmMethodSignature descriptor = erasedInvokeSignature(fd);
        Type owner = getFunctionTraitClassName(fd);
        Type receiverParameterType;
        ReceiverParameterDescriptor receiverParameter = fd.getOriginal().getReceiverParameter();
        if (receiverParameter != null) {
            receiverParameterType = mapType(receiverParameter.getType());
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(owner, null, null, descriptor, INVOKEINTERFACE, owner, receiverParameterType, owner);
    }

    @NotNull
    public Type expressionType(JetExpression expr) {
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expr);
        return asmTypeOrVoid(type);
    }

    @NotNull
    private Type asmTypeOrVoid(@Nullable JetType type) {
        return type == null ? Type.VOID_TYPE : mapType(type);
    }
}
