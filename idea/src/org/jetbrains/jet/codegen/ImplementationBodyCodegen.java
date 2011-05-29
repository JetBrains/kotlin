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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author max
 * @author yole
 */
public class ImplementationBodyCodegen extends ClassBodyCodegen {
    public ImplementationBodyCodegen(BindingContext bindingContext, JetStandardLibrary stdlib, JetClass aClass, OwnerKind kind, ClassVisitor v) {
        super(bindingContext, stdlib, aClass, kind, v);
    }

    @Override
    protected void generateDeclaration() {
        String superClass = getSuperClass();

        final String defaultInterfaceName = JetTypeMapper.jvmNameForInterface(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                JetTypeMapper.jetJvmName(descriptor, kind),
                null,
                superClass,
                new String[]{"jet/JetObject", defaultInterfaceName}
        );
    }

    private String getSuperClass() {
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if (delegationSpecifiers.isEmpty()) return "java/lang/Object";

        JetDelegationSpecifier first = delegationSpecifiers.get(0);
        if (first instanceof JetDelegatorToSuperClass) {
            JetType superType = bindingContext.resolveTypeReference(first.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = bindingContext.getDeclarationPsiElement(superClassDescriptor);
            if (superPsi instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) superPsi;
                String fqn = psiClass.getQualifiedName();
                if (!psiClass.isInterface()) {
                    return fqn.replace('.', '/');
                }
            }
        }
        else if (first instanceof JetDelegatorToSuperCall) {
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

        generateStaticInitializer();

        generatePrimaryConstructor();

        generateGetTypeInfo();
    }

    private void generatePrimaryConstructor() {
        ConstructorDescriptor constructorDescriptor = bindingContext.getConstructorDescriptor(myClass);
        if (constructorDescriptor == null) return;

        Method method = typeMapper.mapConstructorSignature(constructorDescriptor, kind);
        int flags = Opcodes.ACC_PUBLIC; // TODO
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getDescriptor(), null, null);
        mv.visitCode();

        Type[] argTypes = method.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor.getUnsubstitutedValueParameters();

        FrameMap frameMap = new FrameMap();
        frameMap.enterTemp();   // this

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap, typeMapper, null, Type.VOID_TYPE, descriptor, kind);

        String classname = typeMapper.jvmName(descriptor, kind);
        final Type classType = Type.getType("L" + classname + ";");

        List<JetDelegationSpecifier> specifiers = myClass.getDelegationSpecifiers();

        if (specifiers.isEmpty() || !(specifiers.get(0) instanceof JetDelegatorToSuperCall)) {
            String superClass = getSuperClass();
            iv.load(0, Type.getType("L" + superClass + ";"));
            iv.invokespecial(superClass, "<init>", /* TODO super constructor descriptor */"()V");
        }

        int index = 0;
        for (ClassDescriptor outerClassDescriptor : JetTypeMapper.getOuterClassDescriptors(descriptor)) {
            final Type type = JetTypeMapper.jetInterfaceType(outerClassDescriptor);
            String interfaceDesc = type.getDescriptor();
            final String fieldName = "this$" + index;
            v.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, interfaceDesc, null, null);
            iv.load(0, classType);
            iv.load(index + 1, type);
            iv.putfield(classname, fieldName, interfaceDesc);
            frameMap.enterTemp();
        }

        if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            String interfaceDesc = JetTypeMapper.jetInterfaceType(descriptor).getDescriptor();
            v.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "$this", interfaceDesc, /*TODO*/null, null);
            iv.load(1, argTypes[0]);
            iv.putfield(classname, "$this", interfaceDesc);
            frameMap.enterTemp();
        }

        for (int i = 0; i < paramDescrs.size(); i++) {
            ValueParameterDescriptor parameter = paramDescrs.get(i);
            frameMap.enter(parameter, argTypes[i].getSize());
        }

        int firstTypeParameter = -1;
        int typeParamCount = descriptor.getTypeConstructor().getParameters().size();
        if (kind == OwnerKind.IMPLEMENTATION) {
            if (typeParamCount > 0) {
                firstTypeParameter = frameMap.enterTemp();
                for (int i = 1; i < typeParamCount; i++) {
                    frameMap.enterTemp();
                }
            }
        }

        HashSet<FunctionDescriptor> overriden = new HashSet<FunctionDescriptor>();
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetFunction) {
                overriden.addAll(bindingContext.getFunctionDescriptor((JetFunction) declaration).getOverriddenFunctions());
            }
        }


        int n = 0;
        for (JetDelegationSpecifier specifier : specifiers) {
            boolean delegateOnStack = specifier instanceof JetDelegatorToSuperCall && n > 0 ||
                                      specifier instanceof JetDelegatorByExpressionSpecifier ;

            if (delegateOnStack) {
                iv.load(0, classType);
            }

            if (specifier instanceof JetDelegatorToSuperCall) {
                JetDelegatorToSuperCall superCall = (JetDelegatorToSuperCall) specifier;
                ConstructorDescriptor constructorDescriptor1 = bindingContext.resolveSuperConstructor(superCall);

                ClassDescriptor classDecl = constructorDescriptor1.getContainingDeclaration();
                boolean isDelegating = kind == OwnerKind.DELEGATING_IMPLEMENTATION;
                Type type = isDelegating ? JetTypeMapper.jetDelegatingImplementationType(classDecl) : JetTypeMapper.jetImplementationType(classDecl);

                if (n > 0) {
                    if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
                        codegen.thisToStack();
                    }
                }

                if (n == 0) {
                    iv.load(0, type);
                }
                else {
                    iv.anew(type);
                    iv.dup();
                }

                Method method1 = typeMapper.mapConstructorSignature(constructorDescriptor1, kind);
                final Type[] argTypes1 = method1.getArgumentTypes();
                List<JetArgument> args = superCall.getValueArguments();
                for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
                    JetArgument arg = args.get(i);
                    codegen.gen(arg.getArgumentExpression(), argTypes1[i]);
                }

                iv.invokespecial(type.getClassName(), "<init>", method1.getDescriptor());
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
                                JetTypeMapper.jvmNameForInterface(superClassDescriptor)), overriden);
            }

            n++;
        }

        if (firstTypeParameter > 0 && kind == OwnerKind.IMPLEMENTATION) {
            generateTypeInfoInitializer(firstTypeParameter, typeParamCount, iv);
        }

        generateInitializers(codegen, iv);

        int curParam = 0;
        List<JetParameter> constructorParameters = myClass.getPrimaryConstructorParameters();
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

        iv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateTypeInfoInitializer(int firstTypeParameter, int typeParamCount, InstructionAdapter iv) {
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

    private void generateInitializers(ExpressionCodegen codegen, InstructionAdapter iv) {
        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.getVariableDescriptor((JetProperty) declaration);
                if (bindingContext.hasBackingField(propertyDescriptor)) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null) {
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        codegen.genToJVMStack(initializer);
                        codegen.intermediateValueForProperty(propertyDescriptor, false).store(iv);
                    }

                }
            }
        }
    }

    private void generateDelegates(JetClass inClass, JetClass toClass, OwnerKind kind, Set<FunctionDescriptor> overriden) {
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
        if (descriptor.getTypeConstructor().getParameters().size() > 0) {
            // we will have a dynamic type info field
            return;
        }
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        mv.visitCode();

        InstructionAdapter v = new InstructionAdapter(mv);
        ClassCodegen.newTypeInfo(v, Type.getObjectType(JetTypeMapper.jvmNameForInterface(descriptor)));
        v.putstatic(JetTypeMapper.jvmNameForImplementation(descriptor), "$typeInfo", "Ljet/typeinfo/TypeInfo;");

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
