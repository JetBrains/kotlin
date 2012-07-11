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

import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.JetStandardLibraryNames;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.lang.types.ref.ClassName;
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
    public static final Type TUPLE0_TYPE = Type.getObjectType("jet/Tuple0");

    private JetStandardLibrary standardLibrary1;
    public BindingContext bindingContext;
    private ClosureAnnotator closureAnnotator;
    private boolean mapBuiltinsToJava;
    private ClassBuilderMode classBuilderMode;


    @Inject
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Inject
    public void setClosureAnnotator(ClosureAnnotator closureAnnotator) {
        this.closureAnnotator = closureAnnotator;
    }

    @Inject
    public void setBuiltinToJavaTypesMapping(BuiltinToJavaTypesMapping builtinToJavaTypesMapping) {
        mapBuiltinsToJava = builtinToJavaTypesMapping == BuiltinToJavaTypesMapping.ENABLED;
    }

    @Inject
    public void setClassBuilderMode(ClassBuilderMode classBuilderMode) {
        this.classBuilderMode = classBuilderMode;
    }

    @PostConstruct
    public void init() {
        initKnownTypes();
    }




    public boolean hasThis0(ClassDescriptor classDescriptor) {
        return closureAnnotator.hasThis0(classDescriptor);
    }

    public ClosureAnnotator getClosureAnnotator() {
        return closureAnnotator;
    }

    private static final class KnownTypeKey {
        @NotNull
        private final FqNameUnsafe className;
        private final boolean nullable;

        private KnownTypeKey(@NotNull FqNameUnsafe className, boolean nullable) {
            this.className = className;
            this.nullable = nullable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KnownTypeKey that = (KnownTypeKey) o;

            if (nullable != that.nullable) return false;
            if (!className.equals(that.className)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + (nullable ? 1 : 0);
            return result;
        }
    }

    private final HashMap<KnownTypeKey, Type> knowTypes = Maps.newHashMap();


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
            return closureAnnotator.classNameForScriptDescriptor((ScriptDescriptor) containingDeclaration);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
    }

    public static MapTypeMode ownerKindToMapTypeMode(OwnerKind kind) {
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

        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass != null) {
            return getClassFQName(containingClass).getInternalName() + "$" + local;
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
    @NotNull
    private String getFQName(@NotNull DeclarationDescriptor descriptor) {
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
            if (klass.getKind() == ClassKind.OBJECT) {
                if (klass.getContainingDeclaration() instanceof ClassDescriptor) {
                    ClassDescriptor containingKlass = (ClassDescriptor) klass.getContainingDeclaration();
                    if (containingKlass.getKind() == ClassKind.ENUM_CLASS) {
                        return getFQName(containingKlass);
                    }
                }
            }
            else if (klass.getKind() == ClassKind.ENUM_ENTRY) {
                return getFQName(klass.getContainingDeclaration());
            }
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();

        if (container == null) {
            throw new IllegalStateException("descriptor has no container: " + descriptor);
        }

        Name name = descriptor.getName();

        if (descriptor instanceof ClassDescriptor && name.isSpecial()) {
            ClassDescriptor clazz = (ClassDescriptor) descriptor;
            JvmClassName className = closureAnnotator.classNameForClassDescriptor(clazz);
            return className.getInternalName();
        }

        String baseName = getFQName(container);
        if (!baseName.isEmpty()) {
            return baseName + (container instanceof NamespaceDescriptor ? "/" : "$") + name.getIdentifier();
        }

        return name.getIdentifier();
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
        Type known = null;
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();

        if (mapBuiltinsToJava) {
            if (classifier instanceof ClassDescriptor) {
                KnownTypeKey key = new KnownTypeKey(DescriptorUtils.getFQName(classifier), jetType.isNullable());
                known = knowTypes.get(key);
            }
        }

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
                if (mapBuiltinsToJava) {
                    // TODO: enable and fix tests
                    //throw new IllegalStateException("must not map known type to IMPL when not compiling builtins: " + jetType);
                }
                // fall through
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
                visitAsmType(signatureVisitor, asmType, true);
            }
            checkValidType(asmType);
            return asmType;
        }

        if (descriptor instanceof ClassDescriptor
                && JetStandardLibraryNames.ARRAY.is((ClassDescriptor) descriptor)
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

            if (JetStandardLibraryNames.COMPARABLE.is((ClassDescriptor) descriptor) && mapBuiltinsToJava) {
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
        if (!mapBuiltinsToJava) {
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
        if (functionDescriptor == null) { return null; }

        final DeclarationDescriptor functionParent = functionDescriptor.getOriginal().getContainingDeclaration();

        while(functionDescriptor.getKind()==CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
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
            thisClass = owner = ownerForDefaultParam = ownerForDefaultImpl = closureAnnotator.classNameForScriptDescriptor((ScriptDescriptor) functionParent);
            invokeOpcode = INVOKEVIRTUAL;
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
            owner = JvmClassName.byType(type);
            ownerForDefaultParam = JvmClassName.byType(mapType(declarationOwner.getDefaultType(), MapTypeMode.TYPE_PARAMETER));
            ownerForDefaultImpl = JvmClassName.byInternalName(
                    ownerForDefaultParam.getInternalName() + (originalIsInterface ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));

            invokeOpcode = isInterface
                    ? (superCall ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE)
                    : (superCall ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL);
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

        if (kind == OwnerKind.TRAIT_IMPL) {
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
        signatureVisitor.writeFormalTypeParameter(typeParameterDescriptor.getName().getName(), typeParameterDescriptor.getVariance(), typeParameterDescriptor.isReified());

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

        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) parentDescriptor;
            assert containingDeclaration != null;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.IMPL);
            signatureWriter.writeParameterTypeEnd();
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
        if (kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter, MapTypeMode.VALUE);
            signatureWriter.writeParameterTypeEnd();
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

    @NotNull
    public JvmMethodSignature mapScriptSignature(@NotNull ScriptDescriptor script, @NotNull List<ScriptDescriptor> importedScripts) {
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        writeFormalTypeParameters(Collections.<TypeParameterDescriptor>emptyList(), signatureWriter);

        signatureWriter.writeParametersStart();

        for (ScriptDescriptor importedScript : importedScripts) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(closureAnnotator.classDescriptorForScrpitDescriptor(importedScript).getDefaultType(), signatureWriter, MapTypeMode.VALUE);
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

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind, boolean hasThis0) {
        final JvmMethodSignature method = mapConstructorSignature(descriptor, hasThis0);
        MapTypeMode mapTypeMode = ownerKindToMapTypeMode(kind);
        JetType defaultType = descriptor.getContainingDeclaration().getDefaultType();
        Type mapped = mapType(defaultType, mapTypeMode);
        if (mapped.getSort() != Type.OBJECT) {
            throw new IllegalStateException("type must have been mapped to object: " + defaultType + ", actual: " + mapped);
        }
        JvmClassName owner = JvmClassName.byType(mapped);
        return new CallableMethod(owner, owner, owner, method, INVOKESPECIAL, null, null, null);
    }


    public static int getAccessModifiers(@NotNull MemberDescriptor p, int defaultFlags) {
        DeclarationDescriptor declaration = p.getContainingDeclaration();
        if (CodegenUtil.isInterface(declaration)) {
            return ACC_PUBLIC;
        }
        if (p.getVisibility() == Visibilities.PUBLIC) {
            return ACC_PUBLIC;
        }
        else if (p.getVisibility() == Visibilities.PROTECTED) {
            return ACC_PROTECTED;
        }
        else if (p.getVisibility() == Visibilities.PRIVATE) {
            if (DescriptorUtils.isClassObject(declaration)) {
                return defaultFlags;
            }
            if (p.getContainingDeclaration() instanceof NamespaceDescriptor) {
                return ACC_PUBLIC;
            }
            return ACC_PRIVATE;
        }
        else if (p.getVisibility() == Visibilities.INTERNAL) {
            return ACC_PUBLIC;
        }
        else {
            if (p.getVisibility() == Visibilities.INHERITED) {
                throw new IllegalStateException("'inherited' visibility is unresolved on code generation stage");
            }
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

    private void registerKnownType(@NotNull ClassName className, @NotNull Type nonNullType, @NotNull Type nullableType) {
        knowTypes.put(new KnownTypeKey(className.getFqName().toUnsafe(), true), nullableType);
        knowTypes.put(new KnownTypeKey(className.getFqName().toUnsafe(), false), nonNullType);
    }

    private void initKnownTypes() {
        registerKnownType(JetStandardLibraryNames.NOTHING, TYPE_NOTHING, TYPE_NOTHING);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            registerKnownType(primitiveType.getClassName(), jvmPrimitiveType.getAsmType(), jvmPrimitiveType.getWrapper().getAsmType());
        }

        registerKnownType(JetStandardLibraryNames.NUMBER, JL_NUMBER_TYPE, JL_NUMBER_TYPE);
        registerKnownType(JetStandardLibraryNames.STRING, JL_STRING_TYPE, JL_STRING_TYPE);
        registerKnownType(JetStandardLibraryNames.CHAR_SEQUENCE, JL_CHAR_SEQUENCE_TYPE, JL_CHAR_SEQUENCE_TYPE);
        registerKnownType(JetStandardLibraryNames.THROWABLE, TYPE_THROWABLE, TYPE_THROWABLE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            registerKnownType(primitiveType.getArrayClassName(), jvmPrimitiveType.getAsmArrayType(), jvmPrimitiveType.getAsmArrayType());
        }
    }
    
    private boolean isForceReal(JvmClassName className) {
        return JvmPrimitiveType.getByWrapperClass(className) != null
                || className.getFqName().getFqName().equals("java.lang.String")
                || className.getFqName().getFqName().equals("java.lang.CharSequence")
                || className.getFqName().getFqName().equals("java.lang.Object")
                || className.getFqName().getFqName().equals("java.lang.Number");
    }

    public boolean isGenericsArray(JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            return true;
        }

        if (JetStandardLibraryNames.ARRAY.is(type)) {
            return isGenericsArray(type.getArguments().get(0).getType());
        }

        return false;
    }

    public JetType getGenericsElementType(JetType arrayType) {
        JetType type = arrayType.getArguments().get(0).getType();
        return isGenericsArray(type) ? type : null;
    }

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return StackValue.sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
        }
        else if (descriptor instanceof SimpleFunctionDescriptor && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
            PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
            return closureAnnotator.classNameForAnonymousClass((JetElement) psiElement).getAsmType();
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue.sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType(), MapTypeMode.VALUE));
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
        Boolean aBoolean = bindingContext.get(BindingContext.CAPTURED_IN_CLOSURE, variableDescriptor);
        return aBoolean != null && aBoolean && variableDescriptor.isVar();
    }
}
