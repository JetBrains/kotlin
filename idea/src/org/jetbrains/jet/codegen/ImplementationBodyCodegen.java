package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
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
    public ImplementationBodyCodegen(BindingContext bindingContext, JetStandardLibrary stdlib, JetClassOrObject aClass,
                                     OwnerKind kind, ClassVisitor v) {
        super(bindingContext, stdlib, aClass, kind, v);
    }

    @Override
    protected void generateDeclaration() {
        String superClass = getSuperClass();

        List<String> interfaces = new ArrayList<String>();
        interfaces.add("jet/JetObject");
        if (!(myClass instanceof JetObjectDeclaration)) {
            interfaces.add(JetTypeMapper.jvmNameForInterface(descriptor));
        }
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                jvmName(),
                null,
                superClass,
                interfaces.toArray(new String[interfaces.size()])
        );
    }

    private String jvmName() {
        return JetTypeMapper.jetJvmName(descriptor, kind);
    }

    protected String getSuperClass() {
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if (delegationSpecifiers.isEmpty()) return "java/lang/Object";

        JetDelegationSpecifier first = delegationSpecifiers.get(0);
        if (first instanceof JetDelegatorToSuperClass || first instanceof JetDelegatorToSuperCall) {
            JetType superType = bindingContext.resolveTypeReference(first.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            return typeMapper.jvmName(superClassDescriptor, kind);
        }

        return "java/lang/Object";
    }

    @Override
    protected void generateSyntheticParts() {
        int typeinfoStatic = descriptor.getTypeConstructor().getParameters().size() > 0 ? 0 : Opcodes.ACC_STATIC;
        v.visitField(Opcodes.ACC_PRIVATE | typeinfoStatic, "$typeInfo", "Ljet/typeinfo/TypeInfo;", null, null);

        if (myClass instanceof JetObjectDeclaration) {
            Type type = JetTypeMapper.jetImplementationType(descriptor);
            v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$instance", type.getDescriptor(), null, null);
        }

        generateStaticInitializer();

        try {
            generatePrimaryConstructor();
        }
        catch(RuntimeException e) {
            throw new RuntimeException("Error generating primary constructor of class " + myClass.getName() + " with kind " + kind, e);
        }

        generateGetTypeInfo();
    }

    protected void generatePrimaryConstructor() {
        ConstructorDescriptor constructorDescriptor = bindingContext.getConstructorDescriptor((JetElement) myClass);
        if (constructorDescriptor == null && !(myClass instanceof JetObjectDeclaration)) return;

        Method method;
        if (myClass instanceof JetObjectDeclaration) {
            method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        }
        else {
            method = typeMapper.mapConstructorSignature(constructorDescriptor, kind);
        }
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getDescriptor(), null, null);
        mv.visitCode();

        Type[] argTypes = method.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor != null
                ? constructorDescriptor.getUnsubstitutedValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(typeMapper, constructorDescriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap, typeMapper, Type.VOID_TYPE,
                descriptor, kind, StackValue.local(0, typeMapper.jvmType(descriptor, kind)));

        String classname = typeMapper.jvmName(descriptor, kind);
        final Type classType = Type.getType("L" + classname + ";");

        List<JetDelegationSpecifier> specifiers = myClass.getDelegationSpecifiers();

        if (specifiers.isEmpty() || !(specifiers.get(0) instanceof JetDelegatorToSuperCall)) {
            // TODO correct calculation of super class
            String superClass = "java/lang/Object";
            if (!specifiers.isEmpty()) {
                final JetType superType = bindingContext.resolveTypeReference(specifiers.get(0).getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if (superClassDescriptor.hasConstructors()) {
                    superClass = getSuperClass();
                }
            }
            iv.load(0, Type.getType("L" + superClass + ";"));
            iv.invokespecial(superClass, "<init>", /* TODO super constructor descriptor */"()V");
        }

        final DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        if (outerDescriptor instanceof ClassDescriptor) {
            final ClassDescriptor outerClassDescriptor = (ClassDescriptor) outerDescriptor;
            final Type type = JetTypeMapper.jetImplementationType(outerClassDescriptor);
            codegen.addOuterThis(outerClassDescriptor, StackValue.local(frameMap.getOuterThisIndex(), type));
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
                overridden.addAll(bindingContext.getFunctionDescriptor((JetFunction) declaration).getOverriddenFunctions());
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
                ConstructorDescriptor constructorDescriptor1 = bindingContext.resolveSuperConstructor((JetDelegatorToSuperCall) specifier);
                generateDelegatorToConstructorCall(iv, codegen, (JetDelegatorToSuperCall) specifier, constructorDescriptor1, n == 0, frameMap);
            }
            else if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                codegen.genToJVMStack(((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression());
            }

            if (delegateOnStack) {
                JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                String delegateField = "$delegate_" + n;
                Type fieldType = JetTypeMapper.jetInterfaceType(superClassDescriptor);
                String fieldDesc = fieldType.getDescriptor();
                v.visitField(Opcodes.ACC_PRIVATE, delegateField, fieldDesc, /*TODO*/null, null);
                iv.putfield(classname, delegateField, fieldDesc);

                JetClass superClass = (JetClass) bindingContext.getDeclarationPsiElement(superClassDescriptor);
                generateDelegates(myClass, superClass,
                        new OwnerKind.DelegateKind(StackValue.field(fieldType, classname, delegateField, false),
                                JetTypeMapper.jvmNameForInterface(superClassDescriptor)), overridden);
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
                Type type = typeMapper.mapType(descriptor.getOutType());
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

    private void generateDelegatorToConstructorCall(InstructionAdapter iv, ExpressionCodegen codegen, JetCall constructorCall,
                                                    ConstructorDescriptor constructorDescriptor, boolean isJavaSuperclass,
                                                    ConstructorFrameMap frameMap) {
        ClassDescriptor classDecl = constructorDescriptor.getContainingDeclaration();
        boolean isDelegating = kind == OwnerKind.DELEGATING_IMPLEMENTATION;
        PsiElement declaration = bindingContext.getDeclarationPsiElement(classDecl);
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
            iv.load(frameMap.getDelegateThisIndex(), typeMapper.jvmType(classDecl, OwnerKind.INTERFACE));
        }
        if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
            iv.load(frameMap.getOuterThisIndex(), typeMapper.jvmType((ClassDescriptor) descriptor.getContainingDeclaration(), OwnerKind.IMPLEMENTATION));
        }

        Method method = typeMapper.mapConstructorSignature(constructorDescriptor, kind);
        List<ValueParameterDescriptor> valueParameters = constructorDescriptor.getUnsubstitutedValueParameters();
        List<JetArgument> args = constructorCall.getValueArguments();
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            JetArgument arg = args.get(i);
            codegen.gen(arg.getArgumentExpression(), typeMapper.mapType(valueParameters.get(i).getOutType()));
        }

        iv.invokespecial(type.getInternalName(), "<init>", method.getDescriptor());
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetConstructor) {
            generateSecondaryConstructor((JetConstructor) declaration);
        }
        else {
            super.generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }
    }

    private void generateSecondaryConstructor(JetConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = bindingContext.getConstructorDescriptor(constructor);
        if (constructorDescriptor == null) {
            throw new UnsupportedOperationException("failed to get descriptor for secondary constructor");
        }
        Method method = typeMapper.mapConstructorSignature(constructorDescriptor, kind);
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getDescriptor(), null, null);
        mv.visitCode();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(typeMapper, constructorDescriptor, kind);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap, typeMapper, Type.VOID_TYPE,
                descriptor, kind, StackValue.local(0, typeMapper.jvmType(descriptor, kind)));

        for (JetDelegationSpecifier initializer : constructor.getInitializers()) {
            if (initializer instanceof JetDelegatorToThisCall) {
                JetDelegatorToThisCall thisCall = (JetDelegatorToThisCall) initializer;
                DeclarationDescriptor thisDescriptor = bindingContext.resolveReferenceExpression(thisCall.getThisReference());
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
        iv.anew(JetTypeMapper.TYPE_TYPEINFO);
        iv.dup();

        iv.aconst(typeMapper.jvmType(descriptor, OwnerKind.INTERFACE));
        iv.iconst(typeParamCount);
        iv.newarray(JetTypeMapper.TYPE_TYPEINFO);

        for (int i = 0; i < typeParamCount; i++) {
            iv.dup();
            iv.iconst(i);
            iv.load(firstTypeParameter + i, JetTypeMapper.TYPE_OBJECT);
            iv.astore(JetTypeMapper.TYPE_OBJECT);
        }
        iv.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;[Ljet/typeinfo/TypeInfo;)V");
        iv.putfield(typeMapper.jvmName(descriptor, OwnerKind.IMPLEMENTATION), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
    }

    protected void generateInitializers(ExpressionCodegen codegen, InstructionAdapter iv) {
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.getVariableDescriptor((JetProperty) declaration);
                if (bindingContext.hasBackingField(propertyDescriptor)) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null) {
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        codegen.genToJVMStack(initializer);
                        codegen.intermediateValueForProperty(propertyDescriptor, false, false).store(iv);
                    }

                }
            }
            else if (declaration instanceof JetClassInitializer) {
                codegen.gen(((JetClassInitializer) declaration).getBody(), Type.VOID_TYPE);
            }
        }
    }

    protected void generateDelegates(JetClassOrObject inClass, JetClass toClass, OwnerKind kind, Set<FunctionDescriptor> overriden) {
        final FunctionCodegen functionCodegen = new FunctionCodegen(toClass, v, stdlib, bindingContext);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, stdlib, bindingContext, functionCodegen);

        for (JetDeclaration declaration : toClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, kind);
            }
            else if (declaration instanceof JetFunction) {
                if (!overriden.contains(bindingContext.getFunctionDescriptor((JetFunction) declaration))) {
                    functionCodegen.gen((JetFunction) declaration, kind);
                }
            }
        }

        for (JetParameter p : toClass.getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor(p);
                if (propertyDescriptor != null) {
                    propertyCodegen.generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
                    if (propertyDescriptor.isVar()) {
                        propertyCodegen.generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
                    }
                }
            }
        }
    }

    private void generateStaticInitializer() {
        boolean needTypeInfo = descriptor.getTypeConstructor().getParameters().size() == 0;
        boolean needInstance = myClass instanceof JetObjectDeclaration;
        if (!needTypeInfo && !needInstance) {
            // we will have a dynamic type info field
            return;
        }
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        mv.visitCode();

        InstructionAdapter v = new InstructionAdapter(mv);

        if (needTypeInfo) {
            ClassCodegen.newTypeInfo(v, typeMapper.jvmType(descriptor, OwnerKind.INTERFACE));
            v.putstatic(JetTypeMapper.jvmNameForImplementation(descriptor), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
        }
        if (needInstance) {
            String name = jvmName();
            v.anew(Type.getObjectType(name));
            v.dup();
            v.invokespecial(name, "<init>", "()V");
            v.putstatic(name, "$instance", JetTypeMapper.jetImplementationType(descriptor).getDescriptor());
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);

        mv.visitEnd();
    }

    private void generateGetTypeInfo() {
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC,
                "getTypeInfo",
                "()Ljet/typeinfo/TypeInfo;",
                null /* TODO */,
                null);
        mv.visitCode();
        InstructionAdapter v = new InstructionAdapter(mv);
        ExpressionCodegen.loadTypeInfo(descriptor, v);
        v.areturn(JetTypeMapper.TYPE_TYPEINFO);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
