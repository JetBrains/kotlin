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
import org.jetbrains.jet.codegen.signature.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;

public class JetTypeMapper extends BindingTraceAware {

    private final boolean mapBuiltinsToJava;
    private final ClassBuilderMode classBuilderMode;

    public JetTypeMapper(BindingTrace bindingTrace, boolean mapBuiltinsToJava, ClassBuilderMode mode) {
        super(bindingTrace);
        this.mapBuiltinsToJava = mapBuiltinsToJava;
        classBuilderMode = mode;
    }

    @NotNull
    public JvmClassName getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        JetTypeMapperMode mapTypeMode = ownerKindToMapTypeMode(kind);

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            return jvmClassNameForNamespace((NamespaceDescriptor) containingDeclaration);
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            if (classDescriptor.getKind() == ClassKind.OBJECT) {
                mapTypeMode = JetTypeMapperMode.IMPL;
            }
            Type asmType = mapType(classDescriptor.getDefaultType(), mapTypeMode);
            if (asmType.getSort() != Type.OBJECT) {
                throw new IllegalStateException();
            }
            return JvmClassName.byType(asmType);
        }
        else if (containingDeclaration instanceof ScriptDescriptor) {
            return classNameForScriptDescriptor(bindingContext, (ScriptDescriptor) containingDeclaration);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
    }

    private static JetTypeMapperMode ownerKindToMapTypeMode(OwnerKind kind) {
        if (kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.NAMESPACE || kind instanceof OwnerKind.StaticDelegateKind) {
            return JetTypeMapperMode.IMPL;
        }
        else if (kind == OwnerKind.TRAIT_IMPL) {
            return JetTypeMapperMode.TRAIT_IMPL;
        }
        else {
            throw new IllegalStateException("must not call this method with kind = " + kind);
        }
    }

    @NotNull
    private JavaNamespaceKind getNsKind(@NotNull NamespaceDescriptor ns) {
        JavaNamespaceKind javaNamespaceKind = bindingContext.get(JavaBindingContext.JAVA_NAMESPACE_KIND, ns);
        Boolean src = bindingContext.get(BindingContext.NAMESPACE_IS_SRC, ns);

        if (javaNamespaceKind == null && src == null) {
            throw new IllegalStateException("unknown namespace origin: " + ns);
        }

        if (javaNamespaceKind != null) {
            if (javaNamespaceKind == JavaNamespaceKind.CLASS_STATICS && src != null) {
                throw new IllegalStateException(
                        "conflicting namespace " + ns + ": it is both java statics and from src");
            }
            return javaNamespaceKind;
        }
        else {
            return JavaNamespaceKind.PROPER;
        }
    }

    @NotNull
    private JvmClassName jvmClassNameForNamespace(@NotNull NamespaceDescriptor namespace) {

        StringBuilder r = new StringBuilder();

        List<DeclarationDescriptor> path = DescriptorUtils.getPathWithoutRootNsAndModule(namespace);

        for (DeclarationDescriptor pathElement : path) {
            NamespaceDescriptor ns = (NamespaceDescriptor) pathElement;
            if (r.length() > 0) {
                JavaNamespaceKind nsKind = getNsKind((NamespaceDescriptor) ns.getContainingDeclaration());
                if (nsKind == JavaNamespaceKind.PROPER) {
                    r.append("/");
                }
                else if (nsKind == JavaNamespaceKind.CLASS_STATICS) {
                    r.append("$");
                }
            }
            r.append(ns.getName());
        }

        if (getNsKind(namespace) == JavaNamespaceKind.PROPER) {
            if (r.length() > 0) {
                r.append("/");
            }
            r.append(PackageClassUtils.getPackageClassName(namespace.getFqName()));
        }

        if (r.length() == 0) {
            throw new IllegalStateException("internal error: failed to generate classname for " + namespace);
        }

        return JvmClassName.byInternalName(r.toString());
    }

    @NotNull
    public Type mapReturnType(@NotNull final JetType jetType) {
        return mapReturnType(jetType, null);
    }

    @NotNull
    private Type mapReturnType(@NotNull final JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        if (jetType.equals(KotlinBuiltIns.getInstance().getUnitType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(Type.VOID_TYPE, false);
            }
            return Type.VOID_TYPE;
        }
        else if (jetType.equals(KotlinBuiltIns.getInstance().getNothingType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeNothing(false);
            }
            return Type.VOID_TYPE;
        }
        if (jetType.equals(KotlinBuiltIns.getInstance().getNullableNothingType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeNothing(true);
            }
            return AsmTypeConstants.OBJECT_TYPE;
        }
        return mapType(jetType, signatureVisitor, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE);
    }

    @NotNull
    public Type mapType(@NotNull final JetType jetType, @NotNull JetTypeMapperMode kind) {
        return mapType(jetType, null, kind);
    }

    @NotNull
    public Type mapType(@NotNull final JetType jetType) {
        return mapType(jetType, null, JetTypeMapperMode.VALUE);
    }

    @NotNull
    public Type mapType(@NotNull final VariableDescriptor variableDescriptor) {
        return mapType(variableDescriptor.getType(), null, JetTypeMapperMode.VALUE);
    }

    @NotNull
    public Type mapType(@NotNull final ClassifierDescriptor classifierDescriptor) {
        return mapType(classifierDescriptor.getDefaultType());
    }

    @NotNull
    public Type mapType(JetType jetType, @Nullable BothSignatureWriter signatureVisitor, @NotNull JetTypeMapperMode kind) {
        return mapType(jetType, signatureVisitor, kind, Variance.INVARIANT);
    }

    @NotNull
    public Type mapType(
            JetType jetType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull JetTypeMapperMode kind,
            @NotNull Variance howThisTypeIsUsed
    ) {
        Type known = null;
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();

        if (mapBuiltinsToJava) {
            if (descriptor instanceof ClassDescriptor) {
                known = KotlinToJavaTypesMap.getInstance().getJavaAnalog(jetType);
            }
        }

        if (known != null) {
            if (kind == JetTypeMapperMode.VALUE) {
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            }
            else if (kind == JetTypeMapperMode.TYPE_PARAMETER) {
                return mapKnownAsmType(jetType, boxType(known), signatureVisitor, howThisTypeIsUsed);
            }
            else if (kind == JetTypeMapperMode.TRAIT_IMPL) {
                throw new IllegalStateException("TRAIT_IMPL is not possible for " + jetType);
            }
            else if (kind == JetTypeMapperMode.IMPL) {
                //noinspection ConstantConditions
                if (mapBuiltinsToJava) {
                    // TODO: enable and fix tests
                    //throw new IllegalStateException("must not map known type to IMPL when not compiling builtins: " + jetType);
                }
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            }
            else {
                throw new IllegalStateException("unknown kind: " + kind);
            }
        }

        final TypeConstructor constructor = jetType.getConstructor();
        if (constructor instanceof IntersectionTypeConstructor) {
            jetType = CommonSupertypes.commonSupertype(new ArrayList<JetType>(constructor.getSupertypes()));
        }

        if (descriptor == null) {
            throw new UnsupportedOperationException("no descriptor for type constructor of " + jetType);
        }

        if (ErrorUtils.isError(descriptor)) {
            if (classBuilderMode != ClassBuilderMode.SIGNATURES) {
                throw new IllegalStateException(generateErrorMessageForErrorType(descriptor));
            }
            Type asmType = Type.getObjectType("error/NonExistentClass");
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(asmType, true);
            }
            checkValidType(asmType);
            return asmType;
        }

        if (mapBuiltinsToJava && descriptor instanceof ClassDescriptor && KotlinBuiltIns.getInstance().isArray(jetType)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberProjection = jetType.getArguments().get(0);
            JetType memberType = memberProjection.getType();

            if (signatureVisitor != null) {
                signatureVisitor.writeArrayType(jetType.isNullable(), memberProjection.getProjectionKind());
                mapType(memberType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                signatureVisitor.writeArrayEnd();
            }

            Type r;
            if (!isGenericsArray(jetType)) {
                r = Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
            }
            else {
                r = AsmTypeConstants.JAVA_ARRAY_GENERIC_TYPE;
            }
            checkValidType(r);
            return r;
        }

        if (descriptor instanceof ClassDescriptor) {
            JvmClassName name = getJvmInternalName(bindingTrace, descriptor);
            Type asmType;
            if (kind == JetTypeMapperMode.TRAIT_IMPL) {
                asmType = Type.getObjectType(name.getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
            }
            else {
                asmType = name.getAsmType();
            }
            boolean forceReal = KotlinToJavaTypesMap.getInstance().isForceReal(name);

            writeGenericType(signatureVisitor, asmType, jetType, forceReal, howThisTypeIsUsed);

            checkValidType(asmType);
            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            Type type = mapType(typeParameterDescriptor.getUpperBoundsAsType(), kind);
            if (signatureVisitor != null) {
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
            }
            checkValidType(type);
            return type;
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
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

        return String.format("Error types are not allowed when classBuilderMode = %s. For declaration %s:%s in %s:%s",
                      classBuilderMode,
                      declarationElement,
                      declarationElement != null ? declarationElement.getText() : "null",
                      parentDeclarationElement,
                      parentDeclarationElement != null ? parentDeclarationElement.getText() : "null");
    }

    private void writeGenericType(
            BothSignatureWriter signatureVisitor,
            Type asmType,
            JetType jetType,
            boolean forceReal,
            Variance howThisTypeIsUsed
    ) {
        if (signatureVisitor != null) {
            String kotlinTypeName = getKotlinTypeNameForSignature(jetType, asmType);
            signatureVisitor.writeClassBegin(asmType.getInternalName(), jetType.isNullable(), forceReal, kotlinTypeName);

            List<TypeProjection> arguments = jetType.getArguments();
            for (TypeParameterDescriptor parameter : jetType.getConstructor().getParameters()) {
                TypeProjection argument = arguments.get(parameter.getIndex());

                Variance projectionKindForKotlin = argument.getProjectionKind();
                Variance projectionKindForJava = getEffectiveVariance(
                        parameter.getVariance(),
                        projectionKindForKotlin,
                        howThisTypeIsUsed
                );
                signatureVisitor.writeTypeArgument(projectionKindForKotlin, projectionKindForJava);

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
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                String kotlinTypeName = getKotlinTypeNameForSignature(jetType, asmType);
                signatureVisitor.writeAsmType(asmType, jetType.isNullable(), kotlinTypeName);
            }
            else {
                writeGenericType(signatureVisitor, asmType, jetType, false, howThisTypeIsUsed);
            }
        }
        checkValidType(asmType);
        return asmType;
    }

    @Nullable
    private static String getKotlinTypeNameForSignature(@NotNull JetType jetType, @NotNull Type asmType) {
        ClassifierDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (descriptor == null) return null;
        if (asmType.getSort() != Type.OBJECT) return null;

        JvmClassName jvmClassName = JvmClassName.byType(asmType);
        if (JavaToKotlinClassMap.getInstance().mapPlatformClass(jvmClassName.getFqName()).size() > 1) {
            return JvmClassName.byClassDescriptor(descriptor).getSignatureName();
        }
        return null;
    }

    private void checkValidType(@NotNull Type type) {
        if (!mapBuiltinsToJava) {
            String descriptor = type.getDescriptor();
            if (!descriptor.equals("Ljava/lang/Object;")) {
                if (descriptor.startsWith("Ljava/")) {
                    throw new IllegalStateException("builtins must not reference java.* classes: " + descriptor);
                }
            }
        }
    }

    public CallableMethod mapToCallableMethod(
            @NotNull FunctionDescriptor functionDescriptor,
            boolean superCall,
            boolean isInsideClass,
            OwnerKind kind
    ) {
        final DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();

        functionDescriptor = unwrapFakeOverride(functionDescriptor);

        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal(), true, kind);
        JvmClassName owner;
        JvmClassName ownerForDefaultImpl;
        JvmClassName ownerForDefaultParam;
        int invokeOpcode;
        JvmClassName thisClass;
        if (functionParent instanceof NamespaceDescriptor) {
            assert !superCall;
            owner = jvmClassNameForNamespace((NamespaceDescriptor) functionParent);
            ownerForDefaultImpl = ownerForDefaultParam = owner;
            invokeOpcode = INVOKESTATIC;
            thisClass = null;
        }
        else if (functionDescriptor instanceof ConstructorDescriptor) {
            assert !superCall;
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            owner = JvmClassName.byType(mapType(containingClass.getDefaultType(), JetTypeMapperMode.IMPL));
            ownerForDefaultImpl = ownerForDefaultParam = owner;
            invokeOpcode = INVOKESPECIAL;
            thisClass = null;
        }
        else if (functionParent instanceof ScriptDescriptor) {
            thisClass = owner =
            ownerForDefaultParam = ownerForDefaultImpl = classNameForScriptDescriptor(bindingContext, (ScriptDescriptor) functionParent);
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
            Type type = mapType(receiver.getDefaultType(), JetTypeMapperMode.TYPE_PARAMETER);
            owner = JvmClassName.byType(type);
            ownerForDefaultParam = JvmClassName.byType(mapType(declarationOwner.getDefaultType(), JetTypeMapperMode.TYPE_PARAMETER));
            ownerForDefaultImpl = JvmClassName.byInternalName(
                    ownerForDefaultParam.getInternalName() + (originalIsInterface ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
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
                owner = JvmClassName.byInternalName(owner.getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
            }
            thisClass = JvmClassName.byType(mapType(receiver.getDefaultType()));
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
                thisClass, receiverParameterType, null);
    }

    private static boolean isAccessor(FunctionDescriptor functionDescriptor) {
        return functionDescriptor instanceof AccessorForFunctionDescriptor ||
               functionDescriptor instanceof AccessorForPropertyDescriptor.Getter ||
               functionDescriptor instanceof AccessorForPropertyDescriptor.Setter;
    }

    @NotNull
    private static FunctionDescriptor findAnyDeclaration(@NotNull FunctionDescriptor function) {
        //if (function.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
        if (function.getOverriddenDescriptors().isEmpty()) {
            return function;
        }
        else {
            // TODO: prefer class to interface
            return findAnyDeclaration(function.getOverriddenDescriptors().iterator().next());
        }
    }

    private JvmMethodSignature mapSignature(FunctionDescriptor f, boolean needGenericSignature, OwnerKind kind) {

        if (kind == OwnerKind.TRAIT_IMPL) {
            needGenericSignature = false;
        }

        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, needGenericSignature);

        writeFormalTypeParameters(f.getTypeParameters(), signatureVisitor);

        final JetType receiverType = DescriptorUtils.getReceiverParameterType(f.getReceiverParameter());
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();

        signatureVisitor.writeParametersStart();

        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) f.getContainingDeclaration();
            JetType jetType = CodegenUtil.getSuperClass(containingDeclaration);
            Type type = mapType(jetType);
            if (type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = mapType(jetType);
            }

            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            signatureVisitor.writeAsmType(type, jetType.isNullable());
            signatureVisitor.writeParameterTypeEnd();
        }
        else {
            writeThisForAccessorIfNeeded(f, signatureVisitor);
        }

        if (receiverType != null) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiverType, signatureVisitor, JetTypeMapperMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor parameter : parameters) {
            writeParameter(signatureVisitor, parameter.getType());
        }

        signatureVisitor.writeParametersEnd();

        if (f instanceof ConstructorDescriptor) {
            signatureVisitor.writeVoidReturn();
        }
        else {
            signatureVisitor.writeReturnType();
            JetType returnType = f.getReturnType();
            assert returnType != null : "Function " + f + " has no return type";
            mapReturnType(returnType, signatureVisitor);
            signatureVisitor.writeReturnTypeEnd();
        }
        return signatureVisitor.makeJvmMethodSignature(f.getName().getName());
    }

    private void writeThisForAccessorIfNeeded(FunctionDescriptor f, BothSignatureWriter signatureVisitor) {
        if (isAccessor(f) && f.getExpectedThisObject() != null) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(((ClassifierDescriptor) f.getContainingDeclaration()).getDefaultType(), signatureVisitor, JetTypeMapperMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }
    }


    public void writeFormalTypeParameters(List<TypeParameterDescriptor> typeParameters, BothSignatureWriter signatureVisitor) {
        if (signatureVisitor == null) {
            return;
        }

        signatureVisitor.writeFormalTypeParametersStart();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            writeFormalTypeParameter(typeParameterDescriptor, signatureVisitor);
        }
        signatureVisitor.writeFormalTypeParametersEnd();
    }

    private void writeFormalTypeParameter(TypeParameterDescriptor typeParameterDescriptor, BothSignatureWriter signatureVisitor) {
        signatureVisitor.writeFormalTypeParameter(typeParameterDescriptor.getName().getName(), typeParameterDescriptor.getVariance(),
                                                  typeParameterDescriptor.isReified());

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

        signatureVisitor.writeFormalTypeParameterEnd();
    }

    public JvmMethodSignature mapSignature(Name name, FunctionDescriptor f) {
        final ReceiverParameterDescriptor receiver = f.getReceiverParameter();

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        writeFormalTypeParameters(f.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        writeThisForAccessorIfNeeded(f, signatureWriter);

        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        writeReceiverIfNeeded(receiver, signatureWriter);
        for (ValueParameterDescriptor parameter : parameters) {
            writeParameter(signatureWriter, parameter.getType());
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        JetType returnType = f.getReturnType();
        assert returnType != null;
        mapReturnType(returnType, signatureWriter);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature(name.getName());
    }

    private void writeReceiverIfNeeded(@Nullable ReceiverParameterDescriptor receiver, BothSignatureWriter signatureWriter) {
        if (receiver != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiver.getType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
    }


    public JvmPropertyAccessorSignature mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        final DeclarationDescriptor parentDescriptor = descriptor.getContainingDeclaration();
        boolean isAnnotation = parentDescriptor instanceof ClassDescriptor &&
                               ((ClassDescriptor) parentDescriptor).getKind() == ClassKind.ANNOTATION_CLASS;
        String name = isAnnotation ? descriptor.getName().getName() : PropertyCodegen.getterName(descriptor.getName());

        // TODO: do not genClassOrObject generics if not needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        if (kind == OwnerKind.TRAIT_IMPL) {
            @SuppressWarnings("ConstantConditions") ClassDescriptor containingDeclaration = (ClassDescriptor) parentDescriptor;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, JetTypeMapperMode.IMPL);
            signatureWriter.writeParameterTypeEnd();
        }
        else {
            if (descriptor instanceof AccessorForPropertyDescriptor) {
                signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
                mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), signatureWriter,
                        JetTypeMapperMode.VALUE);
                signatureWriter.writeParameterTypeEnd();
            }
        }

        writeReceiverIfNeeded(descriptor.getReceiverParameter(), signatureWriter);

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        mapType(descriptor.getType(), signatureWriter, JetTypeMapperMode.VALUE, Variance.OUT_VARIANCE);
        signatureWriter.writeReturnTypeEnd();

        JvmMethodSignature jvmMethodSignature = signatureWriter.makeJvmMethodSignature(name);

        return new JvmPropertyAccessorSignature(jvmMethodSignature, jvmMethodSignature.getKotlinReturnType());
    }

    @NotNull
    public JvmPropertyAccessorSignature mapSetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        assert descriptor.isVar();

        // TODO: generics signature is not always needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        JetType outType = descriptor.getType();

        signatureWriter.writeParametersStart();

        String name = PropertyCodegen.setterName(descriptor.getName());
        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
        else {
            if (descriptor instanceof AccessorForPropertyDescriptor) {
                signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
                mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), signatureWriter,
                        JetTypeMapperMode.VALUE);
                signatureWriter.writeParameterTypeEnd();
            }
        }

        writeReceiverIfNeeded(descriptor.getReceiverParameter(), signatureWriter);

        writeParameter(signatureWriter, outType);

        signatureWriter.writeParametersEnd();

        signatureWriter.writeVoidReturn();

        JvmMethodSignature jvmMethodSignature = signatureWriter.makeJvmMethodSignature(name);
        return new JvmPropertyAccessorSignature(jvmMethodSignature,
                                                jvmMethodSignature.getKotlinParameterType(jvmMethodSignature.getParameterCount() - 1));
    }

    private void writeParameter(BothSignatureWriter signatureWriter, JetType outType) {
        signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
        mapType(outType, signatureWriter, JetTypeMapperMode.VALUE);
        signatureWriter.writeParameterTypeEnd();
    }

    private JvmMethodSignature mapConstructorSignature(ConstructorDescriptor descriptor, CalculatedClosure closure) {

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        // constructor type parmeters are fake
        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        ClassDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        final ClassDescriptor captureThis = closure != null ? closure.getCaptureThis() : null;
        if (captureThis != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.OUTER);
            mapType(captureThis.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        final ClassifierDescriptor captureReceiver = closure != null ? closure.getCaptureReceiver() : null;
        if (captureReceiver != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(captureReceiver.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
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
                if (variableDescriptor instanceof VariableDescriptor && !(variableDescriptor instanceof PropertyDescriptor)) {
                    Type sharedVarType = getSharedVarType(variableDescriptor);
                    if (sharedVarType == null) {
                        sharedVarType = mapType(((VariableDescriptor) variableDescriptor).getType());
                    }
                    signatureWriter.writeParameterType(JvmMethodParameterKind.SHARED_VAR);
                    signatureWriter.writeAsmType(sharedVarType, false);
                    signatureWriter.writeParameterTypeEnd();
                }
            }

            final JetDelegatorToSuperCall superCall = closure.getSuperCall();
            if (superCall != null) {
                DeclarationDescriptor superDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET,
                                                                                                    superCall
                                                                                                            .getCalleeExpression()
                                                                                                            .getConstructorReferenceExpression());

                if(superDescriptor instanceof ConstructorDescriptor) {
                    final ConstructorDescriptor superConstructor = (ConstructorDescriptor) superDescriptor;

                    if (isObjectLiteral(bindingContext, descriptor.getContainingDeclaration())) {
                        //noinspection SuspiciousMethodCalls
                        CallableMethod superCallable = mapToCallableMethod(superConstructor);
                        final List<JvmMethodParameterSignature> types = superCallable.getSignature().getKotlinParameterTypes();
                        if (types != null) {
                            for (JvmMethodParameterSignature type : types) {
                                signatureWriter.writeParameterType(JvmMethodParameterKind.SUPER_CALL_PARAM);
                                signatureWriter.writeAsmType(type.getAsmType(), false);
                                signatureWriter.writeParameterTypeEnd();
                            }
                        }
                    }
                }
            }
        }

        for (ValueParameterDescriptor parameter : descriptor.getOriginal().getValueParameters()) {
            writeParameter(signatureWriter, parameter.getType());
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeVoidReturn();

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        for (ScriptDescriptor importedScript : importedScripts) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            final ClassDescriptor descriptor = bindingContext.get(CLASS_FOR_FUNCTION, importedScript);
            assert descriptor != null;
            mapType(descriptor.getDefaultType(), signatureWriter, JetTypeMapperMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor valueParameter : script.getValueParameters()) {
            writeParameter(signatureWriter, valueParameter.getType());
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeVoidReturn();

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor) {
        return mapToCallableMethod(descriptor, bindingContext.get(CodegenBinding.CLOSURE, descriptor.getContainingDeclaration()));
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, CalculatedClosure closure) {
        final JvmMethodSignature method = mapConstructorSignature(descriptor, closure);
        JetType defaultType = descriptor.getContainingDeclaration().getDefaultType();
        Type mapped = mapType(defaultType, JetTypeMapperMode.IMPL);
        if (mapped.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + defaultType + ", actual: " + mapped);
        }
        JvmClassName owner = JvmClassName.byType(mapped);
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
    }


    private static boolean isGenericsArray(JetType type) {
        return KotlinBuiltIns.getInstance().isArray(type) &&
               type.getArguments().get(0).getType().getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return StackValue
                    .sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            PsiElement psiElement = descriptorToDeclaration(bindingContext, descriptor);
            return classNameForAnonymousClass(bindingContext, (JetElement) psiElement).getAsmType();
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue
                    .sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof VariableDescriptor && isVarCapturedInClosure(bindingContext, descriptor)) {
            JetType outType = ((VariableDescriptor) descriptor).getType();
            return StackValue.sharedTypeForType(mapType(outType));
        }
        return null;
    }

    public JvmMethodSignature invokeSignature(FunctionDescriptor fd) {
        return mapSignature(Name.identifier("invoke"), fd);
    }

    public CallableMethod asCallableMethod(FunctionDescriptor fd) {
        JvmMethodSignature descriptor = erasedInvokeSignature(fd);
        JvmClassName owner = getInternalClassName(fd);
        Type receiverParameterType;
        ReceiverParameterDescriptor receiverParameter = fd.getOriginal().getReceiverParameter();
        if (receiverParameter != null) {
            receiverParameterType = mapType(receiverParameter.getType());
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(
                owner, null, null, descriptor, INVOKEVIRTUAL,
                getInternalClassName(fd), receiverParameterType, getInternalClassName(fd).getAsmType());
    }
}
