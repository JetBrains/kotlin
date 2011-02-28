package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
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
    private final BindingContext bindingContext;
    private final JetStandardLibrary standardLibrary;

    public FunctionCodegen(ClassVisitor v, JetStandardLibrary standardLibrary, BindingContext bindingContext) {
        this.v = v;
        this.bindingContext = bindingContext;
        this.standardLibrary = standardLibrary;
    }

    public void gen(JetFunction f, JetNamespace owner) {
        final List<JetParameter> parameters = f.getValueParameters();
        Type[] parameterTypes = new Type[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterTypes[i] = mapTypeReference(parameters.get(i).getTypeReference());
        }
        final JetTypeReference returnTypeRef = f.getReturnTypeRef();
        Type returnType;
        if (returnTypeRef == null) {
            final FunctionDescriptor functionDescriptor = bindingContext.getFunctionDescriptor(f);
            final org.jetbrains.jet.lang.types.Type type = functionDescriptor.getUnsubstitutedReturnType();
            if (type.equals(JetStandardClasses.getUnitType())) {
                returnType = Type.VOID_TYPE;
            }
            else if (type.equals(standardLibrary.getIntType())) {
                returnType = Type.getType(Integer.class);
            }
            else {
                throw new UnsupportedOperationException("don't know how to map type " + type);
            }
        }
        else {
            returnType = mapTypeReference(returnTypeRef);
        }
        Method method = new Method(f.getName(), returnType, parameterTypes);
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.getName(), method.getDescriptor(), null, null);
        mv.visitCode();
        final JetExpression bodyExpression = f.getBodyExpression();
        bodyExpression.accept(new ExpressionCodegen(mv, bindingContext));
        generateReturn(mv, bodyExpression);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateReturn(MethodVisitor mv, JetExpression bodyExpression) {
        if (!endsWithReturn(bodyExpression)) {
            final org.jetbrains.jet.lang.types.Type expressionType = bindingContext.getExpressionType(bodyExpression);
            if (expressionType.equals(JetStandardClasses.getUnitType())) {
                mv.visitInsn(Opcodes.RETURN);
            }
            else {
                mv.visitInsn(Opcodes.ARETURN);
            }
        }
    }

    private static boolean endsWithReturn(JetExpression bodyExpression) {
        if (bodyExpression instanceof JetBlockExpression) {
            final List<JetElement> statements = ((JetBlockExpression) bodyExpression).getStatements();
            return statements.size() > 0 && statements.get(statements.size()-1) instanceof JetReturnExpression;
        }
        return false;
    }

    private Type mapTypeReference(JetTypeReference typeRef) {
        if (typeRef == null) {
            throw new UnsupportedOperationException("Cannot evaluate type for parameter with no type ref");
        }
        final JetTypeElement typeElement = typeRef.getTypeElement();
        if (typeElement instanceof JetUserType) {
            final JetUserType userType = (JetUserType) typeElement;
            return mapType(userType.getReferencedName(), userType.getTypeArguments());
        }

        throw new UnsupportedOperationException("Unknown type " + typeRef);
    }

    private Type mapType(final String name, final List<JetTypeProjection> typeArguments) {
        if ("Array".equals(name)) {
            if (typeArguments.size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            final JetTypeReference elementTypeRef = typeArguments.get(0).getTypeReference();
            Type elementType = mapTypeReference(elementTypeRef);
            return Type.getType("[" + elementType.getDescriptor());
        }

        if ("String".equals(name)) {
            return Type.getType(String.class);
        }
        if ("Int".equals(name)) {
            return Type.getType(Integer.class);
        }
        throw new UnsupportedOperationException("Unknown type " + name);
    }

    public void gen(JetFunction f, JetClass owner) {

    }
}
