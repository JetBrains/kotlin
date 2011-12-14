package org.jetbrains.jet.codegen;

import com.intellij.psi.util.PsiTreeUtil;
import jet.JetObject;
import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
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
    public static final Type JET_PARAMETER_TYPE = Type.getObjectType("jet/typeinfo/JetValueParameter");
    public static final Type JET_TYPE_PARAMETER_TYPE = Type.getObjectType("jet/typeinfo/JetTypeParameter");
    public static final Type JET_METHOD_TYPE = Type.getObjectType("jet/typeinfo/JetMethod");

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
    public static final Type TYPE_BOOLEAN_ITERATOR = Type.getObjectType("jet/BooleanIterator");
    public static final Type TYPE_CHAR_ITERATOR = Type.getObjectType("jet/CharIterator");
    public static final Type TYPE_BYTE_ITERATOR = Type.getObjectType("jet/ByteIterator");
    public static final Type TYPE_SHORT_ITERATOR = Type.getObjectType("jet/ShortIterator");
    public static final Type TYPE_INT_ITERATOR = Type.getObjectType("jet/IntIterator");
    public static final Type TYPE_LONG_ITERATOR = Type.getObjectType("jet/LongIterator");
    public static final Type TYPE_FLOAT_ITERATOR = Type.getObjectType("jet/FloatIterator");
    public static final Type TYPE_DOUBLE_ITERATOR = Type.getObjectType("jet/DoubleIterator");

    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

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
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
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

    public static Type correctElementType(Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    public String getOwner(DeclarationDescriptor descriptor, OwnerKind kind) {
        String owner;
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            owner = NamespaceCodegen.getJVMClassName(DescriptorUtils.getFQName((NamespaceDescriptor) containingDeclaration));
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
            owner = mapType(classDescriptor.getDefaultType(), kind).getInternalName();
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate owner for parent " + containingDeclaration);
        }
        return owner;
    }

    @NotNull public Type mapReturnType(final JetType jetType) {
        return mapReturnType(jetType, null);
    }

    @NotNull private Type mapReturnType(final JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        if (jetType.equals(JetStandardClasses.getUnitType()) || jetType.equals(JetStandardClasses.getNothingType())) {
            if (signatureVisitor != null) {
                signatureVisitor.writeAsmType(Type.VOID_TYPE, false);
            }
            return Type.VOID_TYPE;
        }
        if (jetType.equals(JetStandardClasses.getNullableNothingType())) {
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, TYPE_OBJECT, false);
            }
            return TYPE_OBJECT;
        }
        return mapType(jetType, OwnerKind.IMPLEMENTATION, signatureVisitor);
    }

    private HashMap<DeclarationDescriptor,Map<DeclarationDescriptor,String>> naming = new HashMap<DeclarationDescriptor, Map<DeclarationDescriptor, String>>();
    
    public String getFQName(DeclarationDescriptor descriptor) {
        descriptor = descriptor.getOriginal();

        if(descriptor instanceof FunctionDescriptor) {
            return getFQName(descriptor.getContainingDeclaration());
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        String name = descriptor.getName();
        if(JetPsiUtil.NO_NAME_PROVIDED.equals(name)) {
            // create and store name
            Map<DeclarationDescriptor, String> map = naming.get(container);
            if(map == null) {
                map = new HashMap<DeclarationDescriptor, String>();
                naming.put(container, map);
            }

            name = map.get(descriptor);
            if(name == null) {
                name = getFQName(container) + "$" + (map.size()+1);
                map.put(descriptor, name);
            }
            return name;
        }
        if(name.contains("/"))
            return name;
        if (container != null) {
            if (container instanceof ModuleDescriptor) {
                return name;
            }
            if(container instanceof JavaNamespaceDescriptor && JavaDescriptorResolver.JAVA_ROOT.equals(container.getName())) {
                return name;
            }
            String baseName = getFQName(container);
            if (!baseName.isEmpty()) { 
                return baseName + (container instanceof JavaNamespaceDescriptor || container instanceof NamespaceDescriptor ? "/" : "$") + name;
            }
        }

        return name;
    }
    
    @NotNull public Type mapType(final JetType jetType) {
        return mapType(jetType, (BothSignatureWriter) null);
    }

    @NotNull private Type mapType(JetType jetType, @Nullable BothSignatureWriter signatureVisitor) {
        return mapType(jetType, OwnerKind.IMPLEMENTATION, signatureVisitor);
    }

    @NotNull public Type mapType(@NotNull final JetType jetType, OwnerKind kind) {
        return mapType(jetType, kind, null);
    }

    @NotNull private Type mapType(JetType jetType, OwnerKind kind, @Nullable BothSignatureWriter signatureVisitor) {
        return mapType(jetType, kind, signatureVisitor, false);
    }

    @NotNull public Type mapType(JetType jetType, OwnerKind kind, @Nullable BothSignatureWriter signatureVisitor, boolean boxPrimitive) {
        Type known = knowTypes.get(jetType);
        if (known != null) {
            return mapKnownAsmType(jetType, known, signatureVisitor, boxPrimitive);
        }

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();

        if (ErrorUtils.isError(descriptor)) {
            throw new IllegalStateException("should not compile an error type");
        }

        if (standardLibrary.getArray().equals(descriptor)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            JetType memberType = jetType.getArguments().get(0).getType();
            
            if (signatureVisitor != null) {
                signatureVisitor.writeArrayType(jetType.isNullable());
                mapType(memberType, kind, signatureVisitor, true);
                signatureVisitor.writeArrayEnd();
            }
            
            if (!isGenericsArray(jetType)) {
                return Type.getType("[" + boxType(mapType(memberType, kind)).getDescriptor());
            } else {
                return ARRAY_GENERIC_TYPE;
            }
        }

        if (JetStandardClasses.getAny().equals(descriptor)) {
            if (signatureVisitor != null) {
                visitAsmType(signatureVisitor, TYPE_OBJECT, jetType.isNullable());
            }
            return TYPE_OBJECT;
        }

        if (descriptor instanceof ClassDescriptor) {
            
            String name = getFQName(descriptor);
            Type asmType = Type.getObjectType(name + (kind == OwnerKind.TRAIT_IMPL ? "$$TImpl" : ""));

            if (signatureVisitor != null) {
                signatureVisitor.writeClassBegin(asmType.getInternalName(), jetType.isNullable());
                for (TypeProjection proj : jetType.getArguments()) {
                    // TODO: +-
                    signatureVisitor.writeTypeArgument('=');
                    mapType(proj.getType(), kind, signatureVisitor, true);
                    signatureVisitor.writeTypeArgumentEnd();
                }
                signatureVisitor.writeClassEnd();
            }

            return asmType;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            if (signatureVisitor != null) {
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) jetType.getConstructor().getDeclarationDescriptor();
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable());
            }

            return mapType(((TypeParameterDescriptor) descriptor).getUpperBoundsAsType(), kind);
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }
    
    private Type mapKnownAsmType(JetType jetType, Type asmType, @Nullable BothSignatureWriter signatureVisitor, boolean genericTypeParameter) {
        if (signatureVisitor != null) {
            if (genericTypeParameter) {
                visitAsmType(signatureVisitor, boxType(asmType), jetType.isNullable());
            } else {
                visitAsmType(signatureVisitor, asmType, jetType.isNullable());
            }
        }
        return asmType;
    }

    public static void visitAsmType(BothSignatureWriter visitor, Type asmType, boolean nullable) {
        visitor.writeAsmType(asmType, nullable);
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
        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal(), true, valueParameterTypes, kind);
        String owner;
        int invokeOpcode;
        ClassDescriptor thisClass;
        if (functionParent instanceof NamespaceDescriptor) {
            assert !superCall;
            owner = NamespaceCodegen.getJVMClassName(DescriptorUtils.getFQName(functionParent));
            invokeOpcode = INVOKESTATIC;
            thisClass = null;
        }
        else if (functionDescriptor instanceof ConstructorDescriptor) {
            assert !superCall;
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            owner = mapType(containingClass.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
            invokeOpcode = INVOKESPECIAL;
            thisClass = null;
        }
        else if (functionParent instanceof ClassDescriptor) {
            ClassDescriptor containingClass = (ClassDescriptor) functionParent;
            boolean isInterface = CodegenUtil.isInterface(containingClass);
            OwnerKind kind1 = isInterface && superCall ? OwnerKind.TRAIT_IMPL : OwnerKind.IMPLEMENTATION;
            Type type = mapType(containingClass.getDefaultType(), kind1);
            owner = type.getInternalName();
            invokeOpcode = isInterface
                    ? (superCall ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE)
                    : (superCall ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL);
            if(isInterface && superCall) {
                descriptor = mapSignature(functionDescriptor.getOriginal(), false, valueParameterTypes, OwnerKind.TRAIT_IMPL);
            }
            thisClass = (ClassDescriptor) functionParent;
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }

        final CallableMethod result = new CallableMethod(owner, descriptor, invokeOpcode, valueParameterTypes);
        result.setNeedsThis(thisClass);
        if(functionDescriptor.getReceiverParameter().exists()) {
            result.setNeedsReceiver(functionDescriptor);
        }

        return result;
    }

    private JvmMethodSignature mapSignature(FunctionDescriptor f, boolean needGenericSignature, List<Type> valueParameterTypes, OwnerKind kind) {
        
        for (ValueParameterDescriptor valueParameterDescriptor : f.getValueParameters()) {
            if (valueParameterDescriptor.hasDefaultValue()) {
                // TODO
                needGenericSignature = false;
            }
        }

        if (kind == OwnerKind.TRAIT_IMPL) {
            needGenericSignature = false;
        }
        
        BothSignatureWriter signatureVisitor = null;
        if (needGenericSignature) {
            signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD);
        }

        writeFormalTypeParameters(f.getTypeParameters(), signatureVisitor);

        final ReceiverDescriptor receiverTypeRef = f.getReceiverParameter();
        final JetType receiverType = !receiverTypeRef.exists() ? null : receiverTypeRef.getType();
        final List<ValueParameterDescriptor> parameters = f.getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) f.getContainingDeclaration();
            JetType jetType = TraitImplBodyCodegen.getSuperClass(containingDeclaration, bindingContext);
            Type type = mapType(jetType, signatureVisitor);
            if(type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = mapType(jetType, signatureVisitor);
            }
            valueParameterTypes.add(type);
            parameterTypes.add(type);
        }
        if (receiverType != null) {
            if (signatureVisitor != null) {
                signatureVisitor.writeParameterType();
            }
            parameterTypes.add(mapType(receiverType, signatureVisitor));
            if (signatureVisitor != null) {
                signatureVisitor.writeParameterTypeEnd();
            }
        }
        if (signatureVisitor != null) {
            signatureVisitor.writeParametersStart();
        }

        for (TypeParameterDescriptor parameterDescriptor : f.getTypeParameters()) {
            if(parameterDescriptor.isReified()) {
                parameterTypes.add(TYPE_TYPEINFO);
                if (signatureVisitor != null) {
                    signatureVisitor.writeParameterType();
                    visitAsmType(signatureVisitor, TYPE_TYPEINFO, false);
                    signatureVisitor.writeParameterTypeEnd();
                }
            }
        }
        for (ValueParameterDescriptor parameter : parameters) {
            if (signatureVisitor != null) {
                signatureVisitor.writeParameterType();
            }
            Type type = mapType(parameter.getOutType(), signatureVisitor);
            if (signatureVisitor != null) {
                signatureVisitor.writeParameterTypeEnd();
            }
            valueParameterTypes.add(type);
            parameterTypes.add(type);
        }
        
        if (signatureVisitor != null) {
            signatureVisitor.writeParametersEnd();
        }
        
        Type returnType;
        if (f instanceof ConstructorDescriptor) {
            returnType = Type.VOID_TYPE;
            if (signatureVisitor != null) {
                signatureVisitor.writeReturnType();
                visitAsmType(signatureVisitor, Type.VOID_TYPE, false);
                signatureVisitor.writeReturnTypeEnd();
            }
        } else {
            if (signatureVisitor != null) {
                signatureVisitor.writeReturnType();
                returnType = mapReturnType(f.getReturnType(), signatureVisitor);
                signatureVisitor.writeReturnTypeEnd();
            }
            else {
                returnType = mapReturnType(f.getReturnType(), null);
            }
        }
        Method method = new Method(f.getName(), returnType, parameterTypes.toArray(new Type[parameterTypes.size()]));
        if (signatureVisitor == null) {
            return new JvmMethodSignature(method, null, null, null, null);
        } else {
            return new JvmMethodSignature(method, signatureVisitor.makeJavaString(),
                    signatureVisitor.makeKotlinMethodTypeParameters(),
                    signatureVisitor.makeKotlinParameterTypes(),
                    signatureVisitor.makeKotlinReturnTypeSignature());
        }
    }


    public void writeFormalTypeParameters(List<TypeParameterDescriptor> typeParameters, BothSignatureWriter signatureVisitor) {
        if (signatureVisitor == null) {
            return;
        }

        signatureVisitor.writerFormalTypeParametersStart();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            writeFormalTypeParameter(typeParameterDescriptor, signatureVisitor);
        }
        signatureVisitor.writeFormalTypeParametersEnd();
    }

    private void writeFormalTypeParameter(TypeParameterDescriptor typeParameterDescriptor, BothSignatureWriter signatureVisitor) {
        signatureVisitor.writeFormalTypeParameter(typeParameterDescriptor.getName(), typeParameterDescriptor.getVariance());

        classBound:
        {
            signatureVisitor.writeClassBound();

            for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
                if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                    if (!CodegenUtil.isInterface(jetType)) {
                        mapType(jetType, signatureVisitor);
                        break classBound;
                    }
                }
            }

            // "extends Object" seems to be not optional according to ClassFileFormat-Java5.pdf
        }
        signatureVisitor.writeClassBoundEnd();

        for (JetType jetType : typeParameterDescriptor.getUpperBounds()) {
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
                if (CodegenUtil.isInterface(jetType)) {
                    signatureVisitor.writeInterfaceBound();
                    mapType(jetType, signatureVisitor);
                    signatureVisitor.writeInterfaceBoundEnd();
                }
            }
        }
        
        signatureVisitor.writeFormalTypeParameterEnd();

    }

    public JvmMethodSignature mapSignature(String name, FunctionDescriptor f) {
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
        // TODO: proper generic signature
        return new JvmMethodSignature(new Method(name, returnType, parameterTypes.toArray(new Type[parameterTypes.size()])), null, null, null, null);
    }


    public JvmMethodSignature mapGetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        Type returnType = mapType(descriptor.getOutType());
        String name = PropertyCodegen.getterName(descriptor.getName());
        ArrayList<Type> params = new ArrayList<Type>();
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            params.add(mapType(containingDeclaration.getDefaultType()));
        }

        if(descriptor.getReceiverParameter().exists()) {
            params.add(mapType(descriptor.getReceiverParameter().getType()));
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if(typeParameterDescriptor.isReified()) {
                params.add(TYPE_TYPEINFO);
            }
        }

        // TODO: proper generic signature
        return new JvmMethodSignature(new Method(name, returnType, params.toArray(new Type[params.size()])), null, null, null, null);
    }

    @Nullable
    public JvmMethodSignature mapSetterSignature(PropertyDescriptor descriptor, OwnerKind kind) {
        JetType inType = descriptor.getInType();
        if(inType == null)
            return null;

        String name = PropertyCodegen.setterName(descriptor.getName());
        ArrayList<Type> params = new ArrayList<Type>();
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            params.add(mapType(containingDeclaration.getDefaultType()));
        }

        if(descriptor.getReceiverParameter().exists()) {
            params.add(mapType(descriptor.getReceiverParameter().getType()));
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if(typeParameterDescriptor.isReified()) {
                params.add(TYPE_TYPEINFO);
            }
        }

        params.add(mapType(inType));

        // TODO: proper generic signature
        return new JvmMethodSignature(new Method(name, Type.VOID_TYPE, params.toArray(new Type[params.size()])), null, null, null, null);
    }

    private JvmMethodSignature mapConstructorSignature(ConstructorDescriptor descriptor, List<Type> valueParameterTypes) {
        List<ValueParameterDescriptor> parameters = descriptor.getOriginal().getValueParameters();
        List<Type> parameterTypes = new ArrayList<Type>();
        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
        if (CodegenUtil.hasThis0(classDescriptor)) {
            parameterTypes.add(mapType(CodegenUtil.getOuterClassDescriptor(classDescriptor).getDefaultType(), OwnerKind.IMPLEMENTATION));
        }

        if (CodegenUtil.requireTypeInfoConstructorArg(classDescriptor.getDefaultType())) {
            parameterTypes.add(TYPE_TYPEINFO);
        }

        for (ValueParameterDescriptor parameter : parameters) {
            final Type type = mapType(parameter.getOutType());
            parameterTypes.add(type);
            valueParameterTypes.add(type);
        }

        Method method = new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
        return new JvmMethodSignature(method, null, null, null, null); // TODO: generics signature
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind) {
        List<Type> valueParameterTypes = new ArrayList<Type>();
        final JvmMethodSignature method = mapConstructorSignature(descriptor, valueParameterTypes);
        String owner = mapType(descriptor.getContainingDeclaration().getDefaultType(), kind).getInternalName();
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
        if(expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }
        if(expression instanceof JetObjectDeclaration) {
            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
            return getFQName(declarationDescriptor);
        }

        String name = classNamesForAnonymousClasses.get(expression);
        if (name != null) {
            return name;
        }

        @SuppressWarnings("unchecked") JetNamedDeclaration container = PsiTreeUtil.getParentOfType(expression, JetNamespace.class, JetClass.class, JetObjectDeclaration.class);

        String baseName;
        if (container instanceof JetNamespace) {
            baseName = NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(((JetNamespace) container)));
        }
        else {
            ClassDescriptor aClass = bindingContext.get(BindingContext.CLASS, container);
            baseName = mapType(aClass.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
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
            result.add(mapType(classDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName());
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

    public Type getSharedVarType(DeclarationDescriptor descriptor) {
        if(descriptor instanceof PropertyDescriptor) {
            return StackValue.sharedTypeForType(mapType(((PropertyDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return StackValue.sharedTypeForType(mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType()));
        }
        else if (descriptor instanceof VariableDescriptor) {
            Boolean aBoolean = bindingContext.get(BindingContext.MUST_BE_WRAPPED_IN_A_REF, (VariableDescriptor) descriptor);
            if (aBoolean != null && aBoolean) {
                JetType outType = ((VariableDescriptor) descriptor).getOutType();
                return StackValue.sharedTypeForType(mapType(outType));
            }
            else {
                return null;
            }
        }
        return null;
    }
}
