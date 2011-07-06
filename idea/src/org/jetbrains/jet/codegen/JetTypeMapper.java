package org.jetbrains.jet.codegen;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import jet.typeinfo.TypeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author yole
 */
public class JetTypeMapper {
    static final Type TYPE_OBJECT = Type.getObjectType("java/lang/Object");
    static final Type TYPE_TYPEINFO = Type.getType(TypeInfo.class);

    private final JetStandardLibrary standardLibrary;
    private final BindingContext bindingContext;
    private final Map<JetExpression, String> classNamesForAnonymousClasses = new HashMap<JetExpression, String>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

    public JetTypeMapper(JetStandardLibrary standardLibrary, BindingContext bindingContext) {
        this.standardLibrary = standardLibrary;
        this.bindingContext = bindingContext;
    }

    static String jvmName(PsiClass psiClass) {
        final String qName = psiClass.getQualifiedName();
        if (qName == null) {
            throw new UnsupportedOperationException("can't evaluate JVM name for anonymous class " + psiClass);
        }
        return qName.replace(".", "/");
    }

    static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    static Type psiTypeToAsm(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            if (type == PsiType.VOID) {
                return Type.VOID_TYPE;
            }
            if (type == PsiType.INT) {
                return Type.INT_TYPE;
            }
            if (type == PsiType.LONG) {
                return Type.LONG_TYPE;
            }
            if (type == PsiType.BOOLEAN) {
                return Type.BOOLEAN_TYPE;
            }
            if (type == PsiType.BYTE) {
                return Type.BYTE_TYPE;
            }
            if (type == PsiType.SHORT) {
                return Type.SHORT_TYPE;
            }
            if (type == PsiType.CHAR) {
                return Type.CHAR_TYPE;
            }
            if (type == PsiType.FLOAT) {
                return Type.FLOAT_TYPE;
            }
            if (type == PsiType.DOUBLE) {
                return Type.DOUBLE_TYPE;
            }
        }
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            if (psiClass instanceof PsiTypeParameter) {
                final PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
                if (extendsListTypes.length > 0) {
                    throw new UnsupportedOperationException("should return common supertype");
                }
                return TYPE_OBJECT;
            }
            if (psiClass == null) {
                throw new UnsupportedOperationException("unresolved PsiClassType: " + type);
            }
            return psiClassType(psiClass);
        }
        throw new UnsupportedOperationException("don't know how to map type " + type + " to ASM");
    }

    static Method getMethodDescriptor(PsiMethod method) {
        Type returnType = method.isConstructor() ? Type.VOID_TYPE : psiTypeToAsm(method.getReturnType());
        PsiParameter[] parameters = method.getParameterList().getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = psiTypeToAsm(parameters[i].getType());
        }
        return new Method(method.isConstructor() ? "<init>" : method.getName(), Type.getMethodDescriptor(returnType, parameterTypes));
    }

    static CallableMethod mapToCallableMethod(PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();
        String owner = jvmName(containingClass);
        Method signature = getMethodDescriptor(method);
        List<Type> valueParameterTypes = new ArrayList<Type>();
        Collections.addAll(valueParameterTypes, signature.getArgumentTypes());
        int opcode;
        boolean needsReceiver = false;
        if (method.isConstructor()) {
            opcode = Opcodes.INVOKESPECIAL;
        }
        else if (method.hasModifierProperty(PsiModifier.STATIC)) {
            opcode = Opcodes.INVOKESTATIC;
        }
        else {
            needsReceiver = true;
            if (containingClass.isInterface()) {
                opcode = Opcodes.INVOKEINTERFACE;
            }
            else {
                opcode = Opcodes.INVOKEVIRTUAL;
            }
        }
        final CallableMethod result = new CallableMethod(owner, signature, opcode, valueParameterTypes);
        if (needsReceiver) {
            result.setNeedsReceiver(null);
        }
        return result;
    }

    public String jvmName(ClassDescriptor jetClass, OwnerKind kind) {
        PsiElement declaration = bindingContext.getDeclarationPsiElement(jetClass);
        if (declaration instanceof PsiClass) {
            return jvmName((PsiClass) declaration);
        }
        if (declaration instanceof JetObjectDeclaration && ((JetObjectDeclaration) declaration).isObjectLiteral()) {
            String className = classNamesForAnonymousClasses.get(declaration);
            if (className == null) {
                throw new UnsupportedOperationException("Unexpected forward reference to anonymous class " + declaration);
            }
            return className;
        }
        return jetJvmName(jetClass, kind);
    }

    public boolean isInterface(ClassDescriptor jetClass, OwnerKind kind) {
        PsiElement declaration = bindingContext.getDeclarationPsiElement(jetClass);
        if (declaration instanceof JetObjectDeclaration && ((JetObjectDeclaration) declaration).isObjectLiteral()) {
            return false;
        }
        return kind == OwnerKind.INTERFACE;
    }

    private static String jetJvmName(ClassDescriptor jetClass, OwnerKind kind) {
        if (jetClass.isObject()) {
            return jvmNameForImplementation(jetClass);
        }
        if (kind == OwnerKind.INTERFACE) {
            return jvmNameForInterface(jetClass);
        }
        else if (kind == OwnerKind.IMPLEMENTATION) {
            return jvmNameForImplementation(jetClass);
        }
        else if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            return jvmNameForDelegatingImplementation(jetClass);
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

    static Type psiClassType(PsiClass psiClass) {
        return Type.getType("L" + jvmName(psiClass) + ";");
    }

    static Type jetInterfaceType(ClassDescriptor classDescriptor) {
        return Type.getType("L" + jvmNameForInterface(classDescriptor) + ";");
    }

    static Type jetImplementationType(ClassDescriptor classDescriptor) {
        return Type.getType("L" + jvmNameForImplementation(classDescriptor) + ";");
    }

    static Type jetDelegatingImplementationType(ClassDescriptor classDescriptor) {
        return Type.getType("L" + jvmNameForDelegatingImplementation(classDescriptor) + ";");
    }

    static String jvmName(JetNamespace namespace) {
        return NamespaceCodegen.getJVMClassName(namespace.getFQName());
    }

    static String jvmName(NamespaceDescriptor namespace) {
        return NamespaceCodegen.getJVMClassName(DescriptorRenderer.getFQName(namespace));
    }

    public static String jvmNameForInterface(ClassDescriptor descriptor) {
        return DescriptorRenderer.getFQName(descriptor).replace('.', '/');
    }

    public static String jvmNameForImplementation(ClassDescriptor descriptor) {
        return jvmNameForInterface(descriptor) + "$$Impl";
    }

    public static String jvmNameForDelegatingImplementation(ClassDescriptor descriptor) {
        return jvmNameForInterface(descriptor) + "$$DImpl";
    }

    public String getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        String owner;
        if (descriptor.getContainingDeclaration() instanceof NamespaceDescriptorImpl) {
            owner = jvmName((NamespaceDescriptor) descriptor.getContainingDeclaration());
        }
        else if (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor.getContainingDeclaration();
            if (kind instanceof OwnerKind.DelegateKind) {
                kind = OwnerKind.INTERFACE;
            }
            else if (classDescriptor.isObject()) {
                kind = OwnerKind.IMPLEMENTATION;
            }
            owner = jvmName(classDescriptor, kind);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + descriptor.getContainingDeclaration());
        }
        return owner;
    }

    public Type mapType(final JetType jetType) {
        return mapType(jetType, OwnerKind.INTERFACE);
    }

    public Type mapType(@NotNull final JetType jetType, OwnerKind kind) {
        if (jetType.equals(JetStandardClasses.getUnitType()) || jetType.equals(JetStandardClasses.getNothingType())) {
            return Type.VOID_TYPE;
        }
        if (jetType.equals(standardLibrary.getIntType())) {
            return Type.INT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getIntType()))) {
            return Type.getObjectType("java/lang/Integer");
        }
        if (jetType.equals(standardLibrary.getLongType())) {
            return Type.LONG_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getLongType()))) {
            return Type.getObjectType("java/lang/Long");
        }
        if (jetType.equals(standardLibrary.getShortType())) {
            return Type.SHORT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getShortType()))) {
            return Type.getObjectType("java/lang/Short");
        }
        if (jetType.equals(standardLibrary.getByteType())) {
            return Type.BYTE_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getByteType()))) {
            return Type.getObjectType("java/lang/Byte");
        }
        if (jetType.equals(standardLibrary.getCharType())) {
            return Type.CHAR_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getCharType()))) {
            return Type.getObjectType("java/lang/Char");
        }
        if (jetType.equals(standardLibrary.getFloatType())) {
            return Type.FLOAT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getFloatType()))) {
            return Type.getObjectType("java/lang/Float");
        }
        if (jetType.equals(standardLibrary.getDoubleType())) {
            return Type.DOUBLE_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getDoubleType()))) {
            return Type.getObjectType("java/lang/Double");
        }
        if (jetType.equals(standardLibrary.getBooleanType())) {
            return Type.BOOLEAN_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getBooleanType()))) {
            return Type.getObjectType("java/lang/Boolean");
        }
        if (jetType.equals(standardLibrary.getStringType()) || jetType.equals(standardLibrary.getNullableStringType())) {
            return Type.getType(String.class);
        }

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (standardLibrary.getArray().equals(descriptor)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberType = jetType.getArguments().get(0);
            Type elementType = mapType(memberType.getType());
            return Type.getType("[" + elementType.getDescriptor());
        }
        if (JetStandardClasses.getAny().equals(descriptor)) {
            return Type.getType(Object.class);
        }

        if (descriptor instanceof ClassDescriptor) {
            return Type.getObjectType(jvmName((ClassDescriptor) descriptor, kind));
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            return mapType(((TypeParameterDescriptor) descriptor).getBoundsAsType(), kind);
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    public Type boxType(Type asmType) {
        switch (asmType.getSort()) {
            case Type.VOID:
                return Type.getObjectType("java/lang/Void");
            case Type.BYTE:
                return Type.getObjectType("java/lang/Byte");
            case Type.BOOLEAN:
                return Type.getObjectType("java/lang/Boolean");
            case Type.SHORT:
                return Type.getObjectType("java/lang.Short");
            case Type.CHAR:
                return Type.getObjectType("java/lang/Character");
            case Type.INT:
                return Type.getObjectType("java/lang/Integer");
            case Type.FLOAT:
                return Type.getObjectType("java/lang/Float");
            case Type.LONG:
                return Type.getObjectType("java/lang/Long");
            case Type.DOUBLE:
                return Type.getObjectType("java/lang/Double");
        }

        return asmType;
    }


    private static Type getBoxedType(final Type type) {
        switch (type.getSort()) {
        }
        return type;
    }

    private Method mapSignature(JetNamedFunction f, List<Type> valueParameterTypes) {
        final JetTypeReference receiverTypeRef = f.getReceiverTypeRef();
        final JetType receiverType = receiverTypeRef == null ? null : bindingContext.resolveTypeReference(receiverTypeRef);
        final List<JetParameter> parameters = f.getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        if (receiverType != null) {
            parameterTypes.add(mapType(receiverType));
        }
        for (JetParameter parameter : parameters) {
            final Type type = mapType(bindingContext.resolveTypeReference(parameter.getTypeReference()));
            valueParameterTypes.add(type);
            parameterTypes.add(type);
        }
        for (JetTypeParameter p: f.getTypeParameters()) {
            parameterTypes.add(TYPE_TYPEINFO);
        }
        final JetTypeReference returnTypeRef = f.getReturnTypeRef();
        Type returnType;
        if (returnTypeRef == null) {
            final FunctionDescriptor functionDescriptor = bindingContext.getFunctionDescriptor(f);
            final JetType type = functionDescriptor.getReturnType();
            returnType = mapType(type);
        }
        else {
            returnType = mapType(bindingContext.resolveTypeReference(returnTypeRef));
        }
        return new Method(f.getName(), returnType, parameterTypes.toArray(new Type[parameterTypes.size()]));
    }

    public CallableMethod mapToCallableMethod(JetNamedFunction f) {
        final FunctionDescriptor functionDescriptor = bindingContext.getFunctionDescriptor(f);
        final DeclarationDescriptor functionParent = functionDescriptor.getContainingDeclaration();
        final List<Type> valueParameterTypes = new ArrayList<Type>();
        Method descriptor = mapSignature(f, valueParameterTypes);
        String owner;
        int invokeOpcode;
        boolean needsReceiver;
        ClassDescriptor receiverClass = null;
        if (functionParent instanceof NamespaceDescriptor) {
            owner = NamespaceCodegen.getJVMClassName(DescriptorRenderer.getFQName(functionParent));
            invokeOpcode = Opcodes.INVOKESTATIC;
            needsReceiver = f.getReceiverTypeRef() != null;
        }
        else if (functionParent instanceof ClassDescriptor) {
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            owner = jvmName(containingClass, OwnerKind.INTERFACE);
            invokeOpcode = isInterface(containingClass, OwnerKind.INTERFACE)
                    ? Opcodes.INVOKEINTERFACE
                    : Opcodes.INVOKEVIRTUAL;
            needsReceiver = true;
            receiverClass = containingClass;
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }

        final CallableMethod result = new CallableMethod(owner, descriptor, invokeOpcode, valueParameterTypes);
        result.setAcceptsTypeArguments(true);
        if (needsReceiver) {
            result.setNeedsReceiver(receiverClass);
        }
        return result;
    }

    public Method mapSignature(String name, FunctionDescriptor f) {
        final JetType receiverType = f.getReceiverType();
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        if (receiverType != null) {
            parameterTypes.add(mapType(receiverType));
        }
        for (ValueParameterDescriptor parameter : parameters) {
            parameterTypes.add(mapType(parameter.getOutType()));
        }
        Type returnType = mapType(f.getReturnType());
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

    private void appendTypeParameterSignature(StringBuffer answer, TypeParameterDescriptor p) {
        answer.append(p.getName()); // TODO: BOUND!
    }

    public Method mapGetterSignature(PropertyDescriptor descriptor) {
        Type returnType = mapType(descriptor.getOutType());
        return new Method(PropertyCodegen.getterName(descriptor.getName()), returnType, new Type[0]);
    }

    public Method mapSetterSignature(PropertyDescriptor descriptor) {
        final JetType inType = descriptor.getInType();
        if (inType == null) {
            return null;
        }
        Type paramType = mapType(inType);
        return new Method(PropertyCodegen.setterName(descriptor.getName()), Type.VOID_TYPE, new Type[] { paramType });
    }

    private Method mapConstructorSignature(ConstructorDescriptor descriptor, OwnerKind kind, List<Type> valueParameterTypes) {
        boolean delegate = kind == OwnerKind.DELEGATING_IMPLEMENTATION;
        List<ValueParameterDescriptor> parameters = descriptor.getOriginal().getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
        final DeclarationDescriptor outerDescriptor = classDescriptor.getContainingDeclaration();
        if (outerDescriptor instanceof ClassDescriptor) {
            parameterTypes.add(jvmType((ClassDescriptor) outerDescriptor, OwnerKind.IMPLEMENTATION));
        }
        if (delegate) {
            parameterTypes.add(jetInterfaceType(classDescriptor));
        }
        for (ValueParameterDescriptor parameter : parameters) {
            final Type type = mapType(parameter.getOutType());
            parameterTypes.add(type);
            valueParameterTypes.add(type);
        }

        List<TypeParameterDescriptor> typeParameters = classDescriptor.getTypeConstructor().getParameters();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            parameterTypes.add(TYPE_TYPEINFO);
        }

        return new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind) {
        List<Type> valueParameterTypes = new ArrayList<Type>();
        final Method method = mapConstructorSignature(descriptor, kind, valueParameterTypes);
        String owner = jvmName(descriptor.getContainingDeclaration(), kind);
        final CallableMethod result = new CallableMethod(owner, method, Opcodes.INVOKESPECIAL, valueParameterTypes);
        result.setAcceptsTypeArguments(true);
        return result;
    }

    static int getAccessModifiers(JetDeclaration p, int defaultFlags) {
        int flags = 0;
        if (p.hasModifier(JetTokens.PUBLIC_KEYWORD)) {
            flags |= Opcodes.ACC_PUBLIC;
        }
        else if (p.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            flags |= Opcodes.ACC_PRIVATE;
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

        JetNamedDeclaration container = PsiTreeUtil.getParentOfType(expression, JetNamespace.class, JetClass.class, JetObjectDeclaration.class);

        String baseName;
        if (container instanceof JetNamespace) {
            baseName = NamespaceCodegen.getJVMClassName(((JetNamespace) container).getFQName());
        }
        else {
            ClassDescriptor aClass = bindingContext.getClassDescriptor((JetClassOrObject) container);
            baseName = JetTypeMapper.jvmNameForInterface(aClass);
        }

        Integer count = anonymousSubclassesCount.get(baseName);
        if (count == null) count = 0;

        anonymousSubclassesCount.put(baseName, count + 1);

        final String className = baseName + "$" + (count + 1);
        classNamesForAnonymousClasses.put(expression, className);
        return className;
    }
}
