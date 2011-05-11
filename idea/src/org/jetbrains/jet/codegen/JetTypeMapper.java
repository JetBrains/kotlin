package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 */
public class JetTypeMapper {
    static final Type TYPE_OBJECT = Type.getObjectType("java/lang/Object");

    private final JetStandardLibrary standardLibrary;
    private final BindingContext bindingContext;

    public JetTypeMapper(JetStandardLibrary standardLibrary, BindingContext bindingContext) {
        this.standardLibrary = standardLibrary;
        this.bindingContext = bindingContext;
    }

    static String jvmName(PsiClass psiClass) {
        return psiClass.getQualifiedName().replace(".", "/");
    }

    static String jvmName(ClassDescriptor jetClass, OwnerKind kind) {
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
        return NamespaceCodegen.getJVMClassName(DescriptorUtil.getFQName(namespace));
    }

    public static String jvmNameForInterface(ClassDescriptor descriptor) {
        return DescriptorUtil.getFQName(descriptor).replace('.', '/');
    }

    public static String jvmNameForImplementation(ClassDescriptor descriptor) {
        return jvmNameForInterface(descriptor) + "$$Impl";
    }

    public static String jvmNameForDelegatingImplementation(ClassDescriptor descriptor) {
        return jvmNameForInterface(descriptor) + "$$DImpl";
    }

    static String getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        String owner;
        if (descriptor.getContainingDeclaration() instanceof NamespaceDescriptorImpl) {
            owner = jvmName((NamespaceDescriptor) descriptor.getContainingDeclaration());
        }
        else if (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor.getContainingDeclaration();
            owner = jvmName(classDescriptor, kind instanceof OwnerKind.DelegateKind ? OwnerKind.INTERFACE : kind);
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + descriptor.getContainingDeclaration());
        }
        return owner;
    }

    public Type mapType(final JetType jetType) {
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
            final PsiElement declaration = bindingContext.getDeclarationPsiElement(descriptor);
            if (declaration instanceof PsiClass) {
                return psiClassType((PsiClass) declaration);
            }
            return Type.getObjectType(jvmNameForInterface((ClassDescriptor) descriptor));
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    public Method mapSignature(JetFunction f) {
        final List<JetParameter> parameters = f.getValueParameters();
        Type[] parameterTypes = new Type[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterTypes[i] = mapType(bindingContext.resolveTypeReference(parameters.get(i).getTypeReference()));
        }
        final JetTypeReference returnTypeRef = f.getReturnTypeRef();
        Type returnType;
        if (returnTypeRef == null) {
            final FunctionDescriptor functionDescriptor = bindingContext.getFunctionDescriptor(f);
            final JetType type = functionDescriptor.getUnsubstitutedReturnType();
            returnType = mapType(type);
        }
        else {
            returnType = mapType(bindingContext.resolveTypeReference(returnTypeRef));
        }
        return new Method(f.getName(), returnType, parameterTypes);
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

    public Method mapConstructorSignature(ConstructorDescriptor descriptor, OwnerKind kind) {
        boolean delegate = kind == OwnerKind.DELEGATING_IMPLEMENTATION;
        List<ValueParameterDescriptor> parameters = descriptor.getUnsubstitutedValueParameters();
        int count = parameters.size();
        int first = delegate ? 1 : 0;
        Type[] parameterTypes = new Type[count + first];
        if (delegate) {
            parameterTypes[0] = jetInterfaceType(descriptor.getContainingDeclaration());
        }

        for (int i = 0; i < count; i++) {
            parameterTypes[i + first] = mapType(parameters.get(i).getOutType());
        }
        return new Method("<init>", Type.VOID_TYPE, parameterTypes);
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
}
