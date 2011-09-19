package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author max
 * @author yole
 * @author alex.tkachman
 */
public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private JetDelegationSpecifier superCall;
    private String superClass = "java/lang/Object";

    public ImplementationBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
    }

    private Set<String> getSuperInterfaces(JetClassOrObject aClass) {
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<String> superInterfaces = new LinkedHashSet<String>();

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = state.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);

            if (superPsi instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) superPsi;
                String fqn = psiClass.getQualifiedName();
                if (psiClass.isInterface()) {
                    superInterfaces.add(fqn.replace('.', '/'));
                }
                else {
                    if (superClassName == null) {
                        superClassName = fqn.replace('.', '/');

                        while (psiClass != null) {
                            for (PsiClass ifs : psiClass.getInterfaces()) {
                                superInterfaces.add(ifs.getQualifiedName().replace('.', '/'));
                            }
                            psiClass = psiClass.getSuperClass();
                        }
                    }
                    else {
                        throw new RuntimeException("Cannot determine single class to inherit from");
                    }
                }
            }
            else {
                if(superPsi == null || ((JetClass)superPsi).isTrait())
                    superInterfaces.add(JetTypeMapper.jvmNameForInterface(superClassDescriptor));
            }
        }
        return superInterfaces;
    }

    @Override
    protected void generateDeclaration() {
        getSuperClass();

        List<String> interfaces = new ArrayList<String>();
        interfaces.add("jet/JetObject");
        interfaces.addAll(getSuperInterfaces(myClass));

        boolean isAbstract = false;
        boolean isInterface = false;
        if(myClass instanceof JetClass) {
           if(((JetClass) myClass).hasModifier(JetTokens.ABSTRACT_KEYWORD))
               isAbstract = true;
            if(((JetClass) myClass).isTrait()) {
                isAbstract = true;
                isInterface = true;
            }
        }

        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | (isAbstract ? Opcodes.ACC_ABSTRACT : 0) | (isInterface ? Opcodes.ACC_INTERFACE : 0),
                jvmName(),
                null,
                superClass,
                interfaces.toArray(new String[interfaces.size()])
        );
        v.visitSource(myClass.getContainingFile().getName(), null);

        if(myClass instanceof JetClass) {
            AnnotationVisitor annotationVisitor = v.visitAnnotation("Ljet/typeinfo/JetSignature;", true);
            annotationVisitor.visit("value", SignatureUtil.classToSignature((JetClass)myClass, state.getBindingContext(), state.getTypeMapper()));
            annotationVisitor.visitEnd();
        }
    }

    private String jvmName() {
        return state.getTypeMapper().jvmName(descriptor, kind);
    }

    protected void getSuperClass() {
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if(myClass instanceof JetClass && ((JetClass) myClass).isTrait())
            return;

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperClass || specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = state.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                final PsiElement declaration = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
                if (declaration != null) {
                    if (declaration instanceof PsiClass) {
                        if (!((PsiClass) declaration).isInterface()) {
                            superClass = state.getTypeMapper().jvmName(superClassDescriptor, kind);
                            superCall = specifier;
                            return;
                        }
                    }
                    else if(declaration instanceof JetClass) {
                        if(!((JetClass) declaration).isTrait()) {
                            superClass = state.getTypeMapper().jvmName(superClassDescriptor, kind);
                            superCall = specifier;
                        }
                    }
                }
            }
        }
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
        //genGetSuperTypesTypeInfo();
    }

    private void generateFieldForTypeInfo() {
        if(myClass instanceof JetClass && ((JetClass)myClass).isTrait())
            return;

        final boolean typeInfoIsStatic = descriptor.getTypeConstructor().getParameters().size() == 0;
        v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | (typeInfoIsStatic ? Opcodes.ACC_STATIC : 0), "$typeInfo",
                     "Ljet/typeinfo/TypeInfo;", null, null);
        if (typeInfoIsStatic) {
            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    JetTypeMapper typeMapper = state.getTypeMapper();
                    ClassCodegen.newTypeInfo(v, false, typeMapper.jvmType(descriptor, OwnerKind.IMPLEMENTATION));
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
        if(myClass instanceof JetClass && ((JetClass) myClass).isTrait())
            return;

        ConstructorDescriptor constructorDescriptor = state.getBindingContext().get(BindingContext.CONSTRUCTOR, myClass);

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

        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor != null
                ? constructorDescriptor.getValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(callableMethod, constructorDescriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state);

        for(int slot = 0; slot != frameMap.getTypeParameterCount(); ++slot) {
            codegen.addTypeParameter(constructorDescriptor.getTypeParameters().get(slot), StackValue.local(frameMap.getFirstTypeParameter() + slot, JetTypeMapper.TYPE_TYPEINFO));
        }

        String classname = state.getTypeMapper().jvmName(descriptor, kind);
        final Type classType = Type.getType("L" + classname + ";");

        HashSet<FunctionDescriptor> overridden = new HashSet<FunctionDescriptor>();
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetFunction) {
                overridden.addAll(state.getBindingContext().get(BindingContext.FUNCTION, declaration).getOverriddenDescriptors());
            }
        }

        if (superCall == null || superCall instanceof JetDelegatorToSuperClass) {
            iv.load(0, Type.getType("L" + superClass + ";"));
            iv.invokespecial(superClass, "<init>", "()V");
        }
        else {
            iv.load(0, classType);
            ConstructorDescriptor constructorDescriptor1 = (ConstructorDescriptor) state.getBindingContext().get(BindingContext.REFERENCE_TARGET, ((JetDelegatorToSuperCall) superCall).getCalleeExpression().getConstructorReferenceExpression());
            generateDelegatorToConstructorCall(iv, codegen, (JetDelegatorToSuperCall) superCall, constructorDescriptor1, frameMap);
        }

        int n = 0;
        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if(specifier == superCall)
                continue;

            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                iv.load(0, classType);
                codegen.genToJVMStack(((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression());

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
        }

        final ClassDescriptor outerDescriptor = getOuterClassDescriptor();
        if (outerDescriptor != null && outerDescriptor.getKind() != ClassKind.OBJECT) {
            final Type type = JetTypeMapper.jetImplementationType(outerDescriptor);
            String interfaceDesc = type.getDescriptor();
            final String fieldName = "this$0";
            v.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, interfaceDesc, null, null);
            iv.load(0, classType);
            iv.load(frameMap.getOuterThisIndex(), type);
            iv.putfield(classname, fieldName, interfaceDesc);
        }

        if (frameMap.getFirstTypeParameter() > 0 && kind == OwnerKind.IMPLEMENTATION) {
            generateTypeInfoInitializer(frameMap.getFirstTypeParameter(), frameMap.getTypeParameterCount(), iv);
        }

        generateInitializers(codegen, iv);

        generateTraitMethods(codegen);

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

    private void generateTraitMethods(ExpressionCodegen codegen) {
        if(!(myClass instanceof JetClass) || ((JetClass)myClass).isTrait() || ((JetClass)myClass).hasModifier(JetTokens.ABSTRACT_KEYWORD))
            return;

        HashSet<FunctionDescriptor> set = new HashSet<FunctionDescriptor>();
        getAbstractMethods(descriptor.getDefaultType(), set);
        for(FunctionDescriptor fun: set) {
            int flags = Opcodes.ACC_PUBLIC; // TODO.

            Method function = state.getTypeMapper().mapSignature(fun.getName(), fun);

            final MethodVisitor mv = v.visitMethod(flags, function.getName(), function.getDescriptor(), null, null);
            mv.visitCode();

            codegen.generateThisOrOuter(descriptor);

            Type[] argTypes = function.getArgumentTypes();
            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                iv.load(reg, argType);
                //noinspection AssignmentToForLoopParameter
                reg += argType.getSize();
            }

            ClassDescriptor containingDeclaration = (ClassDescriptor) fun.getContainingDeclaration();
            JetType jetType = TraitImplBodyCodegen.getSuperClass(containingDeclaration, state.getBindingContext());
            Type type = state.getTypeMapper().mapType(jetType);
            if(type.getInternalName().equals("java/lang/Object")) {
                jetType = containingDeclaration.getDefaultType();
                type = state.getTypeMapper().mapType(jetType);
            }

            String fdescriptor = function.getDescriptor().replace("(","(" +  type.getDescriptor());
            iv.invokestatic(state.getTypeMapper().jvmName((ClassDescriptor) fun.getContainingDeclaration(), OwnerKind.TRAIT_IMPL), function.getName(), fdescriptor);
            iv.areturn(function.getReturnType());
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
    private void getAbstractMethods(JetType type, Set<FunctionDescriptor> set) {
        if(type.equals(JetStandardClasses.getAny())) {
            return;
        }

        for(JetType superType : type.getConstructor().getSupertypes()) {
            getAbstractMethods(superType, set);
        }

        PsiElement psiElement = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, type.getConstructor().getDeclarationDescriptor());
        if(psiElement instanceof JetClass) {
            JetClass jetClass = (JetClass) psiElement;
            for(JetDeclaration decl : jetClass.getDeclarations()) {
                if(decl instanceof JetNamedFunction) {
                    JetNamedFunction jetNamedFunction = (JetNamedFunction) decl;
                    FunctionDescriptor funDescriptor = state.getBindingContext().get(BindingContext.FUNCTION, decl);
                    set.removeAll(funDescriptor.getOverriddenDescriptors());
                            
                    if(jetNamedFunction.getBodyExpression() == null || jetClass.isTrait()) {
                        set.add(funDescriptor);
                    }
                }
            }
        }
        else if(psiElement instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) psiElement;
        }
    }

    @Nullable
    private ClassDescriptor getOuterClassDescriptor() {
        if (myClass.getParent() instanceof JetClassObject) {
            return null;
        }
        final DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        return outerDescriptor instanceof ClassDescriptor ? (ClassDescriptor) outerDescriptor : null;
    }

    private void generateDelegatorToConstructorCall(InstructionAdapter iv, ExpressionCodegen codegen, JetCallElement constructorCall,
                                                    ConstructorDescriptor constructorDescriptor,
                                                    ConstructorFrameMap frameMap) {
        ClassDescriptor classDecl = constructorDescriptor.getContainingDeclaration();
        PsiElement declaration = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDecl);
        Type type;
        if (declaration instanceof PsiClass) {
            type = JetTypeMapper.psiClassType((PsiClass) declaration);
        }
        else {
            type = JetTypeMapper.jetImplementationType(classDecl);
        }

        iv.load(0, type);

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
        else if (declaration instanceof JetEnumEntry && !((JetEnumEntry) declaration).hasPrimaryConstructor()) {
            String name = declaration.getName();
            final String desc = "L" + state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION) + ";";
            v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, name, desc, null, null);
            if (myEnumConstants.isEmpty()) {
                staticInitializerChunks.add(new CodeChunk() {
                    @Override
                    public void generate(InstructionAdapter v) {
                        initializeEnumConstants(v);
                    }
                });
            }
            myEnumConstants.add((JetEnumEntry) declaration);
        }
        else {
            super.generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }
    }

    private final List<JetEnumEntry> myEnumConstants = new ArrayList<JetEnumEntry>();

    private void initializeEnumConstants(InstructionAdapter v) {
        ExpressionCodegen codegen = new ExpressionCodegen(v, new FrameMap(), Type.VOID_TYPE, context, state);
        for (JetEnumEntry enumConstant : myEnumConstants) {
            // TODO type and constructor parameters
            String implClass = state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION);

            final List<JetDelegationSpecifier> delegationSpecifiers = enumConstant.getDelegationSpecifiers();
            if (delegationSpecifiers.size() > 1) {
                throw new UnsupportedOperationException("multiple delegation specifiers for enum constant not supported");
            }

            v.anew(Type.getObjectType(implClass));
            v.dup();

            if (delegationSpecifiers.size() == 1) {
                final JetDelegationSpecifier specifier = delegationSpecifiers.get(0);
                if (specifier instanceof JetDelegatorToSuperCall) {
                    final JetDelegatorToSuperCall superCall = (JetDelegatorToSuperCall) specifier;
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) state.getBindingContext().get(BindingContext.REFERENCE_TARGET, superCall.getCalleeExpression().getConstructorReferenceExpression());
                    CallableMethod method = state.getTypeMapper().mapToCallableMethod(constructorDescriptor, OwnerKind.IMPLEMENTATION);
                    codegen.invokeMethodWithArguments(method, superCall);
                }
                else {
                    throw new UnsupportedOperationException("unsupported type of enum constant initializer: " + specifier);
                }
            }
            else {
                v.invokespecial(implClass, "<init>", "()V");
            }
            v.putstatic(implClass, enumConstant.getName(), "L" + implClass + ";");
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
                generateDelegatorToConstructorCall(iv, codegen, thisCall, (ConstructorDescriptor) thisDescriptor, frameMap);
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

        iv.aconst(state.getTypeMapper().jvmType(descriptor, OwnerKind.IMPLEMENTATION));
        iv.iconst(0);
        iv.iconst(typeParamCount);
        iv.newarray(JetTypeMapper.TYPE_TYPEINFOPROJECTION);

        for (int i = 0; i < typeParamCount; i++) {
            iv.dup();
            iv.iconst(i);
            iv.load(firstTypeParameter + i, JetTypeMapper.TYPE_OBJECT);
            iv.checkcast(JetTypeMapper.TYPE_TYPEINFOPROJECTION);
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
                            type = JetTypeMapper.boxType(type);
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
        if(myClass instanceof JetClass && ((JetClass)myClass).isTrait())
            return;

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

    private void genGetSuperTypesTypeInfo() {
        if(!(myClass instanceof JetClass) || ((JetClass)myClass).isTrait()) {
            return;
        }

        String sig = getGetSuperTypesTypeInfoSignature(descriptor.getDefaultType());

        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
                "$$getSuperTypesTypeInfo",
                sig,
                null /* TODO */,
                null);
        mv.visitCode();
        InstructionAdapter v = new InstructionAdapter(mv);

        ExpressionCodegen codegen = new ExpressionCodegen(v, new FrameMap(), Type.VOID_TYPE, context, state);

        v.load(0, JetTypeMapper.TYPE_OBJECT);

        int k = 1;
        for (TypeParameterDescriptor parameterDescriptor : descriptor.getTypeConstructor().getParameters()) {
            codegen.addTypeParameter(parameterDescriptor, StackValue.local(k++, JetTypeMapper.TYPE_TYPEINFO));
        }

        for(JetType superType : descriptor.getTypeConstructor().getSupertypes()) {
            for (TypeProjection typeProjection : superType.getArguments()) {
                codegen.generateTypeInfo(typeProjection.getType());
            }
            v.invokestatic(state.getTypeMapper().mapType(superType).getInternalName(), "$$getSuperTypesTypeInfo", getGetSuperTypesTypeInfoSignature(superType));
        }

        v.areturn(Type.VOID_TYPE);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String getGetSuperTypesTypeInfoSignature(JetType type) {
        List<TypeParameterDescriptor> typeParameters = type.getConstructor().getParameters();
        StringBuilder sb = new StringBuilder("(Ljava/util/Set;");
        for(TypeParameterDescriptor tp : typeParameters)
            sb.append("Ljet/typeinfo/TypeInfo;");
        sb.append(")V");

        return sb.toString();
    }
}
