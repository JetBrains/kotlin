package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author max
 * @author yole
 */
public class ImplementationBodyCodegen extends ClassBodyCodegen {
    public ImplementationBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
    }

    @Override
    protected void generateDeclaration() {
        String superClass = getSuperClass();

        List<String> interfaces = new ArrayList<String>();
        interfaces.add("jet/JetObject");
        if (!(myClass instanceof JetObjectDeclaration)) {
            interfaces.add(JetTypeMapper.jvmNameForInterface(descriptor));
        }
        else {
            interfaces.addAll(InterfaceBodyCodegen.getSuperInterfaces(myClass, state.getBindingContext()));
        }

        boolean isAbstract = myClass instanceof JetClass && ((JetClass) myClass).hasModifier(JetTokens.ABSTRACT_KEYWORD);

        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | (isAbstract ? Opcodes.ACC_ABSTRACT : 0),
                jvmName(),
                null,
                superClass,
                interfaces.toArray(new String[interfaces.size()])
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    private String jvmName() {
        return state.getTypeMapper().jvmName(descriptor, kind);
    }

    protected String getSuperClass() {
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if (delegationSpecifiers.isEmpty()) return "java/lang/Object";

        JetDelegationSpecifier first = delegationSpecifiers.get(0);
        if (first instanceof JetDelegatorToSuperClass || first instanceof JetDelegatorToSuperCall) {
            JetType superType = state.getBindingContext().get(BindingContext.TYPE, first.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            final PsiElement declaration = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
            if (declaration instanceof PsiClass && ((PsiClass) declaration).isInterface()) {
                return "java/lang/Object";
            }
            return state.getTypeMapper().jvmName(superClassDescriptor, kind);
        }

        return "java/lang/Object";
    }

    @Override
    protected void generateSyntheticParts() {
        generateFieldForTypeInfo();
        generateFieldForObjectInstance();
        generateFieldForClassObject();

        try {
            generatePrimaryConstructor();
        }
        catch(RuntimeException e) {
            throw new RuntimeException("Error generating primary constructor of class " + myClass.getName() + " with kind " + kind, e);
        }

        generateGetTypeInfo();
    }

    private void generateFieldForTypeInfo() {
        final boolean typeInfoIsStatic = descriptor.getTypeConstructor().getParameters().size() == 0;
        v.visitField(Opcodes.ACC_PRIVATE | (typeInfoIsStatic ? Opcodes.ACC_STATIC : 0), "$typeInfo",
                     "Ljet/typeinfo/TypeInfo;", null, null);
        if (typeInfoIsStatic) {
            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    JetTypeMapper typeMapper = state.getTypeMapper();
                    ClassCodegen.newTypeInfo(v, false, typeMapper.jvmType(descriptor, OwnerKind.INTERFACE));
                    v.putstatic(typeMapper.jvmName(descriptor, kind), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
                }
            });
        }
    }

    private void generateFieldForObjectInstance() {
        if (isNonLiteralObject()) {
            Type type = JetTypeMapper.jetImplementationType(descriptor);
            v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$instance", type.getDescriptor(), null, null);

            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    String name = jvmName();
                    v.anew(Type.getObjectType(name));
                    v.dup();
                    v.invokespecial(name, "<init>", "()V");
                    v.putstatic(name, "$instance", JetTypeMapper.jetImplementationType(descriptor).getDescriptor());
                }
            });

        }
    }

    private void generateFieldForClassObject() {
        final JetClassObject classObject = getClassObject();
        if (classObject != null) {
            Type type = Type.getObjectType(state.getTypeMapper().jvmName(classObject));
            v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$classobj", type.getDescriptor(), null, null);

            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    String name = state.getTypeMapper().jvmName(classObject);
                    final Type classObjectType = Type.getObjectType(name);
                    v.anew(classObjectType);
                    v.dup();
                    v.invokespecial(name, "<init>", "()V");
                    v.putstatic(state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION), "$classobj",
                                classObjectType.getDescriptor());
                }
            });
        }
    }

    protected void generatePrimaryConstructor() {
        ConstructorDescriptor constructorDescriptor = state.getBindingContext().get(BindingContext.CONSTRUCTOR, myClass);
        if (constructorDescriptor == null && !(myClass instanceof JetObjectDeclaration) && !isEnum(myClass)) return;

        Method method;
        CallableMethod callableMethod;
        if (constructorDescriptor == null) {
            method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
            callableMethod = new CallableMethod("", method, Opcodes.INVOKESPECIAL, Collections.<Type>emptyList());
        }
        else {
            callableMethod = state.getTypeMapper().mapToCallableMethod(constructorDescriptor, kind);
            method = callableMethod.getSignature();
        }
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getDescriptor(), null, null);
        mv.visitCode();

        Type[] argTypes = method.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor != null
                ? constructorDescriptor.getValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(callableMethod, constructorDescriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state);

        String classname = state.getTypeMapper().jvmName(descriptor, kind);
        final Type classType = Type.getType("L" + classname + ";");

        List<JetDelegationSpecifier> specifiers = myClass.getDelegationSpecifiers();

        if (specifiers.isEmpty() || !(specifiers.get(0) instanceof JetDelegatorToSuperCall)) {
            // TODO correct calculation of super class
            String superClass = "java/lang/Object";
            if (!specifiers.isEmpty()) {
                final JetType superType = state.getBindingContext().get(BindingContext.TYPE, specifiers.get(0).getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if (superClassDescriptor.hasConstructors()) {
                    superClass = getSuperClass();
                }
            }
            iv.load(0, Type.getType("L" + superClass + ";"));
            iv.invokespecial(superClass, "<init>", /* TODO super constructor descriptor */"()V");
        }

        final ClassDescriptor outerDescriptor = getOuterClassDescriptor();
        if (outerDescriptor != null && !outerDescriptor.isObject()) {
            final Type type = JetTypeMapper.jetImplementationType(outerDescriptor);
            String interfaceDesc = type.getDescriptor();
            final String fieldName = "this$0";
            v.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, interfaceDesc, null, null);
            iv.load(0, classType);
            iv.load(frameMap.getOuterThisIndex(), type);
            iv.putfield(classname, fieldName, interfaceDesc);
        }

        if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            String interfaceDesc = JetTypeMapper.jetInterfaceType(descriptor).getDescriptor();
            v.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "$this", interfaceDesc, /*TODO*/null, null);
            iv.load(0, classType);
            iv.load(frameMap.getDelegateThisIndex(), argTypes[0]);
            iv.putfield(classname, "$this", interfaceDesc);
        }

        HashSet<FunctionDescriptor> overridden = new HashSet<FunctionDescriptor>();
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetFunction) {
                overridden.addAll(state.getBindingContext().get(BindingContext.FUNCTION, declaration).getOverriddenFunctions());
            }
        }

        int n = 0;
        for (JetDelegationSpecifier specifier : specifiers) {
            boolean delegateOnStack = specifier instanceof JetDelegatorToSuperCall && n > 0 ||
                                      specifier instanceof JetDelegatorByExpressionSpecifier;

            if (delegateOnStack) {
                iv.load(0, classType);
            }

            if (specifier instanceof JetDelegatorToSuperCall) {
                ConstructorDescriptor constructorDescriptor1 = (ConstructorDescriptor) state.getBindingContext().get(BindingContext.REFERENCE_TARGET, ((JetDelegatorToSuperCall) specifier).getCalleeExpression().getConstructorReferenceExpression());
                generateDelegatorToConstructorCall(iv, codegen, (JetDelegatorToSuperCall) specifier, constructorDescriptor1, n == 0, frameMap);
            }
            else if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                codegen.genToJVMStack(((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression());
            }

            if (delegateOnStack) {
                JetType superType = state.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                String delegateField = "$delegate_" + n;
                Type fieldType = JetTypeMapper.jetInterfaceType(superClassDescriptor);
                String fieldDesc = fieldType.getDescriptor();
                v.visitField(Opcodes.ACC_PRIVATE, delegateField, fieldDesc, /*TODO*/null, null);
                iv.putfield(classname, delegateField, fieldDesc);

                JetClass superClass = (JetClass) state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
                final ClassContext delegateContext = context.intoClass(superClassDescriptor,
                        new OwnerKind.DelegateKind(StackValue.field(fieldType, classname, delegateField, false),
                        JetTypeMapper.jvmNameForInterface(superClassDescriptor)));
                generateDelegates(superClass, delegateContext, overridden);
            }

            n++;
        }

        if (frameMap.getFirstTypeParameter() > 0 && kind == OwnerKind.IMPLEMENTATION) {
            generateTypeInfoInitializer(frameMap.getFirstTypeParameter(), frameMap.getTypeParameterCount(), iv);
        }

        generateInitializers(codegen, iv);

        int curParam = 0;
        List<JetParameter> constructorParameters = getPrimaryConstructorParameters();
        for (JetParameter parameter : constructorParameters) {
            if (parameter.getValOrVarNode() != null) {
                VariableDescriptor descriptor = paramDescrs.get(curParam);
                Type type = state.getTypeMapper().mapType(descriptor.getOutType());
                iv.load(0, classType);
                iv.load(frameMap.getIndex(descriptor), type);
                iv.putfield(classname, descriptor.getName(), type.getDescriptor());
            }
            curParam++;
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    static boolean isEnum(JetClassOrObject myClass) {
        final JetModifierList modifierList = myClass.getModifierList();
        return modifierList != null && modifierList.hasModifier(JetTokens.ENUM_KEYWORD);
    }

    @Nullable
    private ClassDescriptor getOuterClassDescriptor() {
        if (myClass.getParent() instanceof JetClassObject) {
            return null;
        }
        final DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        return outerDescriptor instanceof ClassDescriptor ? (ClassDescriptor) outerDescriptor : null;
    }

    private void generateDelegatorToConstructorCall(InstructionAdapter iv, ExpressionCodegen codegen, JetCall constructorCall,
                                                    ConstructorDescriptor constructorDescriptor, boolean isJavaSuperclass,
                                                    ConstructorFrameMap frameMap) {
        ClassDescriptor classDecl = constructorDescriptor.getContainingDeclaration();
        boolean isDelegating = kind == OwnerKind.DELEGATING_IMPLEMENTATION;
        PsiElement declaration = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDecl);
        Type type;
        if (declaration instanceof PsiClass) {
            type = JetTypeMapper.psiClassType((PsiClass) declaration);
        }
        else {
            type = isDelegating
                    ? JetTypeMapper.jetDelegatingImplementationType(classDecl)
                    : JetTypeMapper.jetImplementationType(classDecl);
        }

        if (!isJavaSuperclass) {
            if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
                codegen.thisToStack();
            }
        }

        if (isJavaSuperclass) {
            iv.load(0, type);
        }
        else {
            iv.anew(type);
            iv.dup();
        }

        if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            iv.load(frameMap.getDelegateThisIndex(), state.getTypeMapper().jvmType(classDecl, OwnerKind.INTERFACE));
        }
        if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
            iv.load(frameMap.getOuterThisIndex(), state.getTypeMapper().jvmType((ClassDescriptor) descriptor.getContainingDeclaration(), OwnerKind.IMPLEMENTATION));
        }

        CallableMethod method = state.getTypeMapper().mapToCallableMethod(constructorDescriptor, kind);
        codegen.invokeMethodWithArguments(method, constructorCall);
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetConstructor) {
            generateSecondaryConstructor((JetConstructor) declaration);
        }
        else if (declaration instanceof JetClassObject) {
            generateClassObject((JetClassObject) declaration);
        }
        else {
            super.generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }
    }

    private void generateSecondaryConstructor(JetConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = state.getBindingContext().get(BindingContext.CONSTRUCTOR, constructor);
        if (constructorDescriptor == null) {
            throw new UnsupportedOperationException("failed to get descriptor for secondary constructor");
        }
        CallableMethod method = state.getTypeMapper().mapToCallableMethod(constructorDescriptor, kind);
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getSignature().getDescriptor(), null, null);
        mv.visitCode();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(method, constructorDescriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state);

        for (JetDelegationSpecifier initializer : constructor.getInitializers()) {
            if (initializer instanceof JetDelegatorToThisCall) {
                JetDelegatorToThisCall thisCall = (JetDelegatorToThisCall) initializer;
                DeclarationDescriptor thisDescriptor = state.getBindingContext().get(BindingContext.REFERENCE_TARGET, thisCall.getThisReference());
                if (!(thisDescriptor instanceof ConstructorDescriptor)) {
                    throw new UnsupportedOperationException("expected 'this' delegator to resolve to constructor");
                }
                generateDelegatorToConstructorCall(iv, codegen, thisCall, (ConstructorDescriptor) thisDescriptor, true, frameMap);
            }
            else {
                throw new UnsupportedOperationException("unknown initializer type");
            }
        }

        JetExpression bodyExpression = constructor.getBodyExpression();
        if (bodyExpression != null) {
            codegen.gen(bodyExpression, Type.VOID_TYPE);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected void generateTypeInfoInitializer(int firstTypeParameter, int typeParamCount, InstructionAdapter iv) {
        iv.load(0, JetTypeMapper.TYPE_OBJECT);

        iv.aconst(state.getTypeMapper().jvmType(descriptor, OwnerKind.INTERFACE));
        iv.iconst(0);
        iv.iconst(typeParamCount);
        iv.newarray(JetTypeMapper.TYPE_TYPEINFOPROJECTION);

        for (int i = 0; i < typeParamCount; i++) {
            iv.dup();
            iv.iconst(i);
            iv.load(firstTypeParameter + i, JetTypeMapper.TYPE_OBJECT);
            ExpressionCodegen.genTypeInfoToProjection(iv, Variance.INVARIANT);
            iv.astore(JetTypeMapper.TYPE_OBJECT);
        }
        iv.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z[Ljet/typeinfo/TypeInfoProjection;)Ljet/typeinfo/TypeInfo;");
        iv.putfield(state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
    }

    protected void generateInitializers(ExpressionCodegen codegen, InstructionAdapter iv) {
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                if (state.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null) {
                        CompileTimeConstant<?> compileTimeValue = state.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, initializer);
                        if(compileTimeValue != null) {
                            assert compileTimeValue != null;
                            Object value = compileTimeValue.getValue();
                            Type type = state.getTypeMapper().mapType(propertyDescriptor.getOutType());
                            if(JetTypeMapper.isPrimitive(type)) {
                                if( !propertyDescriptor.getOutType().isNullable() && value instanceof Number) {
                                    if(type == Type.INT_TYPE && ((Number)value).intValue() == 0)
                                        continue;
                                    if(type == Type.BYTE_TYPE && ((Number)value).byteValue() == 0)
                                        continue;
                                    if(type == Type.LONG_TYPE && ((Number)value).longValue() == 0L)
                                        continue;
                                    if(type == Type.SHORT_TYPE && ((Number)value).shortValue() == 0)
                                        continue;
                                    if(type == Type.DOUBLE_TYPE && ((Number)value).doubleValue() == 0d)
                                        continue;
                                    if(type == Type.FLOAT_TYPE && ((Number)value).byteValue() == 0f)
                                        continue;
                                }
                                if(type == Type.BOOLEAN_TYPE && value instanceof Boolean && !((Boolean)value))
                                    continue;
                                if(type == Type.CHAR_TYPE && value instanceof Character && ((Character)value) == 0)
                                    continue;
                            }
                            else {
                                if(value == null)
                                    continue;
                            }
                        }
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        Type type = codegen.expressionType(initializer);
                        if(propertyDescriptor.getOutType().isNullable())
                            type = state.getTypeMapper().boxType(type);
                        codegen.gen(initializer, type);
                        codegen.intermediateValueForProperty(propertyDescriptor, false, false).store(iv);
                    }

                }
            }
            else if (declaration instanceof JetClassInitializer) {
                codegen.gen(((JetClassInitializer) declaration).getBody(), Type.VOID_TYPE);
            }
        }
    }

    protected void generateDelegates(JetClass toClass, ClassContext delegateContext, Set<FunctionDescriptor> overriden) {
        final FunctionCodegen functionCodegen = new FunctionCodegen(delegateContext, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(delegateContext, v, functionCodegen, state);

        for (JetDeclaration declaration : toClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration);
            }
            else if (declaration instanceof JetFunction) {
                if (!overriden.contains(state.getBindingContext().get(BindingContext.FUNCTION, declaration))) {
                    functionCodegen.gen((JetNamedFunction) declaration);
                }
            }
        }

        for (JetParameter p : toClass.getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = state.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    propertyCodegen.generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC);
                    if (propertyDescriptor.isVar()) {
                        propertyCodegen.generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC);
                    }
                }
            }
        }
    }

    @Nullable
    private JetClassObject getClassObject() {
        return myClass instanceof JetClass ? ((JetClass) myClass).getClassObject() : null;
    }

    private boolean isNonLiteralObject() {
        return myClass instanceof JetObjectDeclaration && !((JetObjectDeclaration) myClass).isObjectLiteral() &&
                !(myClass.getParent() instanceof JetClassObject);
    }

    private void generateGetTypeInfo() {
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC,
                "getTypeInfo",
                "()Ljet/typeinfo/TypeInfo;",
                null /* TODO */,
                null);
        mv.visitCode();
        InstructionAdapter v = new InstructionAdapter(mv);
        ExpressionCodegen.loadTypeInfo(state.getTypeMapper(), descriptor, v);
        v.areturn(JetTypeMapper.TYPE_TYPEINFO);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateClassObject(JetClassObject declaration) {
        state.forClass().generate(context, declaration.getObjectDeclaration());
    }
}
