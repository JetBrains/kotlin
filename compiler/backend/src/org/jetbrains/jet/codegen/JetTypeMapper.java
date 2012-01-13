package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import jet.JetObject;
import jet.TypeInfo;
import jet.typeinfo.TypeInfoProjection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
    public static final Type JL_NUMBER_TYPE = Type.getObjectType("java/lang/Number");
    public static final Type JL_STRING_BUILDER = Type.getObjectType("java/lang/StringBuilder");
    public static final Type JL_STRING_TYPE = Type.getObjectType("java/lang/String");
    private static final Type JL_COMPARABLE_TYPE = Type.getObjectType("java/lang/Comparable");

    public static final Type ARRAY_GENERIC_TYPE = Type.getType(Object[].class);

    private final JetStandardLibrary standardLibrary;
    private final BindingContext bindingContext;
    private final Map<JetExpression, String> classNamesForAnonymousClasses = new HashMap<JetExpression, String>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

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
            return "ClassObject$";
        }

        return null;
    }

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
                PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
                if (declaration instanceof JetObjectDeclaration) {
                    String stable = getStableNameForObject((JetObjectDeclaration) declaration, descriptor);
                    if (stable != null) {
                        name = stable;
                    }
                }

                if (name == null) {
                    name = getFQName(container) + "$" + (map.size()+1);
                }
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
            return Type.getObjectType("error/NonExistentClass");
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

            Type asmType;

            if (standardLibrary.getComparable().equals(descriptor)) {
                if (jetType.getArguments().size() != 1) {
                    throw new UnsupportedOperationException("Comparable must have one type argument");
                }

                asmType = JL_COMPARABLE_TYPE;
            } else {
                String name = getFQName(descriptor);
                asmType = Type.getObjectType(name + (kind == OwnerKind.TRAIT_IMPL ? JvmAbi.TRAIT_IMPL_SUFFIX : ""));
            }

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

            Type type = mapType(((TypeParameterDescriptor) descriptor).getUpperBoundsAsType(), kind);
            if (signatureVisitor != null) {
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) jetType.getConstructor().getDeclarationDescriptor();
                signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
            }
            return type;
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
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByWrapperAsmType(type);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getAsmType();
        } else {
            throw new UnsupportedOperationException("Unboxing: " + type);
        }
    }

    public static Type boxType(Type asmType) {
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByAsmType(asmType);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getWrapper().getAsmType();
        } else {
            return asmType;
        }
    }

    public CallableMethod mapToCallableMethod(FunctionDescriptor functionDescriptor, boolean superCall, OwnerKind kind) {
        if(functionDescriptor == null)
            return null;

        final DeclarationDescriptor functionParent = functionDescriptor.getContainingDeclaration();
        JvmMethodSignature descriptor = mapSignature(functionDescriptor.getOriginal(), true, kind);
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
                descriptor = mapSignature(functionDescriptor.getOriginal(), false, OwnerKind.TRAIT_IMPL);
            }
            thisClass = (ClassDescriptor) functionParent;
        }
        else {
            throw new UnsupportedOperationException("unknown function parent");
        }

        final CallableMethod result = new CallableMethod(owner, descriptor, invokeOpcode);
        result.setNeedsThis(thisClass);
        if(functionDescriptor.getReceiverParameter().exists()) {
            result.setNeedsReceiver(functionDescriptor);
        }

        return result;
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
            Type type = mapType(jetType);
            if(type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = mapType(jetType);
            }
            
            signatureVisitor.writeParameterType(JvmMethodParameterKind.THIS);
            signatureVisitor.writeAsmType(type, jetType.isNullable());
            signatureVisitor.writeParameterTypeEnd();
        }

        if (receiverType != null) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(receiverType, signatureVisitor);
            signatureVisitor.writeParameterTypeEnd();
        }

        for (TypeParameterDescriptor parameterDescriptor : f.getTypeParameters()) {
            if(parameterDescriptor.isReified()) {
                signatureVisitor.writeTypeInfoParameter();
            }
        }
        for (ValueParameterDescriptor parameter : parameters) {
            signatureVisitor.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getOutType(), signatureVisitor);
            signatureVisitor.writeParameterTypeEnd();
        }

        signatureVisitor.writeParametersEnd();

        if (f instanceof ConstructorDescriptor) {
            signatureVisitor.writeVoidReturn();
        } else {
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
                    mapType(jetType, signatureVisitor);
                    signatureVisitor.writeInterfaceBoundEnd();
                }
            }
            if (jetType.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                signatureVisitor.writeInterfaceBound();
                mapType(jetType, signatureVisitor);
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
            mapType(receiver.getType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }
        for (ValueParameterDescriptor parameter : parameters) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getOutType(), signatureWriter);
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
            mapType(containingDeclaration.getDefaultType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }

        if(descriptor.getReceiverParameter().exists()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(descriptor.getReceiverParameter().getType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if(typeParameterDescriptor.isReified()) {
                signatureWriter.writeTypeInfoParameter();
            }
        }
        
        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        mapType(descriptor.getOutType(), signatureWriter);
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

        JetType outType = descriptor.getOutType();

        signatureWriter.writeParametersStart();

        String name = PropertyCodegen.setterName(descriptor.getName());
        if(kind == OwnerKind.TRAIT_IMPL) {
            ClassDescriptor containingDeclaration = (ClassDescriptor) descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS);
            mapType(containingDeclaration.getDefaultType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }

        if(descriptor.getReceiverParameter().exists()) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.RECEIVER);
            mapType(descriptor.getReceiverParameter().getType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if(typeParameterDescriptor.isReified()) {
                signatureWriter.writeTypeInfoParameter();
            }
        }

        signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
        mapType(outType, signatureWriter);
        signatureWriter.writeParameterTypeEnd();
        
        signatureWriter.writeParametersEnd();
        
        signatureWriter.writeVoidReturn();

        JvmMethodSignature jvmMethodSignature = signatureWriter.makeJvmMethodSignature(name);
        return new JvmPropertyAccessorSignature(jvmMethodSignature, jvmMethodSignature.getKotlinParameterType(jvmMethodSignature.getParameterCount() - 1));
    }

    private JvmMethodSignature mapConstructorSignature(ConstructorDescriptor descriptor) {
        
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, true);
        
        List<ValueParameterDescriptor> parameters = descriptor.getOriginal().getValueParameters();
        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
        
        writeFormalTypeParameters(descriptor.getTypeParameters(), signatureWriter);

        signatureWriter.writeParametersStart();

        if (CodegenUtil.hasThis0(classDescriptor)) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.THIS0);
            mapType(CodegenUtil.getOuterClassDescriptor(classDescriptor).getDefaultType(), OwnerKind.IMPLEMENTATION, signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }

        if (CodegenUtil.requireTypeInfoConstructorArg(classDescriptor.getDefaultType())) {
            signatureWriter.writeTypeInfoParameter();
        }

        for (ValueParameterDescriptor parameter : parameters) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            mapType(parameter.getOutType(), signatureWriter);
            signatureWriter.writeParameterTypeEnd();
        }
        
        signatureWriter.writeParametersEnd();
        
        signatureWriter.writeVoidReturn();

        return signatureWriter.makeJvmMethodSignature("<init>");
    }

    public CallableMethod mapToCallableMethod(ConstructorDescriptor descriptor, OwnerKind kind) {
        final JvmMethodSignature method = mapConstructorSignature(descriptor);
        String owner = mapType(descriptor.getContainingDeclaration().getDefaultType(), kind).getInternalName();
        return new CallableMethod(owner, method, INVOKESPECIAL);
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

        @SuppressWarnings("unchecked")
        PsiElement container = PsiTreeUtil.getParentOfType(expression, JetFile.class, JetClass.class, JetObjectDeclaration.class);

        String baseName;
        if (container instanceof JetFile) {
            baseName = NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(((JetFile) container)));
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

        knowTypes.put(standardLibrary.getStringType(),JL_STRING_TYPE);
        knowTypes.put(standardLibrary.getNullableStringType(),JL_STRING_TYPE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            knowTypes.put(standardLibrary.getPrimitiveArrayJetType(primitiveType), jvmPrimitiveType.getAsmArrayType());
            knowTypes.put(standardLibrary.getNullablePrimitiveArrayJetType(primitiveType), jvmPrimitiveType.getAsmArrayType());
        }
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
