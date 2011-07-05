package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class InterfaceBodyCodegen extends ClassBodyCodegen {
    public InterfaceBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
        assert context.getContextKind() == OwnerKind.INTERFACE;
    }

    protected void generateDeclaration() {
        Set<String> superInterfaces = getSuperInterfaces();

        String fqName = JetTypeMapper.jvmNameForInterface(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                fqName,
                null,
                "java/lang/Object",
                superInterfaces.toArray(new String[superInterfaces.size()])
        );
    }

    private Set<String> getSuperInterfaces() {
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<String> superInterfaces = new LinkedHashSet<String>();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = state.getBindingContext().resolveTypeReference(specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = state.getBindingContext().getDeclarationPsiElement(superClassDescriptor);

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
}
