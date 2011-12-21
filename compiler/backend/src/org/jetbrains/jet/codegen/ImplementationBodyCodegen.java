package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.StdlibNames;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
    private String superClass;
    @Nullable // null means java/lang/Object
    private JetType superClassType;
    private final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;

    public ImplementationBodyCodegen(JetClassOrObject aClass, CodegenContext context, ClassBuilder v, GenerationState state) {
        super(aClass, context, v, state);
        typeMapper = state.getTypeMapper();
        bindingContext = state.getBindingContext();
    }

    @Override
    protected void generateDeclaration() {
        getSuperClass();

        JvmClassSignature signature = signature();

        boolean isAbstract = false;
        boolean isInterface = false;
        boolean isFinal = false;
        boolean isStatic = false;
        
        if(myClass instanceof JetClass) {
            JetClass jetClass = (JetClass) myClass;
            if (jetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD))
               isAbstract = true;
            if (jetClass.isTrait()) {
                isAbstract = true;
                isInterface = true;
            }
            if (!jetClass.hasModifier(JetTokens.OPEN_KEYWORD) && !isAbstract) {
                isFinal = true;
            }
        }
        else if (myClass.getParent() instanceof JetClassObject) {
            isStatic = true;
        }

        int access = 0;
        access |= Opcodes.ACC_PUBLIC;
        if (isAbstract) {
            access |= Opcodes.ACC_ABSTRACT;
        }
        if (isInterface) {
            access |= Opcodes.ACC_INTERFACE; // ACC_SUPER
        }
        if (isFinal) {
            access |= Opcodes.ACC_FINAL;
        }
        if (isStatic) {
            access |= Opcodes.ACC_STATIC;
        }
        v.defineClass(myClass, Opcodes.V1_6,
                access,
                      signature.getName(),
                      signature.getJavaGenericSignature(),
                      signature.getSuperclassName(),
                      signature.getInterfaces().toArray(new String[0])
        );
        v.visitSource(myClass.getContainingFile().getName(), null);

        ClassDescriptor container = getContainingClassDescriptor(descriptor);
        if(container != null) {
            v.visitOuterClass(typeMapper.mapType(container.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), null, null);
        }

        if(myClass instanceof JetClass && signature.getKotlinGenericSignature() != null) {
            AnnotationVisitor annotationVisitor = v.newAnnotation(myClass, JvmStdlibNames.JET_CLASS.getDescriptor(), true);
            annotationVisitor.visit(JvmStdlibNames.JET_CLASS_SIGNATURE, signature.getKotlinGenericSignature());
            annotationVisitor.visitEnd();
        }
    }

    private static ClassDescriptor getContainingClassDescriptor(ClassDescriptor decl) {
        DeclarationDescriptor container = decl.getContainingDeclaration();
        while (container != null && !(container instanceof NamespaceDescriptor)) {
            if (container instanceof ClassDescriptor) return (ClassDescriptor) container;
            container = container.getContainingDeclaration();
        }
        return null;
    }

    private JvmClassSignature signature() {
        List<String> superInterfaces;

        LinkedHashSet<String> superInterfacesLinkedHashSet = new LinkedHashSet<String>();

        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);


        {   // type parameters
            List<TypeParameterDescriptor> typeParameters = descriptor.getTypeConstructor().getParameters();
            typeMapper.writeFormalTypeParameters(typeParameters, signatureVisitor);
        }
        
        signatureVisitor.writeSupersStart();

        {   // superclass
            signatureVisitor.writeSuperclass();
            if (superClassType == null) {
                signatureVisitor.writeClassBegin(superClass, false);
                signatureVisitor.writeClassEnd();
            } else {
                typeMapper.mapType(superClassType, OwnerKind.IMPLEMENTATION, signatureVisitor, true);
            }
            signatureVisitor.writeSuperclassEnd();
        }


        {   // superinterfaces
            superInterfacesLinkedHashSet.add(JvmStdlibNames.JET_OBJECT.getInternalName());

            for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if (CodegenUtil.isInterface(superClassDescriptor)) {
                    signatureVisitor.writeInterface();
                    Type jvmName = typeMapper.mapType(superType, OwnerKind.IMPLEMENTATION, signatureVisitor, true);
                    signatureVisitor.writeInterfaceEnd();
                    superInterfacesLinkedHashSet.add(jvmName.getInternalName());
                }
            }

            superInterfaces = new ArrayList<String>(superInterfacesLinkedHashSet);
        }
        
        signatureVisitor.writeSupersEnd();

        return new JvmClassSignature(jvmName(), superClass, superInterfaces, signatureVisitor.makeJavaString(), signatureVisitor.makeKotlinClassSignature());
    }

    private String jvmName() {
        return typeMapper.mapType(descriptor.getDefaultType(), kind).getInternalName();
    }

    protected void getSuperClass() {
        superClass = "java/lang/Object";
        superClassType = null;

        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if(myClass instanceof JetClass && ((JetClass) myClass).isTrait())
            return;

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperClass || specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if(!CodegenUtil.isInterface(superClassDescriptor)) {
                    superClassType = superType;
                    superClass = typeMapper.mapType(superClassDescriptor.getDefaultType(), kind).getInternalName();
                    superCall = specifier;
                }
            }
        }
    }

    @Override
    protected void generateSyntheticParts(HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        generateFieldForObjectInstance();
        generateFieldForClassObject();
        generateAccessors(accessors);

        try {
            generatePrimaryConstructor();
        }
        catch(RuntimeException e) {
            throw new RuntimeException("Error generating primary constructor of class " + myClass.getName() + " with kind " + kind, e);
        }

        generateGetTypeInfo();
    }

    private void generateAccessors(HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        if(accessors != null) {
            for (Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry : accessors.entrySet()) {
                if(entry.getValue() instanceof FunctionDescriptor) {
                    FunctionDescriptor bridge = (FunctionDescriptor) entry.getValue();
                    FunctionDescriptor original = (FunctionDescriptor) entry.getKey();

                    Method method = typeMapper.mapSignature(bridge.getName(), bridge).getAsmMethod();
                    Method originalMethod = typeMapper.mapSignature(original.getName(), original).getAsmMethod();
                    Type[] argTypes = method.getArgumentTypes();

                    MethodVisitor mv = v.newMethod(null, Opcodes.ACC_PUBLIC|Opcodes.ACC_BRIDGE|Opcodes.ACC_FINAL, bridge.getName(), method.getDescriptor(), null, null);
                    if (v.generateCode()) {
                        mv.visitCode();

                        InstructionAdapter iv = new InstructionAdapter(mv);

                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        for (int i = 0, reg = 1; i < argTypes.length; i++) {
                            Type argType = argTypes[i];
                            iv.load(reg, argType);
                            //noinspection AssignmentToForLoopParameter
                            reg += argType.getSize();
                        }
                        iv.invokespecial(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION), originalMethod.getName(), originalMethod.getDescriptor());

                        iv.areturn(method.getReturnType());
                        FunctionCodegen.endVisit(iv, "accessor", null);
                    }
                }
                else if(entry.getValue() instanceof PropertyDescriptor) {
                    PropertyDescriptor bridge = (PropertyDescriptor) entry.getValue();
                    PropertyDescriptor original = (PropertyDescriptor) entry.getKey();

                    Method method = typeMapper.mapGetterSignature(bridge, OwnerKind.IMPLEMENTATION).getAsmMethod();
                    Method originalMethod = typeMapper.mapGetterSignature(original, OwnerKind.IMPLEMENTATION).getAsmMethod();
                    MethodVisitor mv = v.newMethod(null, Opcodes.ACC_PUBLIC|Opcodes.ACC_BRIDGE|Opcodes.ACC_FINAL, method.getName(), method.getDescriptor(), null, null);
                    InstructionAdapter iv = null;
                    if (v.generateCode()) {
                        mv.visitCode();

                        iv = new InstructionAdapter(mv);

                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        if(original.getVisibility() == Visibility.PRIVATE)
                            iv.getfield(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION), original.getName(), originalMethod.getReturnType().getDescriptor());
                        else
                            iv.invokespecial(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION), originalMethod.getName(), originalMethod.getDescriptor());

                        iv.areturn(method.getReturnType());
                        FunctionCodegen.endVisit(iv, "accessor", null);
                    }

                    method = typeMapper.mapSetterSignature(bridge, OwnerKind.IMPLEMENTATION).getAsmMethod();
                    originalMethod = typeMapper.mapSetterSignature(original, OwnerKind.IMPLEMENTATION).getAsmMethod();
                    mv = v.newMethod(null, Opcodes.ACC_PUBLIC|Opcodes.ACC_BRIDGE|Opcodes.ACC_FINAL, method.getName(), method.getDescriptor(), null, null);
                    if (v.generateCode()) {
                        mv.visitCode();

                        iv = new InstructionAdapter(mv);

                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        Type[] argTypes = method.getArgumentTypes();
                        for (int i = 0, reg = 1; i < argTypes.length; i++) {
                            Type argType = argTypes[i];
                            iv.load(reg, argType);
                            //noinspection AssignmentToForLoopParameter
                            reg += argType.getSize();
                        }
                        if(original.getVisibility() == Visibility.PRIVATE)
                            iv.putfield(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION), original.getName(), originalMethod.getArgumentTypes()[0].getDescriptor());
                        else
                            iv.invokespecial(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION), originalMethod.getName(), originalMethod.getDescriptor());

                        iv.areturn(method.getReturnType());
                        FunctionCodegen.endVisit(iv, "accessor", null);
                    }
                }
                else {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    private void generateFieldForObjectInstance() {
        if (isNonLiteralObject()) {
            Type type = typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION);
            v.newField(myClass, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$instance", type.getDescriptor(), null, null);

            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    String name = jvmName();
                    v.anew(Type.getObjectType(name));
                    v.dup();
                    v.invokespecial(name, "<init>", "()V");
                    v.putstatic(name, "$instance", typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getDescriptor());
                }
            });

        }
    }

    private void generateFieldForClassObject() {
        final JetClassObject classObject = getClassObject();
        if (classObject != null) {
            final ClassDescriptor descriptor1 = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
            Type type = Type.getObjectType(typeMapper.mapType(descriptor1.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName());
            v.newField(classObject, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$classobj", type.getDescriptor(), null, null);

            staticInitializerChunks.add(new CodeChunk() {
                @Override
                public void generate(InstructionAdapter v) {
                    final ClassDescriptor descriptor1 = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
                    String name = typeMapper.mapType(descriptor1.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
                    final Type classObjectType = Type.getObjectType(name);
                    v.anew(classObjectType);
                    v.dup();
                    v.invokespecial(name, "<init>", "()V");
                    v.putstatic(typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "$classobj",
                                classObjectType.getDescriptor());
                }
            });
        }
    }

    protected void generatePrimaryConstructor() {
        if(myClass instanceof JetClass && ((JetClass) myClass).isTrait())
            return;

        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, myClass);

        CodegenContext.ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor, typeMapper);

        Method constructorMethod;
        CallableMethod callableMethod;
        if (constructorDescriptor == null) {
            List<Type> parameterTypes = new ArrayList<Type>();
            if (CodegenUtil.hasThis0(descriptor)) {
                parameterTypes.add(typeMapper.mapType(CodegenUtil.getOuterClassDescriptor(descriptor).getDefaultType(), OwnerKind.IMPLEMENTATION));
            }

            if (CodegenUtil.requireTypeInfoConstructorArg(descriptor.getDefaultType())) {
                parameterTypes.add(JetTypeMapper.TYPE_TYPEINFO);
            }

            constructorMethod = new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
            callableMethod = new CallableMethod("", new JvmMethodSignature(constructorMethod, null, null, null, null) /* TODO */, Opcodes.INVOKESPECIAL, Collections.<Type>emptyList());
        }
        else {
            callableMethod = typeMapper.mapToCallableMethod(constructorDescriptor, kind);
            constructorMethod = callableMethod.getSignature().getAsmMethod();
        }

        ObjectOrClosureCodegen closure = context.closure;
        if(closure != null) {
            final List<Type> consArgTypes = new LinkedList<Type>(Arrays.asList(constructorMethod.getArgumentTypes()));

            int insert = 0;
            if(closure.captureThis) {
                if(!CodegenUtil.hasThis0(descriptor))
                    consArgTypes.add(insert, Type.getObjectType(context.getThisDescriptor().getName()));
                insert++;
            }
            else {
                if(CodegenUtil.hasThis0(descriptor))
                    insert++;
            }

            if(closure.captureReceiver != null)
                consArgTypes.add(insert++, closure.captureReceiver);

            for (DeclarationDescriptor descriptor : closure.closure.keySet()) {
                if(descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                    final Type sharedVarType = typeMapper.getSharedVarType(descriptor);
                    final Type type = sharedVarType != null ? sharedVarType : state.getTypeMapper().mapType(((VariableDescriptor) descriptor).getOutType());
                    consArgTypes.add(insert++, type);
                }
                else if(descriptor instanceof FunctionDescriptor) {
                    assert closure.captureReceiver != null;
                }
            }

            constructorMethod = new Method("<init>", Type.VOID_TYPE, consArgTypes.toArray(new Type[consArgTypes.size()]));
        }

        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.newMethod(myClass, flags, "<init>", constructorMethod.getDescriptor(), null, null);
        if (!v.generateCode()) return;
        
        mv.visitCode();

        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor != null
                ? constructorDescriptor.getValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(callableMethod, constructorDescriptor, descriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, constructorContext, state);

//        for(int slot = 0; slot != frameMap.getTypeParameterCount(); ++slot) {
//            if(constructorDescriptor != null)
//                codegen.addTypeParameter(constructorDescriptor.getTypeParameters().get(slot), StackValue.local(frameMap.getFirstTypeParameter() + slot, JetTypeMapper.TYPE_TYPEINFO));
//            else
//                codegen.addTypeParameter(descriptor.getTypeConstructor().getParameters().get(slot), StackValue.local(frameMap.getFirstTypeParameter() + slot, JetTypeMapper.TYPE_TYPEINFO));
//        }

        String classname = typeMapper.mapType(descriptor.getDefaultType(), kind).getInternalName();
        final Type classType = Type.getType("L" + classname + ";");

        HashSet<FunctionDescriptor> overridden = new HashSet<FunctionDescriptor>();
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetFunction) {
                FunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration);
                assert functionDescriptor != null;
                overridden.addAll(functionDescriptor.getOverriddenDescriptors());
            }
        }

        if (superCall == null || superCall instanceof JetDelegatorToSuperClass) {
            iv.load(0, Type.getType("L" + superClass + ";"));
            if(superCall == null) {
                iv.invokespecial(superClass, "<init>", "()V");
            }
            else {
                JetType superType = bindingContext.get(BindingContext.TYPE, superCall.getTypeReference());
                List<Type> parameterTypes = new ArrayList<Type>();
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if (CodegenUtil.hasThis0(superClassDescriptor)) {
                    iv.load(1, JetTypeMapper.TYPE_OBJECT);
                    parameterTypes.add(typeMapper.mapType(CodegenUtil.getOuterClassDescriptor(descriptor).getDefaultType(), OwnerKind.IMPLEMENTATION));
                }
                for(TypeProjection typeParameterDescriptor : superType.getArguments()) {
                    codegen.generateTypeInfo(typeParameterDescriptor.getType(), null);
                    parameterTypes.add(JetTypeMapper.TYPE_TYPEINFO);
                }
                Method superCallMethod = new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
                iv.invokespecial(typeMapper.mapType(superClassDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "<init>", superCallMethod.getDescriptor());
            }
        }
        else {
            iv.load(0, classType);
            ConstructorDescriptor constructorDescriptor1 = (ConstructorDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, ((JetDelegatorToSuperCall) superCall).getCalleeExpression().getConstructorReferenceExpression());
            generateDelegatorToConstructorCall(iv, codegen, (JetDelegatorToSuperCall) superCall, constructorDescriptor1, frameMap);
        }

        int n = 0;
        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if(specifier == superCall)
                continue;

            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                iv.load(0, classType);
                codegen.genToJVMStack(((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression());

                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                String delegateField = "$delegate_" + n;
                Type fieldType = typeMapper.mapType(superClassDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION);
                String fieldDesc = fieldType.getDescriptor();
                v.newField(specifier, Opcodes.ACC_PRIVATE, delegateField, fieldDesc, /*TODO*/null, null);
                iv.putfield(classname, delegateField, fieldDesc);

                JetClass superClass = (JetClass) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
                final CodegenContext delegateContext = context.intoClass(superClassDescriptor,
                        new OwnerKind.DelegateKind(StackValue.field(fieldType, classname, delegateField, false),
                                                   typeMapper.mapType(superClassDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName()), state.getTypeMapper());
                generateDelegates(superClass, delegateContext, overridden);
            }
        }

        final ClassDescriptor outerDescriptor = getOuterClassDescriptor();
        if (outerDescriptor != null && outerDescriptor.getKind() != ClassKind.OBJECT) {
            final Type type = typeMapper.mapType(outerDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION);
            String interfaceDesc = type.getDescriptor();
            final String fieldName = "this$0";
            v.newField(myClass, Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, interfaceDesc, null, null);
            iv.load(0, classType);
            iv.load(frameMap.getOuterThisIndex(), type);
            iv.putfield(classname, fieldName, interfaceDesc);

            Type outerType = typeMapper.mapType(outerDescriptor.getDefaultType());
            MethodVisitor outer = v.newMethod(myClass, Opcodes.ACC_PUBLIC, "getOuterObject", "()Ljet/JetObject;", null, null);
            outer.visitCode();
            outer.visitVarInsn(Opcodes.ALOAD, 0);
            outer.visitFieldInsn(Opcodes.GETFIELD, classname, "this$0", outerType.getDescriptor());
            outer.visitInsn(Opcodes.ARETURN);
            FunctionCodegen.endVisit(outer, "getOuterObject", myClass);
        }

        if (CodegenUtil.requireTypeInfoConstructorArg(descriptor.getDefaultType()) && kind == OwnerKind.IMPLEMENTATION) {
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            iv.load(frameMap.getTypeInfoIndex(), JetTypeMapper.TYPE_OBJECT);
            iv.invokevirtual(typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "$setTypeInfo", "(Ljet/typeinfo/TypeInfo;)V");
        }

        if(closure != null) {
            int k = outerDescriptor != null && outerDescriptor.getKind() != ClassKind.OBJECT ? 2 : 1;
            if(closure.captureReceiver != null) {
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
                iv.load(1, closure.captureReceiver);
                iv.putfield(typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "receiver$0", closure.captureReceiver.getDescriptor());
                k += closure.captureReceiver.getSize();
            }

            int l = 0;
            for (DeclarationDescriptor varDescr : closure.closure.keySet()) {
                if(varDescr instanceof VariableDescriptor && !(varDescr instanceof PropertyDescriptor)) {
                    Type sharedVarType = typeMapper.getSharedVarType(varDescr);
                    if(sharedVarType == null) {
                        sharedVarType = typeMapper.mapType(((VariableDescriptor) varDescr).getOutType());
                    }
                    iv.load(0, JetTypeMapper.TYPE_OBJECT);
                    iv.load(k, StackValue.refType(sharedVarType));
                    k += StackValue.refType(sharedVarType).getSize();
                    iv.putfield(typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "$" + varDescr.getName(), sharedVarType.getDescriptor());
                    l++;
                }
            }
        }

        int curParam = 0;
        List<JetParameter> constructorParameters = getPrimaryConstructorParameters();
        for (JetParameter parameter : constructorParameters) {
            if (parameter.getValOrVarNode() != null) {
                VariableDescriptor descriptor = paramDescrs.get(curParam);
                Type type = typeMapper.mapType(descriptor.getOutType());
                iv.load(0, classType);
                iv.load(frameMap.getIndex(descriptor), type);
                iv.putfield(classname, descriptor.getName(), type.getDescriptor());
            }
            curParam++;
        }

        generateInitializers(codegen, iv);

        generateTraitMethods(codegen);

        mv.visitInsn(Opcodes.RETURN);
        FunctionCodegen.endVisit(mv, "constructor", myClass);

        FunctionCodegen.generateDefaultIfNeeded(constructorContext, state, v, constructorMethod, constructorDescriptor, OwnerKind.IMPLEMENTATION);
    }

    private void generateTraitMethods(ExpressionCodegen codegen) {
        if(!(myClass instanceof JetClass) || ((JetClass)myClass).isTrait() || ((JetClass)myClass).hasModifier(JetTokens.ABSTRACT_KEYWORD))
            return;

        for (CallableDescriptor callableDescriptor : OverridingUtil.getEffectiveMembers(descriptor)) {
            if(callableDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor fun = (FunctionDescriptor) callableDescriptor;
                DeclarationDescriptor containingDeclaration = fun.getContainingDeclaration();
                if(containingDeclaration instanceof ClassDescriptor) {
                    ClassDescriptor declaration = (ClassDescriptor) containingDeclaration;
                    PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declaration);
                    if(psiElement instanceof JetClass) {
                        JetClass jetClass = (JetClass) psiElement;
                        if(jetClass.isTrait()) {
                            int flags = Opcodes.ACC_PUBLIC; // TODO.

                            Method function = typeMapper.mapSignature(fun.getName(), fun).getAsmMethod();
                            Method functionOriginal = typeMapper.mapSignature(fun.getName(), fun.getOriginal()).getAsmMethod();

                            final MethodVisitor mv = v.newMethod(myClass, flags, function.getName(), function.getDescriptor(), null, null);
                            if (v.generateCode()) {
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

                                JetType jetType = TraitImplBodyCodegen.getSuperClass(declaration, bindingContext);
                                Type type = typeMapper.mapType(jetType);
                                if(type.getInternalName().equals("java/lang/Object")) {
                                    jetType = declaration.getDefaultType();
                                    type = typeMapper.mapType(jetType);
                                }

                                String fdescriptor = functionOriginal.getDescriptor().replace("(","(" +  type.getDescriptor());
                                iv.invokestatic(typeMapper.mapType(((ClassDescriptor) fun.getContainingDeclaration()).getDefaultType(), OwnerKind.TRAIT_IMPL).getInternalName(), function.getName(), fdescriptor);
                                if(function.getReturnType().getSort() == Type.OBJECT) {
                                    iv.checkcast(function.getReturnType());
                                }
                                iv.areturn(function.getReturnType());
                                FunctionCodegen.endVisit(iv, "trait method", bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, fun));
                            }

                            FunctionCodegen.generateBridgeIfNeeded(context, state, v, function, fun, kind);
                        }
                    }
                }
            }
        }
    }
    
    @Nullable
    private ClassDescriptor getOuterClassDescriptor() {
        if (myClass.getParent() instanceof JetClassObject) {
            return null;
        }

        return CodegenUtil.getOuterClassDescriptor(descriptor);
    }

    private void generateDelegatorToConstructorCall(InstructionAdapter iv, ExpressionCodegen codegen, JetCallElement constructorCall,
                                                    ConstructorDescriptor constructorDescriptor,
                                                    ConstructorFrameMap frameMap) {
        ClassDescriptor classDecl = constructorDescriptor.getContainingDeclaration();
        Type type;
        type = typeMapper.mapType(classDecl.getDefaultType(), OwnerKind.IMPLEMENTATION);

        iv.load(0, type);

        if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
            iv.load(frameMap.getOuterThisIndex(), typeMapper.mapType(((ClassDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), OwnerKind.IMPLEMENTATION));
        }

        CallableMethod method = typeMapper.mapToCallableMethod(constructorDescriptor, kind);
        codegen.invokeMethodWithArguments(method, constructorCall, StackValue.none());
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetSecondaryConstructor) {
            generateSecondaryConstructor((JetSecondaryConstructor) declaration);
        }
        else if (declaration instanceof JetClassObject) {
            // done earlier in order to have accessors
        }
        else if (declaration instanceof JetEnumEntry && !((JetEnumEntry) declaration).hasPrimaryConstructor()) {
            String name = declaration.getName();
            final String desc = "L" + typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName() + ";";
            v.newField(declaration, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, name, desc, null, null);
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
            String implClass = typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();

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
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, superCall.getCalleeExpression().getConstructorReferenceExpression());
                    CallableMethod method = typeMapper.mapToCallableMethod(constructorDescriptor, OwnerKind.IMPLEMENTATION);
                    codegen.invokeMethodWithArguments(method, superCall, StackValue.none());
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

    private void generateSecondaryConstructor(JetSecondaryConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, constructor);
        if (constructorDescriptor == null) {
            throw new UnsupportedOperationException("failed to get descriptor for secondary constructor");
        }
        CallableMethod method = typeMapper.mapToCallableMethod(constructorDescriptor, kind);
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.newMethod(constructor, flags, "<init>", method.getSignature().getAsmMethod().getDescriptor(), null, null);
        if (v.generateCode()) {
            mv.visitCode();

            ConstructorFrameMap frameMap = new ConstructorFrameMap(method, constructorDescriptor, descriptor, kind);

            final InstructionAdapter iv = new InstructionAdapter(mv);
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state);

            for (JetDelegationSpecifier initializer : constructor.getInitializers()) {
                if (initializer instanceof JetDelegatorToThisCall) {
                    JetDelegatorToThisCall thisCall = (JetDelegatorToThisCall) initializer;
                    DeclarationDescriptor thisDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, thisCall.getThisReference());
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
            FunctionCodegen.endVisit(mv, "constructor", null);
        }
    }

    protected void generateInitializers(ExpressionCodegen codegen, InstructionAdapter iv) {
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(BindingContext.VARIABLE, declaration);
                if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null) {
                        CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, initializer);
                        if(compileTimeValue != null) {
                            assert compileTimeValue != null;
                            Object value = compileTimeValue.getValue();
                            Type type = typeMapper.mapType(propertyDescriptor.getOutType());
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
                        codegen.intermediateValueForProperty(propertyDescriptor, false, null).store(iv);
                    }

                }
            }
            else if (declaration instanceof JetClassInitializer) {
                codegen.gen(((JetClassInitializer) declaration).getBody(), Type.VOID_TYPE);
            }
        }
    }

    protected void generateDelegates(JetClass toClass, CodegenContext delegateContext, Set<FunctionDescriptor> overriden) {
        final FunctionCodegen functionCodegen = new FunctionCodegen(delegateContext, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(delegateContext, v, functionCodegen, state);

        for (JetDeclaration declaration : toClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration);
            }
            else if (declaration instanceof JetNamedFunction) {
                if (!overriden.contains(bindingContext.get(BindingContext.FUNCTION, declaration))) {
                    functionCodegen.gen((JetNamedFunction) declaration);
                }
            }
        }

        for (JetParameter p : toClass.getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    propertyCodegen.generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, p);
                    if (propertyDescriptor.isVar()) {
                        propertyCodegen.generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, p);
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

        JetType defaultType = descriptor.getDefaultType();
        if(CodegenUtil.requireTypeInfoConstructorArg(defaultType)) {
            if(!CodegenUtil.hasDerivedTypeInfoField(defaultType)) {
                v.newField(myClass, Opcodes.ACC_PROTECTED, "$typeInfo", "Ljet/typeinfo/TypeInfo;", null, null);

                MethodVisitor mv = v.newMethod(myClass, Opcodes.ACC_PUBLIC, "getTypeInfo", "()Ljet/typeinfo/TypeInfo;", null, null);
                if (v.generateCode()) {
                    mv.visitCode();
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    String owner = typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
                    iv.load(0, JetTypeMapper.TYPE_OBJECT);
                    iv.getfield(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
                    iv.areturn(JetTypeMapper.TYPE_TYPEINFO);
                    FunctionCodegen.endVisit(iv, "getTypeInfo", myClass);
                }

                mv = v.newMethod(myClass, Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL, "$setTypeInfo", "(Ljet/typeinfo/TypeInfo;)V", null, null);
                if (v.generateCode()) {
                    mv.visitCode();
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    String owner = typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
                    iv.load(0, JetTypeMapper.TYPE_OBJECT);
                    iv.load(1, JetTypeMapper.TYPE_OBJECT);
                    iv.putfield(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
                    mv.visitInsn(Opcodes.RETURN);
                    FunctionCodegen.endVisit(iv, "$setTypeInfo", myClass);
                }
            }
        }
        else {
            genGetStaticGetTypeInfoMethod();
            staticTypeInfoField();
        }
    }

    private void genGetStaticGetTypeInfoMethod() {
        final MethodVisitor mv = v.newMethod(myClass, Opcodes.ACC_PUBLIC, "getTypeInfo", "()Ljet/typeinfo/TypeInfo;", null, null);
        if (v.generateCode()) {
            mv.visitCode();
            InstructionAdapter v = new InstructionAdapter(mv);
            String owner = typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
            v.getstatic(owner, "$staticTypeInfo", "Ljet/typeinfo/TypeInfo;");
            v.areturn(JetTypeMapper.TYPE_TYPEINFO);
            FunctionCodegen.endVisit(v, "getTypeInfo", myClass);
        }
    }

    private void staticTypeInfoField() {
        v.newField(myClass, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, "$staticTypeInfo", "Ljet/typeinfo/TypeInfo;", null, null);
        staticInitializerChunks.add(new CodeChunk() {
            @Override
            public void generate(InstructionAdapter v) {
                v.aconst(typeMapper.mapType(descriptor.getDefaultType(), OwnerKind.IMPLEMENTATION));
                v.iconst(0);
                v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z)Ljet/typeinfo/TypeInfo;");
                v.putstatic(typeMapper.mapType(descriptor.getDefaultType(), kind).getInternalName(), "$staticTypeInfo", "Ljet/typeinfo/TypeInfo;");
            }
        });
    }
}
