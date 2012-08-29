/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.CalculatedClosure;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.codegen.signature.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.context.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;

/**
 * @author yole
 * @author alex.tkachman
 */
public class JetTypeMapper {
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Type JAVA_NUMBER_TYPE = Type.getType(Number.class);
    public static final Type JAVA_STRING_BUILDER_TYPE = Type.getType(StringBuilder.class);
    public static final Type JAVA_STRING_TYPE = Type.getType(String.class);
    public static final Type JAVA_THROWABLE_TYPE = Type.getType(Throwable.class);
    public static final Type JAVA_CLASS_TYPE = Type.getType(Class.class);
    public static final Type JAVA_BOOLEAN_TYPE = Type.getType(Boolean.class);
    public static final Type JAVA_ARRAY_GENERIC_TYPE = Type.getType(Object[].class);

    public static final Type JET_NOTHING_TYPE = Type.getObjectType("jet/Nothing");
    public static final Type JET_TUPLE0_TYPE = Type.getObjectType("jet/Tuple0");
    public static final Type JET_FUNCTION0_TYPE = Type.getObjectType("jet/Function0");
    public static final Type JET_FUNCTION1_TYPE = Type.getObjectType("jet/Function1");
    public static final Type JET_ITERATOR_TYPE = Type.getObjectType("jet/Iterator");
    public static final Type JET_INT_RANGE_TYPE = Type.getObjectType("jet/IntRange");
    public static final Type JET_SHARED_VAR_TYPE = Type.getObjectType("jet/runtime/SharedVar$Object");
    public static final Type JET_SHARED_INT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Int");
    public static final Type JET_SHARED_DOUBLE_TYPE = Type.getObjectType("jet/runtime/SharedVar$Double");
    public static final Type JET_SHARED_FLOAT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Float");
    public static final Type JET_SHARED_BYTE_TYPE = Type.getObjectType("jet/runtime/SharedVar$Byte");
    public static final Type JET_SHARED_SHORT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Short");
    public static final Type JET_SHARED_CHAR_TYPE = Type.getObjectType("jet/runtime/SharedVar$Char");
    public static final Type JET_SHARED_LONG_TYPE = Type.getObjectType("jet/runtime/SharedVar$Long");
    public static final Type JET_SHARED_BOOLEAN_TYPE = Type.getObjectType("jet/runtime/SharedVar$Boolean");

    public BindingContext bindingContext;
    private boolean mapBuiltinsToJava;

    private ClassBuilderMode classBuilderMode;

    private static final int ACC_LOCAL = 0;
    private static final int ACC_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.PRIVATE, ACC_PRIVATE)
            .put(Visibilities.PROTECTED, ACC_PROTECTED)
            .put(Visibilities.PUBLIC, ACC_PUBLIC)
            .put(Visibilities.INTERNAL, ACC_PUBLIC)
            .put(Visibilities.LOCAL, ACC_LOCAL)
            .build();

    @Inject
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Inject
    public void setBuiltinToJavaTypesMapping(BuiltinToJavaTypesMapping builtinToJavaTypesMapping) {
        mapBuiltinsToJava = builtinToJavaTypesMapping == BuiltinToJavaTypesMapping.ENABLED;
    }

    @Inject
    public void setClassBuilderMode(ClassBuilderMode classBuilderMode) {
        this.classBuilderMode = classBuilderMode;
    }

    public boolean hasThis0(ClassDescriptor classDescriptor) {
        //noinspection SuspiciousMethodCalls
        final CalculatedClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        return closure != null && closure.getCaptureThis() != null;
    }

    public CalculatedClosure getCalculatedClosure(ClassDescriptor classDescriptor) {
        //noinspection SuspiciousMethodCalls
        return bindingContext.get(CLOSURE, classDescriptor);
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    public static Type correctElementType(Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    @NotNull
    public JvmClassName getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        MapTypeMode mapTypeMode = ownerKindToMapTypeMode(kind);

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            return jvmClassNameForNamespace((NamespaceDescriptor) containingDeclaration);
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            if (kind instanceof OwnerKind.DelegateKind) {
                mapTypeMode = MapTypeMode.IMPL;
            }
            else {
                if (classDescriptor.getKind() == ClassKind.OBJECT) {
                    mapTypeMode = MapTypeMode.IMPL;
                }
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

    private static MapTypeMode ownerKindToMapTypeMode(OwnerKind kind) {
        if (kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.NAMESPACE || kind instanceof OwnerKind.StaticDelegateKind) {
            return MapTypeMode.IMPL;
        }
        else if (kind == OwnerKind.TRAIT_IMPL) {
            return MapTypeMode.TRAIT_IMPL;
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
            r.append("namespace");
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
        if (jetType.equals(JetStandardClasses.getUnitType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(Type.VOID_TYPE, false);
            }
            return Type.VOID_TYPE;
        }
        else if (jetType.equals(JetStandardClasses.getNothingType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeNothing(false);
            }
            return Type.VOID_TYPE;
        }
        if (jetType.equals(JetStandardClasses.getNullableNothingType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeNothing(true);
            }
            return OBJECT_TYPE;
        }
        return mapType(jetType, signatureVisitor, MapTypeMode.VALUE);
    }

    @Nullable
    public static String getLocalNameForObject(JetObjectDeclaration object) {
        PsiElement parent = object.getParent();
        if (parent instanceof JetClassObject) {
            return JvmAbi.CLASS_OBJECT_CLASS_NAME;
        }

        return null;
    }

    public JvmClassName getJvmClassName(ClassDescriptor classDescriptor) {
        return JvmClassName.byInternalName(getJvmInternalFQName(classDescriptor));
    }

    @NotNull
    private String getJvmInternalFQName(@NotNull DeclarationDescriptor descriptor) {
        descriptor = descriptor.getOriginal();

        if (descriptor instanceof FunctionDescriptor) {
            throw new IllegalStateException("requested fq name for function: " + descriptor);
        }

        if (descriptor.getContainingDeclaration() instanceof ModuleDescriptor || descriptor instanceof ScriptDescriptor) {
            return "";
        }

        if (descriptor instanceof ModuleDescriptor) {
            throw new IllegalStateException("missed something");
        }

        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor klass = (ClassDescriptor) descriptor;
            if (klass.getKind() == ClassKind.OBJECT || klass.getKind() == ClassKind.CLASS_OBJECT) {
                if (klass.getContainingDeclaration() instanceof ClassDescriptor) {
                    ClassDescriptor containingKlass = (ClassDescriptor) klass.getContainingDeclaration();
                    if (containingKlass.getKind() == ClassKind.ENUM_CLASS) {
                        return getJvmInternalFQName(containingKlass);
                    }
                    else {
                        return getJvmInternalFQName(containingKlass) + JvmAbi.CLASS_OBJECT_SUFFIX;
                    }
                }
            }
            else if (klass.getKind() == ClassKind.ENUM_ENTRY) {
                if (enumEntryNeedSubclass(bindingContext, klass)) {
                    return getJvmInternalFQName(klass.getContainingDeclaration()) + "$" + klass.getName().getName();
                }
                else {
                    return getJvmInternalFQName(klass.getContainingDeclaration());
                }
            }

            JvmClassName name = classNameForClassDescriptor(bindingContext, (ClassDescriptor) descriptor);
            if (name != null) {
                return name.getInternalName();
            }
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();

        if (container == null) {
            throw new IllegalStateException("descriptor has no container: " + descriptor);
        }

        Name name = descriptor.getName();

        String baseName = getJvmInternalFQName(container);
        if (!baseName.isEmpty()) {
            return baseName + (container instanceof NamespaceDescriptor ? "/" : "$") + name.getIdentifier();
        }

        return name.getIdentifier();
    }

    @NotNull
    public Type mapType(@NotNull final JetType jetType, @NotNull MapTypeMode kind) {
        return mapType(jetType, null, kind);
    }

    @NotNull
    public Type mapType(JetType jetType, @Nullable BothSignatureWriter signatureVisitor, @NotNull MapTypeMode kind) {
        Type known = null;
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();

        if (mapBuiltinsToJava) {
            if (classifier instanceof ClassDescriptor) {
                known = KotlinToJavaTypesMap.getInstance().getJavaAnalog(jetType);
            }
        }

        if (known != null) {
            if (kind == MapTypeMode.VALUE) {
                return mapKnownAsmType(jetType, known, signatureVisitor);
            }
            else if (kind == MapTypeMode.TYPE_PARAMETER) {
                return mapKnownAsmType(jetType, boxType(known), signatureVisitor);
            }
            else if (kind == MapTypeMode.TRAIT_IMPL) {
                throw new IllegalStateException("TRAIT_IMPL is not possible for " + jetType);
            }
            else if (kind == MapTypeMode.IMPL) {
                //noinspection ConstantConditions
                if (mapBuiltinsToJava) {
                    // TODO: enable and fix tests
                    //throw new IllegalStateException("must not map known type to IMPL when not compiling builtins: " + jetType);
                }
                return mapKnownAsmType(jetType, known, signatureVisitor);
            }
            else {
                throw new IllegalStateException("unknown kind: " + kind);
            }
        }

        final TypeConstructor constructor = jetType.getConstructor();
        if (constructor instanceof IntersectionTypeConstructor) {
            jetType = CommonSupertypes.commonSupertype(new ArrayList<JetType>(constructor.getSupertypes()));
        }
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();

        if (descriptor == null) {
            throw new UnsupportedOperationException("no descriptor for type constructor of " + jetType);
        }

        if (ErrorUtils.isError(descriptor)) {
            if (classBuilderMode != ClassBuilderMode.SIGNATURES) {
                throw new IllegalStateException("error types are not allowed when classBuilderMode = " + classBuilderMode);
            }
            Type asmType = Type.getObjectType("error/NonExistentClass");
            if (signatureVisitor != null) {
                writeSimpleType(signatureVisitor, asmType, true);
            }
            checkValidType(asmType);
            return asmType;
        }

        if (descriptor instanceof ClassDescriptor
            && JetStandardLibrary.getInstance().isArray(jetType)
            && mapBuiltinsToJava) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            JetType memberType = jetType.getArguments().get(0).getType();

            if (signatureVisitor != null) {
                signatureVisitor.writeArrayType(jetType.isNullable());
                mapType(memberType, signatureVisitor, MapTypeMode.TYPE_PARAMETER);
                signatureVisitor.writeArrayEnd();
            }

            Type r;
            if (!isGenericsArray(jetType)) {
                r = Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
            }
            else {
                r = JAVA_ARRAY_GENERIC_TYPE;
            }
            checkValidType(r);
            return r;
        }

        if (descriptor instanceof ClassDescriptor) {
            JvmClassName name = getJvmClassName((ClassDescriptor) descriptor);
            Type asmType = Type.getObjectType(name.getInternalName() + (kind == MapTypeMode.TRAIT_IMPL ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
            boolean forceReal = KotlinToJavaTypesMap.getInstance().isForceReal(name);

            writeGenericType(jetType, signatureVisitor, asmType, forceReal);

            checkValidType(asmType);
            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {

            Type type = mapType(((TypeParameterDescriptor) descriptor).getUpperBoundsAsType(), kind);
            if (signatureVisitor != null) {
                TypeParameterDescriptor typeParameterDescriptor =
                        (TypeParameterDescriptor) jetType.getConstructor().getDeclarationDescriptor();
                assert typeParameterDescriptor != null;
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
            }
            checkValidType(type);
            return type;
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    private void writeGenericType(JetType jetType, BothSignatureWriter signatureVisitor, Type asmType, boolean forceReal) {
        if (signatureVisitor != null) {
            signatureVisitor.writeClassBegin(asmType.getInternalName(), jetType.isNullable(), forceReal);
            for (TypeProjection proj : jetType.getArguments()) {
                // TODO: +-
                signatureVisitor.writeTypeArgument(proj.getProjectionKind());
                mapType(proj.getType(), signatureVisitor, MapTypeMode.TYPE_PARAMETER);
                signatureVisitor.writeTypeArgumentEnd();
            }
            signatureVisitor.writeClassEnd();
        }
    }

    private Type mapKnownAsmType(JetType jetType, Type asmType, @Nullable BothSignatureWriter signatureVisitor) {
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                writeSimpleType(signatureVisitor, asmType, jetType.isNullable());
            }
            else {
                writeGenericType(jetType, signatureVisitor, asmType, false);
            }
        }
        checkValidType(asmType);
        return asmType;
    }

    private static void writeSimpleType(BothSignatureWriter visitor, Type asmType, boolean nullable) {
        visitor.writeAsmType(asmType, nullable);
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

    public static Type unboxType(final Type type) {
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByWrapperAsmType(type);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getAsmType();
        }
        else {
            throw new UnsupportedOperationException("Unboxing: " + type);
        }
    }

    public static Type boxType(Type asmType) {
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByAsmType(asmType);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getWrapper().getAsmType();
        }
        else {
            return asmType;
        }
    }

    public CallableMethod mapToCallableMethod(@NotNull FunctionDescriptor functionDescriptor, boolean superCall, OwnerKind kind) {
        final DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();

        while (functionDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            functionDescriptor = functionDescriptor.getOverriddenDescriptors().iterator().next();
        }

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
            owner = JvmClassName.byType(mapType(containingClass.getDefaultType(), MapTypeMode.IMPL));
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

            boolean originalIsInterface = CodegenUtil.isInterface(declarationOwner);
            boolean currentIsInterface = CodegenUtil.isInterface(currentOwner);

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
            Type type = mapType(receiver.getDefaultType(), MapTypeMode.TYPE_PARAMETER);
            owner = JvmClassName.byType(type);
            ownerForDefaultParam = JvmClassName.byType(mapType(declarationOwner.getDefaultType(), MapTypeMode.TYPE_PARAMETER));
            ownerForDefaultImpl = JvmClassName.byInternalName(
                    ownerForDefaultParam.getInternalName() + (originalIsInterface ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));

            invokeOpcode = isInterface
                           ? (superCall ? INVOKESTATIC : INVOKEINTERFACE)
                           : (isAccessor ? INVOKESTATIC : (superCall ? INVOKESPECIAL : INVOKEVIRTUAL));
            if (isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor, false, OwnerKind.TRAIT_IMPL);
                owner = JvmClassName.byInternalName(owner.getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
            }
            thisClass = JvmClassName.byType(mapType(receiver.getDefaultType(), MapTypeMode.VALUE));
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }


        Type receiverParameterType;
        if (functionDescriptor.getReceiverParameter().exists()) {
            receiverParameterType = mapType(functionDescriptor.getOriginal().getReceiverParameter().getType(), MapTypeMode.VALUE);
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

        final ReceiverDescriptor receiverTypeRef = f.getReceiverParameter();
        final JetType receiverType = !receiverTypeRef.exists() ? null : receiverTypeRef.getType();
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();

        signatureVisitor.writeParametersStart();

        if (isAccessor(f)) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(((ClassifierDescriptor) f.getContainingDeclaration()).getDefaultType(), signatureVisitor, MapTypeMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }

        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) f.getContainingDeclaration();
            JetType jetType = TraitImplBodyCodegen.getSuperClass(containingDeclaration);
            Type type = mapType(jetType, MapTypeMode.VALUE);
            if (type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = mapType(jetType, MapTypeMode.VALUE);
            }

            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            signatureVisitor.writeAsmType(type, jetType.isNullable());
            signatureVisitor.writeParameterTypeEnd();
        }

        if (receiverType != null) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiverType, signatureVisitor, MapTypeMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor parameter : parameters) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getType(), signatureVisitor, MapTypeMode.VALUE);
            signatureVisitor.writeParameterTypeEnd();
        }

        signatureVisitor.writeParametersEnd();

        if (f instanceof ConstructorDescriptor) {
            signatureVisitor.writeVoidReturn();
        }
        else {
            signatureVisitor.writeReturnType();
            JetType returnType = f.getReturnType();
            assert returnType != null;
            mapReturnType(returnType, signatureVisitor);
            signatureVisitor.writeReturnTypeEnd();
        }
        return signatureVisitor.makeJvmMethodSignature(f.getName().getName());
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
                    if (!CodegenUtil.isInterface(jetType)) {
                        mapType(jetType, signatureVisitor, MapTypeMode.TYPE_PARAMETER);
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
                if (CodegenUtil.isInterface(jetType)) {
                    signatureVisitor.writeInterfaceBound();
                    mapType(jetType, signatureVisitor, MapTypeMode.TYPE_PARAMETER);
                    signatureVisitor.writeInterfaceBoundEnd();
                }
            }
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                signatureVisitor.writeInterfaceBound();
                mapType(jetType, signatureVisitor, MapTypeMode.TYPE_PARAMETER);
                signatureVisitor.writeInterfaceBoundEnd();
            }
        }

        signatureVisitor.writeFormalTypeParameterEnd();
    }

    public JvmMethodSignature mapSignature(Name name, FunctionDescriptor f) {
        final ReceiverDescriptor receiver = f.getReceiverParameter();

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        writeFormalTypeParameters(f.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        if (isAccessor(f)) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(((ClassifierDescriptor) f.getContainingDeclaration()).getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        if (receiver.exists()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiver.getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
        for (ValueParameterDescriptor parameter : parameters) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        JetType returnType = f.getReturnType();
        assert returnType != null;
        mapReturnType(returnType, signatureWriter);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature(name.getName());
    }


    public JvmPropertyAccessorSignature mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        final DeclarationDescriptor parentDescriptor = descriptor.getContainingDeclaration();
        boolean isAnnotation = parentDescriptor instanceof ClassDescriptor &&
                               ((ClassDescriptor) parentDescriptor).getKind() == ClassKind.ANNOTATION_CLASS;
        String name = isAnnotation ? descriptor.getName().getName() : PropertyCodegen.getterName(descriptor.getName());

        // TODO: do not generate generics if not needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        if (kind == OwnerKind.TRAIT_IMPL) {
            @SuppressWarnings("ConstantConditions") ClassDescriptor containingDeclaration = (ClassDescriptor) parentDescriptor;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.IMPL);
            signatureWriter.writeParameterTypeEnd();
        }
        else {
            if (descriptor instanceof AccessorForPropertyDescriptor) {
                signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
                mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), signatureWriter,
                        MapTypeMode.VALUE);
                signatureWriter.writeParameterTypeEnd();
            }
        }

        if (descriptor.getReceiverParameter().exists()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(descriptor.getReceiverParameter().getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        mapType(descriptor.getType(), signatureWriter, MapTypeMode.VALUE);
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
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
        else {
            if (descriptor instanceof AccessorForPropertyDescriptor) {
                signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
                mapType(((ClassifierDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), signatureWriter,
                        MapTypeMode.VALUE);
                signatureWriter.writeParameterTypeEnd();
            }
        }

        if (descriptor.getReceiverParameter().exists()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(descriptor.getReceiverParameter().getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
        mapType(outType, signatureWriter, MapTypeMode.VALUE);
        signatureWriter.writeParameterTypeEnd();

        signatureWriter.writeParametersEnd();

        signatureWriter.writeVoidReturn();

        JvmMethodSignature jvmMethodSignature = signatureWriter.makeJvmMethodSignature(name);
        return new JvmPropertyAccessorSignature(jvmMethodSignature,
                                                jvmMethodSignature.getKotlinParameterType(jvmMethodSignature.getParameterCount() - 1));
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
            mapType(captureThis.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        final ClassifierDescriptor captureReceiver = closure != null ? closure.getCaptureReceiver() : null;
        if (captureReceiver != null) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(captureReceiver.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        if (containingDeclaration.getKind() == ClassKind.ENUM_CLASS || containingDeclaration.getKind() == ClassKind.ENUM_ENTRY) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_NAME);
            mapType(JetStandardLibrary.getInstance().getStringType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
            signatureWriter.writeParameterType(JvmMethodParameterKind.ENUM_ORDINAL);
            mapType(JetStandardLibrary.getInstance().getIntType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        if (closure != null) {
            for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closure.getCaptureVariables().entrySet()) {
                if (entry.getKey() instanceof VariableDescriptor && !(entry.getKey() instanceof PropertyDescriptor)) {
                    Type sharedVarType = getSharedVarType(entry.getKey());
                    if (sharedVarType == null) {
                        sharedVarType = mapType(((VariableDescriptor) entry.getKey()).getType(), MapTypeMode.VALUE);
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
                        CallableMethod superCallable = mapToCallableMethod(superConstructor,
                                                                           getCalculatedClosure(superConstructor.getContainingDeclaration()));
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
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
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
            mapType(descriptor.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor valueParameter : script.getValueParameters()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(valueParameter.getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeVoidReturn();

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, CalculatedClosure closure) {
        final JvmMethodSignature method = mapConstructorSignature(descriptor, closure);
        JetType defaultType = descriptor.getContainingDeclaration().getDefaultType();
        Type mapped = mapType(defaultType, MapTypeMode.IMPL);
        if (mapped.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + defaultType + ", actual: " + mapped);
        }
        JvmClassName owner = JvmClassName.byType(mapped);
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
    }

    //TODO: move mapping logic to front-end java
    public static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor) {
        Integer specialCase = specialCaseVisibility(descriptor);
        if (specialCase != null) {
            return specialCase;
        }
        Integer defaultMapping = visibilityToAccessFlag.get(descriptor.getVisibility());
        if (defaultMapping == null) {
            throw new IllegalStateException(descriptor.getVisibility() + " is not a valid visibility in backend.");
        }
        return defaultMapping;
    }

    @Nullable
    private static Integer specialCaseVisibility(@NotNull MemberDescriptor memberDescriptor) {
        DeclarationDescriptor containingDeclaration = memberDescriptor.getContainingDeclaration();
        if (CodegenUtil.isInterface(containingDeclaration)) {
            return ACC_PUBLIC;
        }
        Visibility memberVisibility = memberDescriptor.getVisibility();
        if (memberVisibility != Visibilities.PRIVATE) {
            return null;
        }
        if (isClassObject(containingDeclaration)) {
            return ACC_PACKAGE_PRIVATE;
        }
        if (memberDescriptor instanceof ConstructorDescriptor) {
            ClassKind kind = ((ClassDescriptor) containingDeclaration).getKind();
            if (kind == ClassKind.OBJECT) {
                //TODO: should be ACC_PACKAGE_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2700
                return ACC_PUBLIC;
            }
            else if (kind == ClassKind.ENUM_ENTRY) {
                return ACC_PACKAGE_PRIVATE;
            }
            else if (kind == ClassKind.ENUM_CLASS) {
                //TODO: should be ACC_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2680
                return ACC_PROTECTED;
            }
        }
        if (containingDeclaration instanceof NamespaceDescriptor) {
            return ACC_PUBLIC;
        }
        return null;
    }

    private static boolean isGenericsArray(JetType type) {
        return JetStandardLibrary.getInstance().isArray(type) &&
               type.getArguments().get(0).getType().getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return StackValue
                    .sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
        }
        else if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            PsiElement psiElement = descriptorToDeclaration(bindingContext, descriptor);
            return classNameForAnonymousClass(bindingContext, (JetElement) psiElement).getAsmType();
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue
                    .sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
        }
        else if (descriptor instanceof VariableDescriptor && isVarCapturedInClosure(descriptor)) {
            JetType outType = ((VariableDescriptor) descriptor).getType();
            return StackValue.sharedTypeForType(mapType(outType, MapTypeMode.VALUE));
        }
        return null;
    }

    public boolean isVarCapturedInClosure(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return Boolean.TRUE.equals(bindingContext.get(BindingContext.CAPTURED_IN_CLOSURE, variableDescriptor)) &&
               variableDescriptor.isVar();
    }

    protected JvmMethodSignature invokeSignature(FunctionDescriptor fd) {
        return mapSignature(Name.identifier("invoke"), fd);
    }

    public CallableMethod asCallableMethod(FunctionDescriptor fd) {
        JvmMethodSignature descriptor = CodegenUtil.erasedInvokeSignature(fd);
        JvmClassName owner = CodegenUtil.getInternalClassName(fd);
        Type receiverParameterType;
        if (fd.getReceiverParameter().exists()) {
            receiverParameterType = mapType(fd.getOriginal().getReceiverParameter().getType(), MapTypeMode.VALUE);
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(
                owner, null, null, descriptor, INVOKEVIRTUAL,
                CodegenUtil.getInternalClassName(fd), receiverParameterType, CodegenUtil.getInternalClassName(fd).getAsmType());
    }
}
