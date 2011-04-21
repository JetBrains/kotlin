package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.PropertyDescriptor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public class ClassCodegen {
    private final Project project;
    private final BindingContext bindingContext;
    private final Codegens factory;

    public ClassCodegen(Project project, Codegens factory, BindingContext bindingContext) {
        this.project = project;
        this.factory = factory;
        this.bindingContext = bindingContext;
    }

    public void generate(JetClass aClass) {
        generateInterface(aClass);
        generateImplementation(aClass, OwnerKind.IMPLEMENTATION);
        generateImplementation(aClass, OwnerKind.DELEGATING_IMPLEMENTATION);
    }

    private void generateInterface(JetClass aClass) {
        ClassDescriptor descriptor =  bindingContext.getClassDescriptor(aClass);

        Set<String> superInterfaces = getSuperInterfaces(aClass);

        String fqName = JetTypeMapper.jvmNameForInterface(descriptor);
        ClassVisitor v = factory.forClassInterface(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                fqName,
                null,
                "java/lang/Object",
                superInterfaces.toArray(new String[superInterfaces.size()])
        );

        generateClassBody(aClass, v, OwnerKind.INTERFACE);

        v.visitEnd();
    }

    private void generateImplementation(JetClass aClass, OwnerKind kind) {
        ClassDescriptor descriptor =  bindingContext.getClassDescriptor(aClass);

        String superClass = getSuperClass(aClass, kind);

        ClassVisitor v = kind == OwnerKind.IMPLEMENTATION ? factory.forClassImplementation(descriptor) : factory.forClassDelegatingImplementation(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                JetTypeMapper.jvmNameForImplementation(descriptor),
                null,
                superClass,
                new String[] {JetTypeMapper.jvmNameForInterface(descriptor)}
        );

        generatePrimaryConstructor(aClass, v, kind);

        generateClassBody(aClass, v, kind);

        v.visitEnd();
    }

    private String getSuperClass(JetClass aClass, OwnerKind kind) {
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<ClassDescriptor> superInterfaces = new LinkedHashSet<ClassDescriptor>();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = bindingContext.getDeclarationPsiElement(superClassDescriptor);

            if (superPsi instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) superPsi;
                String fqn = psiClass.getQualifiedName();
                if (!psiClass.isInterface()) {
                    if (superClassName == null) {
                        superClassName = fqn.replace('.', '/');
                    }
                    else {
                        throw new RuntimeException("Cannot determine single class to inherit from");
                    }
                }
            }
            else {
                superInterfaces.add(superClassDescriptor);
            }
        }

        if (superClassName != null) {
            return superClassName;
        }

        if (superInterfaces.size() > 0) {
            ClassDescriptor first = superInterfaces.iterator().next();
            return kind == OwnerKind.IMPLEMENTATION ? JetTypeMapper.jvmNameForImplementation(first) : JetTypeMapper.jvmNameForDelegatingImplementation(first);
        }

        return "java/lang/Object";
    }

    private Set<String> getSuperInterfaces(JetClass aClass) {
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<String> superInterfaces = new LinkedHashSet<String>();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = bindingContext.getDeclarationPsiElement(superClassDescriptor);

            if (superPsi instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) superPsi;
                String fqn = psiClass.getQualifiedName();
                if (psiClass.isInterface()) {
                    superInterfaces.add(fqn);
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
                superInterfaces.add(JetTypeMapper.jvmNameForInterface(superClassDescriptor));
            }
        }
        return superInterfaces;
    }

    private void generatePrimaryConstructor(JetClass aClass, ClassVisitor v, OwnerKind kind) {
        int flags = Opcodes.ACC_PUBLIC; // TODO
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        final MethodVisitor mv = v.visitMethod(flags, "<init>", method.getDescriptor(), null, null);
        mv.visitCode();

        FrameMap frameMap = new FrameMap();
        frameMap.enterTemp();   // this

        final InstructionAdapter iv = new InstructionAdapter(mv);
        String superClass = getSuperClass(aClass, kind);
        iv.load(0, Type.getType("L" + superClass + ";"));
        iv.invokespecial(superClass, "<init>", method.getDescriptor());

        final JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        final JetTypeMapper typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap,
                typeMapper, Type.VOID_TYPE);
        generateInitializers(aClass, kind, codegen, iv, typeMapper);

        iv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateInitializers(JetClass aClass, OwnerKind kind, ExpressionCodegen codegen, InstructionAdapter iv, JetTypeMapper typeMapper) {
        for (JetDeclaration declaration : aClass.getDeclarations()) {
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

    private void generateClassBody(JetClass aClass, ClassVisitor v, OwnerKind kind) {
        final JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, standardLibrary, bindingContext);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, standardLibrary, bindingContext, functionCodegen);

        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, kind);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.gen((JetFunction) declaration, kind);
            }
        }
    }
}
