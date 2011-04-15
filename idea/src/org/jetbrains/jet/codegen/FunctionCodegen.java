package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
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
    private final JetTypeMapper typeMapper;

    public FunctionCodegen(ClassVisitor v, JetStandardLibrary standardLibrary, BindingContext bindingContext) {
        this.v = v;
        this.bindingContext = bindingContext;
        this.standardLibrary = standardLibrary;
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
    }

    public void genInNamespace(JetFunction f) {
        gen(f);
    }

    public void genInInterface(JetFunction f) {

    }

    public void genInImplementation(JetFunction f) {

    }

    public void genInDelegatingImplementation(JetFunction f) {

    }

    private void gen(JetFunction f) {
        Method method = typeMapper.mapSignature(f);
        final MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.getName(), method.getDescriptor(), null, null);
        mv.visitCode();
        final JetExpression bodyExpression = f.getBodyExpression();
        FrameMap frameMap = new FrameMap();

        List<ValueParameterDescriptor> parameDescrs = bindingContext.getFunctionDescriptor(f).getUnsubstitutedValueParameters();

        Type[] argTypes = method.getArgumentTypes();
        for (int i = 0; i < parameDescrs.size(); i++) {
            ValueParameterDescriptor parameter = parameDescrs.get(i);
            frameMap.enter(parameter, argTypes[i].getSize());
        }

        ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap, typeMapper, method.getReturnType());
        bodyExpression.accept(codegen);
        generateReturn(mv, bodyExpression, codegen);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateReturn(MethodVisitor mv, JetExpression bodyExpression, ExpressionCodegen codegen) {
        if (!endsWithReturn(bodyExpression)) {
            final JetType expressionType = bindingContext.getExpressionType(bodyExpression);
            if (expressionType == null || expressionType.equals(JetStandardClasses.getUnitType())) {
                mv.visitInsn(Opcodes.RETURN);
            }
            else {
                codegen.returnTopOfStack();
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
}
