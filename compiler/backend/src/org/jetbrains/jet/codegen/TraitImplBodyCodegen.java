package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class TraitImplBodyCodegen extends ClassBodyCodegen {
    public TraitImplBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
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

    @Override
    protected void genNamedFunction(JetNamedFunction declaration, FunctionCodegen functionCodegen) {
        super.genNamedFunction(declaration, functionCodegen);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
