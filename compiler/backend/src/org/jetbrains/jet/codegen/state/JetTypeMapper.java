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
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassStaticsPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.mapping.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.AsmUtil.getTraitImplThisParameterType;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.FunctionTypesUtil.getFunctionTraitClassName;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

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
        if (packageFragment instanceof JavaClassStaticsPackageFragmentDescriptor) {
            JavaClassStaticsPackageFragmentDescriptor javaPackageFragment = (JavaClassStaticsPackageFragmentDescriptor) packageFragment;
            return mapClass(javaPackageFragment.getCorrespondingClass()).getInternalName();
        }

        // It's not a package created for Java class statics
        JetFile file = BindingContextUtils.getContainingFile(bindingContext, descriptor);
        if (insideModule && file != null) {
            return PackageCodegen.getPackagePartInternalName(file);
        }
        else {
            return PackageClassUtils.getPackageClassFqName(packageFragment.getFqName()).asString().replace('.', '/');
        }
    }

    @NotNull
    public Type mapReturnType(@NotNull CallableDescriptor descriptor) {
        return mapReturnType(descriptor, null);
    }

    @NotNull
    private Type mapReturnType(@NotNull CallableDescriptor descriptor, @Nullable BothSignatureWriter sw) {
        JetType returnType = descriptor.getReturnType();
        assert returnType != null : "Function has no return type: " + descriptor;
        if (returnType.equals(KotlinBuiltIns.getInstance().getUnitType()) && !(descriptor instanceof PropertyGetterDescriptor)) {
            if (sw != null) {
                sw.writeAsmType(Type.VOID_TYPE);
            }
            return Type.VOID_TYPE;
        }
        else {
            return mapType(returnType, sw, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE, false);
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
    private Type mapType(
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
                throw new IllegalStateException(generateErrorMessageForErrorType(jetType, descriptor));
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

            if (memberType.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                return AsmTypeConstants.getType(Object[].class);
            }

            return Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
        }

        if (descriptor instanceof ClassDescriptor) {
            Type asmType = getAsmType(bindingTrace, (ClassDescriptor) descriptor);
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

    @NotNull
    public Type mapTraitImpl(@NotNull ClassDescriptor descriptor) {
        return Type.getObjectType(getAsmType(bindingTrace, descriptor).getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
    }

    @NotNull
    private String generateErrorMessageForErrorType(@NotNull JetType type, @NotNull DeclarationDescriptor descriptor) {
        PsiElement declarationElement = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        PsiElement parentDeclarationElement = null;
        if (declarationElement != null) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration != null) {
                parentDeclarationElement = BindingContextUtils.descriptorToDeclaration(bindingContext, containingDeclaration);
            }
        }

        return String.format("Error types are not allowed when classBuilderMode = %s. " +
                             "Type: %s (%s). Descriptor: %s. For declaration %s:%s in %s:%s",
                             classBuilderMode,
                             type,
                             type.getClass().getSimpleName(),
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
            @NotNull CodegenContext<?> context
    ) {
        DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();

        functionDescriptor = unwrapFakeOverride(functionDescriptor.getOriginal());

        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal());
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
            descriptor = mapSignature(functionDescriptor);
            calleeType = owner;
        }
        else if (functionParent instanceof PackageFragmentDescriptor) {
            assert !superCall;
            owner = asmTypeForPackage((PackageFragmentDescriptor) functionParent, functionDescriptor,
                                      isCallInsideSameModuleAsDeclared(functionDescriptor, context));
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

            ClassDescriptor receiver;
            if (currentIsInterface && !originalIsInterface) {
                receiver = declarationOwner;
            }
            else {
                receiver = currentOwner;
            }

            boolean isInterface = originalIsInterface && currentIsInterface;
            owner = mapClass(receiver);

            ClassDescriptor declarationOwnerForDefault = (ClassDescriptor) findBaseDeclaration(functionDescriptor).getContainingDeclaration();
            ownerForDefaultParam = mapClass(declarationOwnerForDefault);
            ownerForDefaultImpl = Type.getObjectType(
                    ownerForDefaultParam.getInternalName() + (isInterface(declarationOwnerForDefault) ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
            if (isInterface) {
                invokeOpcode = superCall ? INVOKESTATIC : INVOKEINTERFACE;
            }
            else {
                if (isAccessor(functionDescriptor)) {
                    invokeOpcode = INVOKESTATIC;
                }
                else {
                    boolean isPrivateFunInvocation = isCallInsideSameClassAsDeclared(functionDescriptor, context) &&
                                                     functionDescriptor.getVisibility() == Visibilities.PRIVATE;
                    invokeOpcode = superCall || isPrivateFunInvocation ? INVOKESPECIAL : INVOKEVIRTUAL;
                }
            }

            if (isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor, OwnerKind.TRAIT_IMPL);
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
    private static String mapFunctionName(@NotNull FunctionDescriptor descriptor) {
        if (descriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
            if (isAnnotationClass(property.getContainingDeclaration())) {
                return property.getName().asString();
            }

            if (descriptor instanceof PropertyGetterDescriptor) {
                return PropertyCodegen.getterName(property.getName());
            }
            else {
                return PropertyCodegen.setterName(property.getName());
            }
        }
        else if (isLocalNamedFun(descriptor) ||
                 descriptor instanceof AnonymousFunctionDescriptor ||
                 descriptor instanceof ExpressionAsFunctionDescriptor) {
            return "invoke";
        }
        else {
            return descriptor.getName().asString();
        }
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor descriptor) {
        return mapSignature(descriptor, OwnerKind.IMPLEMENTATION);
    }

    @NotNull
    public JvmMethodSignature mapSignature(@NotNull FunctionDescriptor f, @NotNull OwnerKind kind) {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        if (f instanceof ConstructorDescriptor) {
            sw.writeParametersStart();
            writeAdditionalConstructorParameters((ConstructorDescriptor) f, sw);

            for (ValueParameterDescriptor parameter : f.getOriginal().getValueParameters()) {
                writeParameter(sw, parameter.getType());
            }

            writeVoidReturn(sw);
        }
        else {
            if (f instanceof PropertyAccessorDescriptor) {
                writeFormalTypeParameters(((PropertyAccessorDescriptor) f).getCorrespondingProperty().getTypeParameters(), sw);
            }
            else {
                writeFormalTypeParameters(f.getTypeParameters(), sw);
            }

            sw.writeParametersStart();
            writeThisIfNeeded(f, kind, sw);
            writeReceiverIfNeeded(f.getReceiverParameter(), sw);

            for (ValueParameterDescriptor parameter : f.getValueParameters()) {
                writeParameter(sw, parameter.getType());
            }

            sw.writeReturnType();
            if (forceBoxedReturnType(f)) {
                // TYPE_PARAMETER is a hack to automatically box the return type
                //noinspection ConstantConditions
                mapType(f.getReturnType(), sw, JetTypeMapperMode.TYPE_PARAMETER);
            }
            else {
                mapReturnType(f, sw);
            }
            sw.writeReturnTypeEnd();
        }

        return sw.makeJvmMethodSignature(mapFunctionName(f));
    }

    /**
     * @return true iff a given function descriptor should be compiled to a method with boxed return type regardless of whether return type
     * of that descriptor is nullable or not. This happens when a function returning a value of a primitive type overrides another function
     * with a non-primitive return type. In that case the generated method's return type should be boxed: otherwise it's not possible to use
     * this class from Java since javac issues errors when loading the class (incompatible return types)
     */
    private static boolean forceBoxedReturnType(@NotNull FunctionDescriptor descriptor) {
        //noinspection ConstantConditions
        if (!KotlinBuiltIns.getInstance().isPrimitiveType(descriptor.getReturnType())) return false;

        for (FunctionDescriptor overridden : descriptor.getOverriddenDescriptors()) {
            //noinspection ConstantConditions
            if (!KotlinBuiltIns.getInstance().isPrimitiveType(overridden.getOriginal().getReturnType())) return true;
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
        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.TYPE);
        mapType(backingFieldType, signatureVisitor, JetTypeMapperMode.VALUE);
        return signatureVisitor.makeJavaGenericSignature();
    }

    private void writeThisIfNeeded(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull OwnerKind kind,
            @NotNull BothSignatureWriter sw
    ) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            Type type = getTraitImplThisParameterType(containingDeclaration, this);

            sw.writeParameterType(JvmMethodParameterKind.THIS);
            sw.writeAsmType(type);
            sw.writeParameterTypeEnd();
        }
        else if (isAccessor(descriptor) && descriptor.getExpectedThisObject() != null) {
            sw.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), sw, JetTypeMapperMode.VALUE);
            sw.writeParameterTypeEnd();
        }
    }


    public void writeFormalTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull BothSignatureWriter sw) {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            writeFormalTypeParameter(typeParameter, sw);
        }
    }

    private void writeFormalTypeParameter(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull BothSignatureWriter sw) {
        sw.writeFormalTypeParameter(typeParameterDescriptor.getName().asString());

        classBound:
        {
            sw.writeClassBound();

            for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
                if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                    if (!isInterface(jetType)) {
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
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                if (isInterface(jetType)) {
                    sw.writeInterfaceBound();
                    mapType(jetType, sw, JetTypeMapperMode.TYPE_PARAMETER);
                    sw.writeInterfaceBoundEnd();
                }
            }
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                sw.writeInterfaceBound();
                mapType(jetType, sw, JetTypeMapperMode.TYPE_PARAMETER);
                sw.writeInterfaceBoundEnd();
            }
        }
    }

    private void writeReceiverIfNeeded(@Nullable ReceiverParameterDescriptor receiver, @NotNull BothSignatureWriter sw) {
        if (receiver != null) {
            sw.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiver.getType(), sw, JetTypeMapperMode.VALUE);
            sw.writeParameterTypeEnd();
        }
    }

    private void writeParameter(@NotNull BothSignatureWriter sw, @NotNull JetType type) {
        sw.writeParameterType(JvmMethodParameterKind.VALUE);
        mapType(type, sw, JetTypeMapperMode.VALUE);
        sw.writeParameterTypeEnd();
    }

    private void writeAdditionalConstructorParameters(
            @NotNull ConstructorDescriptor descriptor,
            @NotNull BothSignatureWriter signatureWriter
    ) {
        CalculatedClosure closure = bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration());

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

        ClassDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration.getKind() == ClassKind.ENUM_CLASS || containingDeclaration.getKind() == ClassKind.ENUM_ENTRY) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_NAME);
            mapType(KotlinBuiltIns.getInstance().getStringType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_ORDINAL);
            mapType(KotlinBuiltIns.getInstance().getIntType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
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
            else if (isLocalNamedFun(variableDescriptor)) {
                type = asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) variableDescriptor);
            }
            else {
                type = null;
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

            if (superDescriptor instanceof ConstructorDescriptor && isAnonymousObject(descriptor.getContainingDeclaration())) {
                for (JvmMethodParameterSignature parameter : mapSignature((ConstructorDescriptor) superDescriptor).getValueParameters()) {
                    signatureWriter.writeParameterType(JvmMethodParameterKind.SUPER_CALL_PARAM);
                    signatureWriter.writeAsmType(parameter.getAsmType());
                    signatureWriter.writeParameterTypeEnd();
                }
            }
        }
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

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
        JvmMethodSignature method = mapSignature(descriptor);
        ClassDescriptor container = descriptor.getContainingDeclaration();
        Type owner = mapClass(container);
        if (owner.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + container.getDefaultType() + ", actual: " + owner);
        }
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
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
        JvmMethodSignature signature = erasedInvokeSignature(fd);
        Type owner = getFunctionTraitClassName(fd);
        Type receiverParameterType;
        ReceiverParameterDescriptor receiverParameter = fd.getOriginal().getReceiverParameter();
        if (receiverParameter != null) {
            receiverParameterType = mapType(receiverParameter.getType());
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(owner, null, null, signature, INVOKEINTERFACE, owner, receiverParameterType, owner);
    }

    @NotNull
    private static JvmMethodSignature erasedInvokeSignature(@NotNull FunctionDescriptor descriptor) {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);

        int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter() != null) {
            paramCount++;
        }

        sw.writeParametersStart();

        for (int i = 0; i < paramCount; ++i) {
            sw.writeParameterType(JvmMethodParameterKind.VALUE);
            sw.writeAsmType(OBJECT_TYPE);
            sw.writeParameterTypeEnd();
        }

        sw.writeReturnType();
        sw.writeAsmType(OBJECT_TYPE);
        sw.writeReturnTypeEnd();

        return sw.makeJvmMethodSignature("invoke");
    }
}
