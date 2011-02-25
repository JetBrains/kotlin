package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author max
 */
public class FunctionCodegen {
    private final ClassVisitor v;

    public FunctionCodegen(ClassVisitor v) {
        this.v = v;
    }

    public void gen(JetFunction f, JetNamespace owner) {
        final List<JetParameter> parameters = f.getValueParameters();
        Type[] parameterTypes = new Type[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterTypes[i] = mapTypeReference(parameters.get(i).getTypeReference());
        }
        Method method = new Method(f.getName(), Type.VOID_TYPE, parameterTypes);
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.getName(), method.getDescriptor(), null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitEnd();
    }

    private Type mapTypeReference(JetTypeReference typeRef) {
        if (typeRef == null) {
            throw new UnsupportedOperationException("Cannot evaluate type for parameter with no type ref");
        }
        final JetTypeElement typeElement = typeRef.getTypeElement();
        if (typeElement instanceof JetUserType) {
            final String referencedName = ((JetUserType) typeElement).getReferencedName();
            if ("Array".equals(referencedName)) {
                final List<JetTypeProjection> typeArguments = ((JetUserType) typeElement).getTypeArguments();
                if (typeArguments.size() != 1) {
                    throw new UnsupportedOperationException("arrays must have one type argument");
                }
                final JetTypeReference elementTypeRef = typeArguments.get(0).getTypeReference();
                Type elementType = mapTypeReference(elementTypeRef);
                return Type.getType("[" + elementType.getDescriptor());
            }

            if ("String".equals(referencedName)) {
                return Type.getType(String.class);
            }
        }

        throw new UnsupportedOperationException("Unknown type " + typeRef);
    }

    public void gen(JetFunction f, JetClass owner) {

    }
}
