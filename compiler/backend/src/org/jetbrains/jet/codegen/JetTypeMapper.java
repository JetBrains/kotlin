package org.jetbrains.jet.codegen;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import jet.JetObject;
import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author yole
 * @author alex.tkachman
 */
public class JetTypeMapper {
    public static final Type TYPE_OBJECT = Type.getObjectType("java/lang/Object");
    public static final Type TYPE_TYPEINFO = Type.getType(TypeInfo.class);
    public static final Type TYPE_TYPEINFOPROJECTION = Type.getType(TypeInfoProjection.class);
    public static final Type TYPE_JET_OBJECT = Type.getType(JetObject.class);
    public static final Type TYPE_NOTHING = Type.getObjectType("jet/Nothing");
    public static final Type JL_INTEGER_TYPE = Type.getObjectType("java/lang/Integer");
    public static final Type JL_LONG_TYPE = Type.getObjectType("java/lang/Long");
    public static final Type JL_SHORT_TYPE = Type.getObjectType("java/lang/Short");
    public static final Type JL_BYTE_TYPE = Type.getObjectType("java/lang/Byte");
    public static final Type JL_CHAR_TYPE = Type.getObjectType("java/lang/Character");
    public static final Type JL_FLOAT_TYPE = Type.getObjectType("java/lang/Float");
    public static final Type JL_DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
    public static final Type JL_BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");
    public static final Type JL_NUMBER_TYPE = Type.getObjectType("java/lang/Number");
    public static final Type JL_STRING_BUILDER = Type.getObjectType("java/lang/StringBuilder");
    public static final Type JL_STRING_TYPE = Type.getObjectType("java/lang/String");

    public static final Type ARRAY_INT_TYPE = Type.getType(int[].class);
    public static final Type ARRAY_LONG_TYPE = Type.getType(long[].class);
    public static final Type ARRAY_SHORT_TYPE = Type.getType(short[].class);
    public static final Type ARRAY_BYTE_TYPE = Type.getType(byte[].class);
    public static final Type ARRAY_CHAR_TYPE = Type.getType(char[].class);
    public static final Type ARRAY_FLOAT_TYPE = Type.getType(float[].class);
    public static final Type ARRAY_DOUBLE_TYPE = Type.getType(double[].class);
    public static final Type ARRAY_BOOL_TYPE = Type.getType(boolean[].class);
    public static final Type ARRAY_GENERIC_TYPE = Type.getType(Object[].class);

    private final JetStandardLibrary standardLibrary;
    private final BindingContext bindingContext;
    private final Map<JetExpression, String> classNamesForAnonymousClasses = new HashMap<JetExpression, String>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

    private final HashMap<JetType,String> knowTypeNames = new HashMap<JetType, String>();
    private final HashMap<JetType,Type> knowTypes = new HashMap<JetType, Type>();

    public static final Type TYPE_FUNCTION1 = Type.getObjectType("jet/Function1");
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

    public JetTypeMapper(JetStandardLibrary standardLibrary, BindingContext bindingContext) {
        this.standardLibrary = standardLibrary;
        this.bindingContext = bindingContext;
        initKnownTypes();
        initKnownTypeNames();
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type == Type.INT_TYPE
                || type == Type.SHORT_TYPE
                || type == Type.BYTE_TYPE
                || type == Type.CHAR_TYPE
                || type == Type.SHORT_TYPE
                || type == Type.FLOAT_TYPE
                || type == Type.DOUBLE_TYPE
                || type == Type.LONG_TYPE
                || type == Type.BOOLEAN_TYPE
                || type == Type.VOID_TYPE;
    }

    public static Type getBoxedType(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
                return JL_BYTE_TYPE;
            case Type.BOOLEAN:
                return JL_BOOLEAN_TYPE;
            case Type.SHORT:
                return JL_SHORT_TYPE;
            case Type.CHAR:
                return JL_CHAR_TYPE;
            case Type.INT:
                return JL_INTEGER_TYPE;
            case Type.FLOAT:
                return JL_FLOAT_TYPE;
            case Type.LONG:
                return JL_LONG_TYPE;
            case Type.DOUBLE:
                return JL_DOUBLE_TYPE;
        }
        return type;
    }

    public String jvmName(ClassDescriptor jetClass, OwnerKind kind) {
        PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, jetClass);
        if (declaration instanceof JetObjectDeclaration && ((JetObjectDeclaration) declaration).isObjectLiteral()) {
            final PsiElement parent = declaration.getParent();
            if (parent instanceof JetClassObject) {
                JetClass containingClass = PsiTreeUtil.getParentOfType(parent, JetClass.class);
                final ClassDescriptor containingClassDescriptor = bindingContext.get(BindingContext.CLASS, containingClass);
                return jvmName(containingClassDescriptor, OwnerKind.IMPLEMENTATION) + "$$ClassObj";
            }
            @SuppressWarnings("SuspiciousMethodCalls") String className = classNamesForAnonymousClasses.get(declaration);
            if (className == null) {
                throw new UnsupportedOperationException("Unexpected forward reference to anonymous class " + declaration);
            }
            return className;
        }
        return jetJvmName(jetClass, kind);
    }

    public String jvmName(JetClassObject classObject) {
        final ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
        return jvmName(descriptor, OwnerKind.IMPLEMENTATION);
    }

    private String jetJvmName(ClassDescriptor jetClass, OwnerKind kind) {
        if (jetClass.getKind() == ClassKind.OBJECT) {
            return jvmNameForImplementation(jetClass, OwnerKind.IMPLEMENTATION);
        }
        else if (kind == OwnerKind.IMPLEMENTATION) {
            return jvmNameForImplementation(jetClass, OwnerKind.IMPLEMENTATION);
        }
        else if (kind == OwnerKind.TRAIT_IMPL) {
            return jvmNameForImplementation(jetClass, OwnerKind.IMPLEMENTATION) + "$$TImpl";
        }
        else {
            assert false : "Unsuitable kind";
            return "java/lang/Object";
        }
    }

    public Type jvmType(ClassDescriptor jetClass, OwnerKind kind) {
        if (jetClass == standardLibrary.getString()) {
            return Type.getType(String.class);
        }
        return Type.getType("L" + jvmName(jetClass, kind) + ";");
    }

    Type jetImplementationType(ClassDescriptor classDescriptor) {
        return Type.getType("L" + jvmNameForImplementation(classDescriptor, OwnerKind.IMPLEMENTATION) + ";");
    }

    public static String jvmName(JetNamespace namespace) {
        return NamespaceCodegen.getJVMClassName(CodegenUtil.getFQName(namespace));
    }

    static String jvmName(NamespaceDescriptor namespace) {
        return NamespaceCodegen.getJVMClassName(DescriptorRenderer.getFQName(namespace));
    }

    public String jvmNameForImplementation(ClassDescriptor descriptor, OwnerKind kind) {
        return mapType(descriptor.getDefaultType(), kind).getInternalName();
    }

    public String getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        String owner;
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            owner = jvmName((NamespaceDescriptor) containingDeclaration);
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            if (kind instanceof OwnerKind.DelegateKind) {
                kind = OwnerKind.IMPLEMENTATION;
            }
            else {
                if (classDescriptor.getKind() == ClassKind.OBJECT) {
                    kind = OwnerKind.IMPLEMENTATION;
                }
            }
            owner = jvmName(classDescriptor, kind);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
        return owner;
    }

    @NotNull public Type mapReturnType(final JetType jetType) {
        if (jetType.equals(JetStandardClasses.getUnitType()) || jetType.equals(JetStandardClasses.getNothingType())) {
            return Type.VOID_TYPE;
        }
        if (jetType.equals(JetStandardClasses.getNullableNothingType())) {
            return TYPE_OBJECT;
        }
        return mapType(jetType, OwnerKind.IMPLEMENTATION);
    }

    @NotNull public Type mapType(final JetType jetType) {
        return mapType(jetType, OwnerKind.IMPLEMENTATION);
    }

    @NotNull public Type mapType(@NotNull final JetType jetType, OwnerKind kind) {
        Type known = knowTypes.get(jetType);
        if(known != null)
            return known;

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (standardLibrary.getArray().equals(descriptor)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            JetType memberType = jetType.getArguments().get(0).getType();
            if(!isGenericsArray(jetType))
                return Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
            else
                return ARRAY_GENERIC_TYPE;
        }
        if (JetStandardClasses.getAny().equals(descriptor)) {
            return Type.getType(Object.class);
        }

        if (descriptor instanceof ClassDescriptor) {
            String name = DescriptorRenderer.getFQName(descriptor).replace('.', '/');
            if(name.startsWith("<java_root>"))
                name = name.substring("<java_root>".length()+1);
            return Type.getObjectType(name + (kind == OwnerKind.TRAIT_IMPL ? "$$TImpl" : ""));
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            return mapType(((TypeParameterDescriptor) descriptor).getBoundsAsType(), kind);
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    public static Type unboxType(final Type type) {
        if (type == JL_INTEGER_TYPE) {
            return Type.INT_TYPE;
        }
        else if (type == JL_BOOLEAN_TYPE) {
            return Type.BOOLEAN_TYPE;
        }
        else if (type == JL_CHAR_TYPE) {
            return Type.CHAR_TYPE;
        }
        else if (type == JL_SHORT_TYPE) {
            return Type.SHORT_TYPE;
        }
        else if (type == JL_LONG_TYPE) {
            return Type.LONG_TYPE;
        }
        else if (type == JL_BYTE_TYPE) {
            return Type.BYTE_TYPE;
        }
        else if (type == JL_FLOAT_TYPE) {
            return Type.FLOAT_TYPE;
        }
        else if (type == JL_DOUBLE_TYPE) {
            return Type.DOUBLE_TYPE;
        }
        throw new UnsupportedOperationException("Unboxing: " + type);
    }

    public static Type boxType(Type asmType) {
        switch (asmType.getSort()) {
            case Type.VOID:
                return Type.VOID_TYPE;
            case Type.BYTE:
                return JL_BYTE_TYPE;
            case Type.BOOLEAN:
                return JL_BOOLEAN_TYPE;
            case Type.SHORT:
                return JL_SHORT_TYPE;
            case Type.CHAR:
                return JL_CHAR_TYPE;
            case Type.INT:
                return JL_INTEGER_TYPE;
            case Type.FLOAT:
                return JL_FLOAT_TYPE;
            case Type.LONG:
                return JL_LONG_TYPE;
            case Type.DOUBLE:
                return JL_DOUBLE_TYPE;
        }

        return asmType;
    }


    public CallableMethod mapToCallableMethod(FunctionDescriptor functionDescriptor, boolean superCall, OwnerKind kind) {
        if(functionDescriptor == null)
            return null;

        final DeclarationDescriptor functionParent = functionDescriptor.getContainingDeclaration();
        final List<Type> valueParameterTypes = new ArrayList<Type>();
        Method descriptor = mapSignature(functionDescriptor.getOriginal(), valueParameterTypes, kind);
        String owner;
        int invokeOpcode;
        ClassDescriptor thisClass;
        if (functionParent instanceof NamespaceDescriptor) {
            assert !superCall;
            owner = NamespaceCodegen.getJVMClassName(DescriptorRenderer.getFQName(functionParent));
            invokeOpcode = INVOKESTATIC;
            thisClass = null;
        }
        else if (functionDescriptor instanceof ConstructorDescriptor) {
            assert !superCall;
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            owner = jvmName(containingClass, OwnerKind.IMPLEMENTATION);
            invokeOpcode = INVOKESPECIAL;
            thisClass = null;
        }
        else if (functionParent instanceof ClassDescriptor) {
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            boolean isInterface = CodegenUtil.isInterface(containingClass);
            owner = jvmName(containingClass, isInterface && superCall ? OwnerKind.TRAIT_IMPL : OwnerKind.IMPLEMENTATION);
            invokeOpcode = isInterface
                    ? (superCall ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE)
                    : (superCall ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL);
            if(isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor.getOriginal(), valueParameterTypes, OwnerKind.TRAIT_IMPL);
            }
            thisClass = containingClass;
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }

        final CallableMethod result = new CallableMethod(owner, descriptor, invokeOpcode, valueParameterTypes);
        result.setNeedsThis(thisClass);
        if(functionDescriptor.getReceiverParameter().exists()) {
            result.setNeedsReceiver(functionDescriptor.getReceiverParameter().getType().getConstructor().getDeclarationDescriptor());
        }

        return result;
    }

    private Method mapSignature(FunctionDescriptor f, List<Type> valueParameterTypes, OwnerKind kind) {
        final ReceiverDescriptor receiverTypeRef = f.getReceiverParameter();
        final JetType receiverType = !receiverTypeRef.exists() ? null : receiverTypeRef.getType();
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        if (receiverType != null) {
            parameterTypes.add(mapType(receiverType));
        }
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) f.getContainingDeclaration();
            JetType jetType = TraitImplBodyCodegen.getSuperClass(containingDeclaration, bindingContext);
            Type type = mapType(jetType);
            if(type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = mapType(jetType);
            }
            valueParameterTypes.add(type);
            parameterTypes.add(type);
        }
        for (ValueParameterDescriptor parameter : parameters) {
            Type type = mapType(parameter.getOutType());
            valueParameterTypes.add(type);
            parameterTypes.add(type);
        }
        for (TypeParameterDescriptor parameterDescriptor : f.getTypeParameters()) {
            if(parameterDescriptor.isReified()) {
                parameterTypes.add(TYPE_TYPEINFO);
            }
        }
        Type returnType = f instanceof ConstructorDescriptor ? Type.VOID_TYPE : mapReturnType(f.getReturnType());
        return new Method(f.getName(), returnType, parameterTypes.toArray(new Type[parameterTypes.size()]));
    }

    public Method mapSignature(String name, FunctionDescriptor f) {
        final ReceiverDescriptor receiver = f.getReceiverParameter();
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        if (receiver.exists()) {
            parameterTypes.add(mapType(receiver.getType()));
        }
        for (ValueParameterDescriptor parameter : parameters) {
            parameterTypes.add(mapType(parameter.getOutType()));
        }
        Type returnType = mapReturnType(f.getReturnType());
        return new Method(name, returnType, parameterTypes.toArray(new Type[parameterTypes.size()]));
    }

    public String genericSignature(FunctionDescriptor f) {
        StringBuffer answer = new StringBuffer();
        final List<TypeParameterDescriptor> typeParameters = f.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            answer.append('<');
            for (TypeParameterDescriptor p : typeParameters) {
                appendTypeParameterSignature(answer, p);
            }
            answer.append('>');
        }

        answer.append('(');
        for (ValueParameterDescriptor p : f.getValueParameters()) {
            appendType(answer, p.getOutType());
        }
        answer.append(')');

        appendType(answer, f.getReturnType());

        return answer.toString();
    }

    private void appendType(StringBuffer answer, JetType type) {
        answer.append(mapType(type).getDescriptor()); // TODO: type parameter references!
    }

    private static void appendTypeParameterSignature(StringBuffer answer, TypeParameterDescriptor p) {
        answer.append(p.getName()); // TODO: BOUND!
    }

    public Method mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        Type returnType = mapType(descriptor.getOutType());
        if(kind != OwnerKind.TRAIT_IMPL)
            return new Method(PropertyCodegen.getterName(descriptor.getName()), returnType, new Type[0]);
        else {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            return new Method(PropertyCodegen.getterName(descriptor.getName()), returnType, new Type[] { mapType(containingDeclaration.getDefaultType()) });
        }
    }

    public Method mapSetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        final JetType inType = descriptor.getInType();
        if (inType == null) {
            return null;
        }
        Type paramType = mapType(inType);
        if(kind != OwnerKind.TRAIT_IMPL) {
            return new Method(PropertyCodegen.setterName(descriptor.getName()), Type.VOID_TYPE, new Type[] { paramType });
        }
        else {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            return new Method(PropertyCodegen.setterName(descriptor.getName()), Type.VOID_TYPE, new Type[] { mapType(containingDeclaration.getDefaultType()), paramType });
        }
    }

    private Method mapConstructorSignature(ConstructorDescriptor descriptor, List<Type> valueParameterTypes) {
        List<ValueParameterDescriptor> parameters = descriptor.getOriginal().getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
        if (CodegenUtil.hasThis0(classDescriptor)) {
            parameterTypes.add(jvmType(CodegenUtil.getOuterClassDescriptor(classDescriptor), OwnerKind.IMPLEMENTATION));
        }
        for (ValueParameterDescriptor parameter : parameters) {
            final Type type = mapType(parameter.getOutType());
            parameterTypes.add(type);
            valueParameterTypes.add(type);
        }

        PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
        if(!(psiElement instanceof PsiClass)) {
            List<TypeParameterDescriptor> typeParameters = classDescriptor.getTypeConstructor().getParameters();
            for (int n = typeParameters.size(); n > 0; n--) {
                parameterTypes.add(TYPE_TYPEINFO);
            }
        }

        return new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind) {
        List<Type> valueParameterTypes = new ArrayList<Type>();
        final Method method = mapConstructorSignature(descriptor, valueParameterTypes);
        String owner = jvmName(descriptor.getContainingDeclaration(), kind);
        return new CallableMethod(owner, method, INVOKESPECIAL, valueParameterTypes);
    }

    static int getAccessModifiers(JetDeclaration p, int defaultFlags) {
        int flags = 0;
        if (p.hasModifier(JetTokens.PUBLIC_KEYWORD)) {
            flags |= ACC_PUBLIC;
        }
        else if (p.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            flags |= ACC_PRIVATE;
        }
        else {
            flags |= defaultFlags;
        }
        return flags;
    }

    String classNameForAnonymousClass(JetExpression expression) {
        String name = classNamesForAnonymousClasses.get(expression);
        if (name != null) {
            return name;
        }

        @SuppressWarnings("unchecked") JetNamedDeclaration container = PsiTreeUtil.getParentOfType(expression, JetNamespace.class, JetClass.class, JetObjectDeclaration.class);

        String baseName;
        if (container instanceof JetNamespace) {
            baseName = NamespaceCodegen.getJVMClassName(CodegenUtil.getFQName(((JetNamespace) container)));
        }
        else {
            ClassDescriptor aClass = bindingContext.get(BindingContext.CLASS, container);
            baseName = jvmNameForImplementation(aClass, OwnerKind.IMPLEMENTATION);
        }

        Integer count = anonymousSubclassesCount.get(baseName);
        if (count == null) count = 0;

        anonymousSubclassesCount.put(baseName, count + 1);

        final String className = baseName + "$" + (count + 1);
        classNamesForAnonymousClasses.put(expression, className);
        return className;
    }

    public Collection<String> allJvmNames(JetClassOrObject jetClass) {
        Set<String> result = new HashSet<String>();
        final ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, jetClass);
        if (classDescriptor != null) {
            result.add(jvmName(classDescriptor, OwnerKind.IMPLEMENTATION));
        }
        return result;
    }

    private void initKnownTypeNames() {
        knowTypeNames.put(standardLibrary.getIntType(), "INT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableIntType(), "NULLABLE_INT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getLongType(), "LONG_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableLongType(), "NULLABLE_LONG_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getShortType(),"SHORT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableShortType(),"NULLABLE_SHORT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getByteType(),"BYTE_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableByteType(),"NULLABLE_BYTE_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getCharType(),"CHAR_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableCharType(),"NULLABLE_CHAR_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getFloatType(),"FLOAT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableFloatType(),"NULLABLE_FLOAT_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getDoubleType(),"DOUBLE_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableDoubleType(),"NULLABLE_DOUBLE_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getBooleanType(),"BOOLEAN_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableBooleanType(),"NULLABLE_BOOLEAN_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getStringType(),"STRING_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableStringType(),"NULLABLE_STRING_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getTuple0Type(),"TUPLE0_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableTuple0Type(),"NULLABLE_TUPLE0_TYPE_INFO");

        knowTypeNames.put(standardLibrary.getIntArrayType(), "INT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getLongArrayType(), "LONG_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getShortArrayType(),"SHORT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getByteArrayType(),"BYTE_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getCharArrayType(),"CHAR_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getFloatArrayType(),"FLOAT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getDoubleArrayType(),"DOUBLE_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getBooleanArrayType(),"BOOLEAN_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableIntArrayType(), "INT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableLongArrayType(), "LONG_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableShortArrayType(),"SHORT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableByteArrayType(),"BYTE_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableCharArrayType(),"CHAR_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableFloatArrayType(),"FLOAT_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableDoubleArrayType(),"DOUBLE_ARRAY_TYPE_INFO");
        knowTypeNames.put(standardLibrary.getNullableBooleanArrayType(),"BOOLEAN_ARRAY_TYPE_INFO");
    }

    private void initKnownTypes() {
        knowTypes.put(JetStandardClasses.getNothingType(), TYPE_NOTHING);
        knowTypes.put(JetStandardClasses.getNullableNothingType(), TYPE_NOTHING);

        knowTypes.put(standardLibrary.getIntType(), Type.INT_TYPE);
        knowTypes.put(standardLibrary.getNullableIntType(), JL_INTEGER_TYPE);
        knowTypes.put(standardLibrary.getLongType(), Type.LONG_TYPE);
        knowTypes.put(standardLibrary.getNullableLongType(), JL_LONG_TYPE);
        knowTypes.put(standardLibrary.getShortType(),Type.SHORT_TYPE);
        knowTypes.put(standardLibrary.getNullableShortType(),JL_SHORT_TYPE);
        knowTypes.put(standardLibrary.getByteType(),Type.BYTE_TYPE);
        knowTypes.put(standardLibrary.getNullableByteType(),JL_BYTE_TYPE);
        knowTypes.put(standardLibrary.getCharType(),Type.CHAR_TYPE);
        knowTypes.put(standardLibrary.getNullableCharType(),JL_CHAR_TYPE);
        knowTypes.put(standardLibrary.getFloatType(),Type.FLOAT_TYPE);
        knowTypes.put(standardLibrary.getNullableFloatType(),JL_FLOAT_TYPE);
        knowTypes.put(standardLibrary.getDoubleType(),Type.DOUBLE_TYPE);
        knowTypes.put(standardLibrary.getNullableDoubleType(),JL_DOUBLE_TYPE);
        knowTypes.put(standardLibrary.getBooleanType(),Type.BOOLEAN_TYPE);
        knowTypes.put(standardLibrary.getNullableBooleanType(),JL_BOOLEAN_TYPE);

        knowTypes.put(standardLibrary.getStringType(),JL_STRING_TYPE);
        knowTypes.put(standardLibrary.getNullableStringType(),JL_STRING_TYPE);

        knowTypes.put(standardLibrary.getIntArrayType(), ARRAY_INT_TYPE);
        knowTypes.put(standardLibrary.getLongArrayType(), ARRAY_LONG_TYPE);
        knowTypes.put(standardLibrary.getShortArrayType(),ARRAY_SHORT_TYPE);
        knowTypes.put(standardLibrary.getByteArrayType(),ARRAY_BYTE_TYPE);
        knowTypes.put(standardLibrary.getCharArrayType(),ARRAY_CHAR_TYPE);
        knowTypes.put(standardLibrary.getFloatArrayType(),ARRAY_FLOAT_TYPE);
        knowTypes.put(standardLibrary.getDoubleArrayType(),ARRAY_DOUBLE_TYPE);
        knowTypes.put(standardLibrary.getBooleanArrayType(),ARRAY_BOOL_TYPE);
        knowTypes.put(standardLibrary.getNullableIntArrayType(), ARRAY_INT_TYPE);
        knowTypes.put(standardLibrary.getNullableLongArrayType(), ARRAY_LONG_TYPE);
        knowTypes.put(standardLibrary.getNullableShortArrayType(),ARRAY_SHORT_TYPE);
        knowTypes.put(standardLibrary.getNullableByteArrayType(),ARRAY_BYTE_TYPE);
        knowTypes.put(standardLibrary.getNullableCharArrayType(),ARRAY_CHAR_TYPE);
        knowTypes.put(standardLibrary.getNullableFloatArrayType(),ARRAY_FLOAT_TYPE);
        knowTypes.put(standardLibrary.getNullableDoubleArrayType(),ARRAY_DOUBLE_TYPE);
        knowTypes.put(standardLibrary.getNullableBooleanArrayType(),ARRAY_BOOL_TYPE);
    }

    public String isKnownTypeInfo(JetType jetType) {
        return knowTypeNames.get(jetType);
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
}
