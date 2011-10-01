package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class TraitImplBodyCodegen extends ClassBodyCodegen {
    public TraitImplBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
    }

    //todo not needed when frontend will be able to calculate properly
    static JetType getSuperClass(ClassDescriptor myClassDescr, BindingContext bindingContext) {
        JetClassOrObject myClass = (JetClassOrObject) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, myClassDescr);
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperClass || specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                final PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
                if (declaration != null) {
                    if (declaration instanceof PsiClass) {
                        if (!((PsiClass) declaration).isInterface()) {
                            return superClassDescriptor.getDefaultType();
                        }
                    }
                    else if(declaration instanceof JetClass) {
                        if(!((JetClass) declaration).isTrait()) {
                            return superClassDescriptor.getDefaultType();
                        }
                    }
                }
            }
        }
        return JetStandardClasses.getAnyType();
    }

    @Override
    protected void generateDeclaration() {
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                jvmName(),
                null,
                "java/lang/Object",
                new String[0]
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    private String jvmName() {
        return state.getTypeMapper().jvmName(descriptor, OwnerKind.TRAIT_IMPL);
    }
}
