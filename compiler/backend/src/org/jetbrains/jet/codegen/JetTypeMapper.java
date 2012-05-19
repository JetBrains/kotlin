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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author yole
 * @author alex.tkachman
 */
public class JetTypeMapper {
    public static final Type TYPE_OBJECT = Type.getObjectType("java/lang/Object");
    public static final Type TYPE_THROWABLE = Type.getObjectType("java/lang/Throwable");
    public static final Type TYPE_NOTHING = Type.getObjectType("jet/Nothing");
    public static final Type JL_NUMBER_TYPE = Type.getObjectType("java/lang/Number");
    public static final Type JL_STRING_BUILDER = Type.getObjectType("java/lang/StringBuilder");
    public static final Type JL_ARRAY_LIST = Type.getObjectType("java/util/ArrayList");
    public static final Type JL_STRING_TYPE = Type.getObjectType("java/lang/String");
    public static final Type JL_CHAR_SEQUENCE_TYPE = Type.getObjectType("java/lang/CharSequence");
    private static final Type JL_COMPARABLE_TYPE = Type.getObjectType("java/lang/Comparable");
    public static final Type JL_CLASS_TYPE = Type.getObjectType("java/lang/Class");

    public static final Type ARRAY_GENERIC_TYPE = Type.getType(Object[].class);

    private JetStandardLibrary standardLibrary;
    public BindingContext bindingContext;
    private ClosureAnnotator closureAnnotator;
    private CompilerSpecialMode compilerSpecialMode;
    private ClassBuilderMode classBuilderMode;


    @Inject
    public void setStandardLibrary(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    @Inject
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Inject
    public void setClosureAnnotator(ClosureAnnotator closureAnnotator) {
        this.closureAnnotator = closureAnnotator;
    }

    @Inject
    public void setCompilerSpecialMode(CompilerSpecialMode compilerSpecialMode) {
        this.compilerSpecialMode = compilerSpecialMode;
    }

    @Inject
    public void setClassBuilderMode(ClassBuilderMode classBuilderMode) {
        this.classBuilderMode = classBuilderMode;
    }

    @PostConstruct
    public void init() {
        initKnownTypes();
        initKnownTypeNames();
    }




    public boolean hasThis0(ClassDescriptor classDescriptor) {
        return closureAnnotator.hasThis0(classDescriptor);
    }

    public ClosureAnnotator getClosureAnnotator() {
        return closureAnnotator;
    }

    private final HashMap<JetType,String> knowTypeNames = new HashMap<JetType, String>();
    private final HashMap<JetType,Type> knowTypes = new HashMap<JetType, Type>();

    public static final Type TYPE_ITERATOR = Type.getObjectType("jet/Iterator");
    public static final Type TYPE_INT_RANGE = Type.getObjectType("jet/IntRange");
    public static final Type TYPE_SHARED_VAR = Type.getObjectType("jet/runtime/SharedVar$Object");
    public static final Type TYPE_SHARED_INT = Type.getObjectType("jet/runtime/SharedVar$Int");
    public static final Type TYPE_SHARED_DOUBLE = Type.getObjectType("jet/runtime/SharedVar$Double");
    public static final Type TYPE_SHARED_FLOAT = Type.getObjectType("jet/runtime/SharedVar$Float");
    public static final Type TYPE_SHARED_BYTE = Type.getObjectType("jet/runtime/SharedVar$Byte");
    public static final Type TYPE_SHARED_SHORT = Type.getObjectType("jet/runtime/SharedVar$Short");
    public static final Type TYPE_SHARED_CHAR = Type.getObjectType("jet/runtime/SharedVar$Char");
    public static final Type TYPE_SHARED_LONG = Type.getObjectType("jet/runtime/SharedVar$Long");
    public static final Type TYPE_SHARED_BOOLEAN = Type.getObjectType("jet/runtime/SharedVar$Boolean");
    public static final Type TYPE_FUNCTION0 = Type.getObjectType("jet/Function0");
    public static final Type TYPE_FUNCTION1 = Type.getObjectType("jet/Function1");

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

    public String getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        MapTypeMode mapTypeMode = ownerKindToMapTypeMode(kind);

        String owner;
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            owner = jvmClassNameForNamespace((NamespaceDescriptor) containingDeclaration);
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
            owner = asmType.getInternalName();
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
        return owner;
    }

    public static MapTypeMode ownerKindToMapTypeMode(OwnerKind kind) {
        if (kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.NAMESPACE) {
            return MapTypeMode.IMPL;
        }
        else if (kind == OwnerKind.TRAIT_IMPL) {
            return MapTypeMode.TRAIT_IMPL;
        }
        else {
            throw new IllegalStateException("must not call this method with kind = " + kind);
        }
    }

    private String jvmClassNameForNamespace(NamespaceDescriptor namespace) {
        FqName fqName = DescriptorUtils.getFQName(namespace).toSafe();
        JavaNamespaceKind javaNamespaceKind = bindingContext.get(JavaBindingContext.JAVA_NAMESPACE_KIND, namespace);
        Boolean src = bindingContext.get(BindingContext.NAMESPACE_IS_SRC, namespace);

        if (javaNamespaceKind == null && src == null) {
            throw new IllegalStateException("unknown namespace origin: " + fqName);
        }

        boolean classStatics;
        if (javaNamespaceKind != null) {
            if (javaNamespaceKind == JavaNamespaceKind.CLASS_STATICS && src != null) {
                throw new IllegalStateException(
                        "conflicting namespace " + fqName + ": it is both java statics and from src");
            }
            classStatics = javaNamespaceKind == JavaNamespaceKind.CLASS_STATICS;
        }
        else {
            classStatics = false;
        }

        return NamespaceCodegen.getJVMClassName(fqName, !classStatics);
    }

    @NotNull public Type mapReturnType(@NotNull final JetType jetType) {
        return mapReturnType(jetType, null);
    }

    @NotNull private Type mapReturnType(@NotNull final JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
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
            return TYPE_OBJECT;
        }
        return mapType(jetType, signatureVisitor, MapTypeMode.VALUE);
    }

    private String getStableNameForObject(JetObjectDeclaration object, DeclarationDescriptor descriptor) {
        String local = getLocalNameForObject(object);
        if (local == null) return null;

        DeclarationDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass != null) {
            return getFQName(containingClass) + "$" + local;
        }
        else {
            return getFQName(getContainingNamespace(descriptor)) + "/" + local;
        }
    }

    public static String getLocalNameForObject(JetObjectDeclaration object) {
        PsiElement parent = object.getParent();
        if (parent instanceof JetClassObject) {
            return JvmAbi.CLASS_OBJECT_CLASS_NAME;
        }

        return null;
    }

    public JvmClassName getClassFQName(ClassDescriptor classDescriptor) {
        return JvmClassName.byInternalName(getFQName(classDescriptor));
    }

    /**
     * @return Internal name
     *
     * @see DescriptorUtils#getFQName(DeclarationDescriptor)
     */
    public String getFQName(DeclarationDescriptor descriptor) {
        descriptor = descriptor.getOriginal();

        if(descriptor instanceof FunctionDescriptor) {
            return getFQName(descriptor.getContainingDeclaration());
        }

        if (descriptor.getContainingDeclaration() instanceof ModuleDescriptor) {
            return "";
        }

        if (descriptor instanceof ModuleDescriptor) {
            throw new IllegalStateException("missed something");
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        String name = descriptor.getName();
        if(JetPsiUtil.NO_NAME_PROVIDED.equals(name)) {
            return closureAnnotator.classNameForAnonymousClass((JetElement) BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor));
        }
        if(name.contains("/"))
            return name;
        if (container != null) {
            String baseName = getFQName(container);
            if (!baseName.isEmpty()) { 
                return baseName + (container instanceof NamespaceDescriptor ? "/" : "$") + name;
            }
        }

        return name;
    }

    private static ClassDescriptor getContainingClass(DeclarationDescriptor descriptor) {
        DeclarationDescriptor parent = descriptor.getContainingDeclaration();
        if (parent == null || parent instanceof ClassDescriptor) return (ClassDescriptor) parent;
        return getContainingClass(parent);
    }

    private static NamespaceDescriptor getContainingNamespace(DeclarationDescriptor descriptor) {
        DeclarationDescriptor parent = descriptor.getContainingDeclaration();
        if (parent == null || parent instanceof NamespaceDescriptor) return (NamespaceDescriptor) parent;
        return getContainingNamespace(parent);
    }

    @NotNull
    public Type mapType(@NotNull final JetType jetType, @NotNull MapTypeMode kind) {
        return mapType(jetType, null, kind);
    }

    @NotNull
    public Type mapType(JetType jetType, @Nullable BothSignatureWriter signatureVisitor, @NotNull MapTypeMode kind) {
        Type known = knowTypes.get(jetType);
        if (known != null) {
            if (kind == MapTypeMode.VALUE) {
                return mapKnownAsmType(jetType, known, signatureVisitor, false);
            }
            else if (kind == MapTypeMode.TYPE_PARAMETER) {
                return mapKnownAsmType(jetType, known, signatureVisitor, true);
            }
            else if (kind == MapTypeMode.TRAIT_IMPL) {
                throw new IllegalStateException("TRAIT_IMPL is not possible for " + jetType);
            }
            else if (kind == MapTypeMode.IMPL) {
                if (compilerSpecialMode != CompilerSpecialMode.BUILTINS) {
                    // TODO: enable and fix tests
                    //throw new IllegalStateException("must not map known type to IMPL when not compiling builtins: " + jetType);
                }
                // fall through
            }
            else {
                throw new IllegalStateException("unknown kind: " + kind);
            }
        }

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();

        if (ErrorUtils.isError(descriptor)) {
            if (classBuilderMode != ClassBuilderMode.SIGNATURES) {
                throw new IllegalStateException("error types are not allowed when classBuilderMode = " + classBuilderMode);
            }
            Type asmType = Type.getObjectType("error/NonExistentClass");
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, asmType, true);
            }
            checkValidType(asmType);
            return asmType;
        }

        if (standardLibrary.getArray().equals(descriptor)) {
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
                r = ARRAY_GENERIC_TYPE;
            }
            checkValidType(r);
            return r;
        }

        if (JetStandardClasses.getAny().equals(descriptor)) {
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, TYPE_OBJECT, jetType.isNullable());
            }
            checkValidType(TYPE_OBJECT);
            return TYPE_OBJECT;
        }

        if (descriptor instanceof ClassDescriptor) {

            Type asmType;
            boolean forceReal;

            if (standardLibrary.getComparable().equals(descriptor)) {
                if (jetType.getArguments().size() != 1) {
                    throw new UnsupportedOperationException("Comparable must have one type argument");
                }

                asmType = JL_COMPARABLE_TYPE;
                forceReal = false;
            }
            else {
                JvmClassName name = getClassFQName((ClassDescriptor) descriptor);
                asmType = Type.getObjectType(name.getInternalName() + (kind == MapTypeMode.TRAIT_IMPL ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
                forceReal = isForceReal(name);
            }

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

            checkValidType(asmType);
            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {

            Type type = mapType(((TypeParameterDescriptor) descriptor).getUpperBoundsAsType(), kind);
            if (signatureVisitor != null) {
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) jetType.getConstructor().getDeclarationDescriptor();
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
            }
            checkValidType(type);
            return type;
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }
    
    private Type mapKnownAsmType(JetType jetType, Type asmType, @Nullable BothSignatureWriter signatureVisitor, boolean boxPrimitive) {
        if (boxPrimitive) {
            Type boxed = boxType(asmType);
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, boxed, jetType.isNullable());
            }
            checkValidType(boxed);
            return boxed;
        }
        else {
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, asmType, jetType.isNullable());
            }
            checkValidType(asmType);
            return asmType;
        }
    }

    public static void visitAsmType(BothSignatureWriter visitor, Type asmType, boolean nullable) {
        visitor.writeAsmType(asmType, nullable);
    }

    private void checkValidType(@NotNull Type type) {
        if (compilerSpecialMode == CompilerSpecialMode.BUILTINS) {
            String descriptor = type.getDescriptor();
            if (descriptor.equals("Ljava/lang/Object;")) {
                return;
            }
            else if (descriptor.startsWith("Ljava/")) {
                throw new IllegalStateException("builtins must not reference java.* classes: " + descriptor);
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

    public CallableMethod mapToCallableMethod(FunctionDescriptor functionDescriptor, boolean superCall, OwnerKind kind) {
        if(functionDescriptor == null)
            return null;

        final DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();
        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal(), true, kind);
        String owner;
        String ownerForDefaultImpl;
        String ownerForDefaultParam;
        int invokeOpcode;
        ClassDescriptor thisClass;
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
            owner = mapType(containingClass.getDefaultType(), MapTypeMode.IMPL).getInternalName();
            ownerForDefaultImpl = ownerForDefaultParam = owner;
            invokeOpcode = INVOKESPECIAL;
            thisClass = null;
        }
        else if (functionParent instanceof ClassDescriptor) {
            
            FunctionDescriptor declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor);

            ClassDescriptor currentOwner = (ClassDescriptor) functionParent;
            ClassDescriptor declarationOwner = (ClassDescriptor) declarationFunctionDescriptor.getContainingDeclaration();

            boolean originalIsInterface = CodegenUtil.isInterface(declarationOwner);
            boolean currentIsInterface = CodegenUtil.isInterface(currentOwner);

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
            owner = type.getInternalName();
            ownerForDefaultParam = mapType(declarationOwner.getDefaultType(), MapTypeMode.TYPE_PARAMETER).getInternalName();
            ownerForDefaultImpl = ownerForDefaultParam
                    + (originalIsInterface ? JvmAbi.TRAIT_IMPL_SUFFIX : "");

            invokeOpcode = isInterface
                    ? (superCall ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE)
                    : (superCall ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL);
            if(isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor, false, OwnerKind.TRAIT_IMPL);
                owner += JvmAbi.TRAIT_IMPL_SUFFIX;
            }
            thisClass = receiver;
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }

        final CallableMethod result = new CallableMethod(owner, ownerForDefaultImpl, ownerForDefaultParam, descriptor, invokeOpcode);
        result.setNeedsThis(thisClass);
        if(functionDescriptor.getReceiverParameter().exists()) {
            result.setNeedsReceiver(functionDescriptor);
        }

        return result;
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

        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) f.getContainingDeclaration();
            JetType jetType = TraitImplBodyCodegen.getSuperClass(containingDeclaration, bindingContext);
            Type type = mapType(jetType, MapTypeMode.VALUE);
            if(type.getInternalName().equals("java/lang/Object")) {
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
            mapReturnType(f.getReturnType(), signatureVisitor);
            signatureVisitor.writeReturnTypeEnd();
        }
        return signatureVisitor.makeJvmMethodSignature(f.getName());
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
        signatureVisitor.writeFormalTypeParameter(typeParameterDescriptor.getName(), typeParameterDescriptor.getVariance(), typeParameterDescriptor.isReified());

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

    public JvmMethodSignature mapSignature(String name, FunctionDescriptor f) {
        final ReceiverDescriptor receiver = f.getReceiverParameter();
        
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);
        
        writeFormalTypeParameters(f.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();
        
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
        mapReturnType(f.getReturnType(), signatureWriter);
        signatureWriter.writeReturnTypeEnd();
        
        return signatureWriter.makeJvmMethodSignature(name);
    }

    
    public JvmPropertyAccessorSignature mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        String name = PropertyCodegen.getterName(descriptor.getName());

        // TODO: do not generate generics if not needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);

        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.IMPL);
            signatureWriter.writeParameterTypeEnd();
        }

        if(descriptor.getReceiverParameter().exists()) {
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

    @Nullable
    public JvmPropertyAccessorSignature mapSetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        if (!descriptor.isVar()) {
            return null;
        }

        // TODO: generics signature is not always needed
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);
        
        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        JetType outType = descriptor.getType();

        signatureWriter.writeParametersStart();

        String name = PropertyCodegen.setterName(descriptor.getName());
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        if(descriptor.getReceiverParameter().exists()) {
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
        return new JvmPropertyAccessorSignature(jvmMethodSignature, jvmMethodSignature.getKotlinParameterType(jvmMethodSignature.getParameterCount() - 1));
    }

    private JvmMethodSignature mapConstructorSignature(ConstructorDescriptor descriptor, boolean hasThis0) {
        
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);
        
        List<ValueParameterDescriptor> parameters = descriptor.getOriginal().getValueParameters();

        // constructor type parmeters are fake
        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        if (hasThis0) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS0);
            mapType(closureAnnotator.getEclosingClassDescriptor(descriptor.getContainingDeclaration()).getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }

        for (ValueParameterDescriptor parameter : parameters) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
        }
        
        signatureWriter.writeParametersEnd();
        
        signatureWriter.writeVoidReturn();

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind, boolean hasThis0) {
        final JvmMethodSignature method = mapConstructorSignature(descriptor, hasThis0);
        MapTypeMode mapTypeMode = ownerKindToMapTypeMode(kind);
        JetType defaultType = descriptor.getContainingDeclaration().getDefaultType();
        Type mapped = mapType(defaultType, mapTypeMode);
        if (mapped.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + defaultType + ", actual: " + mapped);
        }
        String owner = mapped.getInternalName();
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL);
    }

    public static int getAccessModifiers(MemberDescriptor p, int defaultFlags) {
        DeclarationDescriptor declaration = p.getContainingDeclaration();
        if(CodegenUtil.isInterface(declaration)) {
            return ACC_PUBLIC;
        }
        if (p.getVisibility() == Visibilities.PUBLIC) {
            return ACC_PUBLIC;
        }
        else if (p.getVisibility() == Visibilities.PROTECTED) {
            return ACC_PROTECTED;
        }
        else if (p.getVisibility() == Visibilities.PRIVATE) {
            if(DescriptorUtils.isClassObject(declaration)) {
                return defaultFlags;
            }
            return ACC_PRIVATE;
        }
        else {
            return defaultFlags;
        }
    }

    public Collection<String> allJvmNames(JetClassOrObject jetClass) {
        Set<String> result = new HashSet<String>();
        final ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, jetClass);
        if (classDescriptor != null) {
            result.add(mapType(classDescriptor.getDefaultType(), MapTypeMode.IMPL).getInternalName());
        }
        return result;
    }

    private void initKnownTypeNames() {
        knowTypeNames.put(JetStandardClasses.getAnyType(), "ANY_TYPE_INFO");
        knowTypeNames.put(JetStandardClasses.getNullableAnyType(), "NULLABLE_ANY_TYPE_INFO");
        
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            knowTypeNames.put(standardLibrary.getPrimitiveJetType(primitiveType), jvmPrimitiveType.name() + "_TYPE_INFO");
            knowTypeNames.put(standardLibrary.getNullablePrimitiveJetType(primitiveType), "NULLABLE_" + jvmPrimitiveType.name() + "_TYPE_INFO");
            knowTypeNames.put(standardLibrary.getPrimitiveArrayJetType(primitiveType), jvmPrimitiveType.name() + "_ARRAY_TYPE_INFO");
            knowTypeNames.put(standardLibrary.getNullablePrimitiveArrayJetType(primitiveType), jvmPrimitiveType.name() + "_ARRAY_TYPE_INFO");
        }
        
        knowTypeNames.put(standardLibrary.getStringType(),"STRING_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableStringType(),"NULLABLE_STRING_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getTuple0Type(),"TUPLE0_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableTuple0Type(),"NULLABLE_TUPLE0_TYPE_INFO");
    }

    private void initKnownTypes() {
        knowTypes.put(JetStandardClasses.getNothingType(), TYPE_NOTHING);
        knowTypes.put(JetStandardClasses.getNullableNothingType(), TYPE_NOTHING);
        
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            knowTypes.put(standardLibrary.getPrimitiveJetType(primitiveType), jvmPrimitiveType.getAsmType());
            knowTypes.put(standardLibrary.getNullablePrimitiveJetType(primitiveType), jvmPrimitiveType.getWrapper().getAsmType());
        }

        knowTypes.put(standardLibrary.getNumberType(),JL_NUMBER_TYPE);
        knowTypes.put(standardLibrary.getStringType(),JL_STRING_TYPE);
        knowTypes.put(standardLibrary.getNullableStringType(),JL_STRING_TYPE);
        knowTypes.put(standardLibrary.getCharSequenceType(),JL_CHAR_SEQUENCE_TYPE);
        knowTypes.put(standardLibrary.getNullableCharSequenceType(),JL_CHAR_SEQUENCE_TYPE);
        knowTypes.put(standardLibrary.getThrowableType(), TYPE_THROWABLE);
        knowTypes.put(standardLibrary.getNullableThrowableType(), TYPE_THROWABLE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            knowTypes.put(standardLibrary.getPrimitiveArrayJetType(primitiveType), jvmPrimitiveType.getAsmArrayType());
            knowTypes.put(standardLibrary.getNullablePrimitiveArrayJetType(primitiveType), jvmPrimitiveType.getAsmArrayType());
        }
    }
    
    private boolean isForceReal(JvmClassName className) {
        return JvmPrimitiveType.getByWrapperClass(className) != null
                || className.getFqName().getFqName().equals("java.lang.String")
                || className.getFqName().getFqName().equals("java.lang.CharSequence")
                || className.getFqName().getFqName().equals("java.lang.Object");
    }

    public boolean isGenericsArray(JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if(declarationDescriptor instanceof TypeParameterDescriptor)
            return true;

        if(standardLibrary.getArray().equals(declarationDescriptor))
            return isGenericsArray(type.getArguments().get(0).getType());

        return false;
    }

    public JetType getGenericsElementType(JetType arrayType) {
        JetType type = arrayType.getArguments().get(0).getType();
        return isGenericsArray(type) ? type : null;
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if(descriptor instanceof PropertyDescriptor) {
            return StackValue.sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
        }
        else if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
            return Type.getObjectType(closureAnnotator.classNameForAnonymousClass((JetElement) psiElement));
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue.sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
        }
        else if (descriptor instanceof VariableDescriptor) {
            Boolean aBoolean = bindingContext.get(BindingContext.MUST_BE_WRAPPED_IN_A_REF, (VariableDescriptor) descriptor);
            if (aBoolean != null && aBoolean) {
                JetType outType = ((VariableDescriptor) descriptor).getType();
                return StackValue.sharedTypeForType(mapType(outType, MapTypeMode.VALUE));
            }
            else {
                return null;
            }
        }
        return null;
    }
}
