package org.jetbrains.jet.codegen;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;
import java.util.Stack;

/**
 * @author max
 */
public class ExpressionCodegen extends JetVisitor {
    private final Stack<Label> myLoopStarts = new Stack<Label>();
    private final Stack<Label> myLoopEnds = new Stack<Label>();
    private final Stack<StackValue> myStack = new Stack<StackValue>();

    private final InstructionAdapter v;
    private final JetStandardLibrary stdlib;
    private final FrameMap myMap;
    private final JetTypeMapper typeMapper;
    private final Type returnType;
    private final BindingContext bindingContext;

    public ExpressionCodegen(MethodVisitor v, BindingContext bindingContext, JetStandardLibrary stdlib, FrameMap myMap,
                             JetTypeMapper typeMapper, Type returnType) {
        this.stdlib = stdlib;
        this.myMap = myMap;
        this.typeMapper = typeMapper;
        this.returnType = returnType;
        this.v = new InstructionAdapter(v);
        this.bindingContext = bindingContext;
    }

    private void gen(JetElement expr) {
        if (expr == null) throw new CompilationException();
        expr.accept(this);
    }

    private void gen(JetElement expr, Type type) {
        int oldStackDepth = myStack.size();
        gen(expr);
        if (myStack.size() == oldStackDepth+1) {
            StackValue value = myStack.pop();
            value.put(type, v);
        }
    }

    @Override
    public void visitExpression(JetExpression expression) {
        throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
    }

    @Override
    public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
        gen(expression.getExpression());
    }

    @Override
    public void visitAnnotatedExpression(JetAnnotatedExpression expression) {
        gen(expression.getBaseExpression());
    }

    @Override
    public void visitIfExpression(JetIfExpression expression) {
        JetType expressionType = bindingContext.getExpressionType(expression);
        Type asmType = typeMapper.mapType(expressionType);
        int oldStackDepth = myStack.size();
        gen(expression.getCondition());
        assert myStack.size() == oldStackDepth+1;

        JetExpression thenExpression = expression.getThen();
        JetExpression elseExpression = expression.getElse();

        if (thenExpression == null && elseExpression == null) {
            throw new CompilationException();
        }

        if (thenExpression == null) {
            generateSingleBranchIf(elseExpression, false);
            return;
        }

        if (elseExpression == null) {
            generateSingleBranchIf(thenExpression, true);
            return;
        }


        Label elseLabel = new Label();
        myStack.pop().condJump(elseLabel, true, v);   // == 0, i.e. false

        gen(thenExpression, asmType);

        Label endLabel = new Label();
        v.goTo(endLabel);
        v.mark(elseLabel);

        gen(elseExpression, asmType);

        v.mark(endLabel);
    }

    @Override
    public void visitWhileExpression(JetWhileExpression expression) {
        Label condition = new Label();
        myLoopStarts.push(condition);
        v.mark(condition);

        Label end = new Label();
        myLoopEnds.push(end);

        gen(expression.getCondition());
        myStack.pop().condJump(end, true, v);

        gen(expression.getBody(), Type.VOID_TYPE);
        v.goTo(condition);

        v.mark(end);
        myLoopEnds.pop();
        myLoopStarts.pop();
    }

    @Override
    public void visitDoWhileExpression(JetDoWhileExpression expression) {
        Label condition = new Label();
        v.mark(condition);
        myLoopStarts.push(condition);

        Label end = new Label();
        myLoopEnds.push(end);

        gen(expression.getBody(), Type.VOID_TYPE);

        gen(expression.getCondition());
        unboxBoolean();
        v.ifne(condition);

        v.mark(end);

        myLoopEnds.pop();
        myLoopStarts.pop();
    }

    @Override
    public void visitBreakExpression(JetBreakExpression expression) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();

        Label label = labelElement == null ? myLoopEnds.peek() : null; // TODO:

        v.goTo(label);
    }

    @Override
    public void visitContinueExpression(JetContinueExpression expression) {
        String labelName = expression.getLabelName();

        Label label = labelName == null ? myLoopStarts.peek() : null; // TODO:

        v.goTo(label);
    }

    private void unboxBoolean() {
        v.invokevirtual("java/lang/Boolean", "booleanValue", "()Z");
    }

    private void generateSingleBranchIf(JetExpression expression, boolean inverse) {
        Label endLabel = new Label();

        myStack.pop().condJump(endLabel, inverse, v);

        gen(expression, Type.VOID_TYPE);

        v.mark(endLabel);
    }

    @Override
    public void visitConstantExpression(JetConstantExpression expression) {
        myStack.push(StackValue.constant(expression.getValue()));
        /*
        Object value = element.getValue();
        v.aconst(value);

        if (value instanceof Integer) {
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        }
        else if (value instanceof Boolean) {
            v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (value instanceof Character) {
            v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        }
        else if (value instanceof Short) {
            v.invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        }
        else if (value instanceof Long) {
            v.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        }
        else if (value instanceof Byte) {
            v.invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        }
        else if (value instanceof Float) {
            v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        }
        else if (value instanceof Double) {
            v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        }
        */
    }

    @Override
    public void visitBlockExpression(JetBlockExpression expression) {
        List<JetElement> statements = expression.getStatements();
        generateBlock(statements);
    }

    @Override
    public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
        if (bindingContext.isBlock(expression)) {
            generateBlock(expression.getBody());
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate non-block function literals");
        }
    }

    private void generateBlock(List<JetElement> statements) {
        Label blockStart = new Label();
        v.mark(blockStart);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                myMap.enter(bindingContext.getPropertyDescriptor((JetProperty) statement));
            }
        }

        for (JetElement statement : statements) {
            gen(statement);
        }

        Label blockEnd = new Label();
        v.mark(blockEnd);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                JetProperty var = (JetProperty) statement;
                PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor(var);
                Type outType = typeMapper.mapType(propertyDescriptor.getOutType());

                int index = myMap.leave(propertyDescriptor);
                v.visitLocalVariable(var.getName(), outType.getDescriptor(), null, blockStart, blockEnd, index);
            }
        }
    }

    @Override
    public void visitReturnExpression(JetReturnExpression expression) {
        final JetExpression returnedExpression = expression.getReturnedExpression();
        if (returnedExpression != null) {
            gen(returnedExpression, returnType);
            v.areturn(returnType);
        }
        else {
            v.visitInsn(Opcodes.RETURN);
        }
    }

    public void returnTopOfStack() {
        if (myStack.size() > 0) {
            StackValue value = myStack.pop();
            value.put(returnType, v);
            v.areturn(returnType);
        }
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        final DeclarationDescriptor descriptor = bindingContext.resolveReferenceExpression(expression);
        PsiElement declaration = bindingContext.getDeclarationPsiElement(descriptor);
        if (declaration instanceof PsiField) {
            PsiField psiField = (PsiField) declaration;
            if (psiField.hasModifierProperty(PsiModifier.STATIC)) {
                v.visitFieldInsn(Opcodes.GETSTATIC,
                                 jvmName(psiField.getContainingClass()),
                                 psiField.getName(),
                                 psiTypeToAsm(psiField.getType()).getDescriptor());
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate field reference " + descriptor);
            }
        }
        else {
            int index = myMap.getIndex(descriptor);
            if (index >= 0) {
                myStack.push(StackValue.local(index, ((PropertyDescriptor) descriptor).getOutType()));
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
            }
        }
    }

    @Override
    public void visitCallExpression(JetCallExpression expression) {
        JetExpression callee = expression.getCalleeExpression();

        if (callee instanceof JetSimpleNameExpression) {
            DeclarationDescriptor funDescriptor = bindingContext.resolveReferenceExpression((JetSimpleNameExpression) callee);
            if (funDescriptor instanceof FunctionDescriptor) {
                PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(funDescriptor);
                if (declarationPsiElement instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) declarationPsiElement;
                    PsiParameter[] parameters = method.getParameterList().getParameters();

                    List<JetArgument> args = expression.getValueArguments();
                    for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
                        JetArgument arg = args.get(i);
                        gen(arg.getArgumentExpression(), psiTypeToAsm(parameters [i].getType()));
                    }

                    if (method.hasModifierProperty(PsiModifier.STATIC)) {
                        v.visitMethodInsn(Opcodes.INVOKESTATIC,
                                jvmName(method.getContainingClass()),
                                method.getName(),
                                getMethodDescriptor(method));
                    }
                    else {
                        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                          jvmName(method.getContainingClass()),
                                          method.getName(),
                                          getMethodDescriptor(method));
                    }
                    myStack.push(StackValue.onStack(psiTypeToAsm(method.getReturnType())));
                }
            }
            else {
                throw new CompilationException();
            }
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate a call");
        }
    }

    private static String jvmName(PsiClass containingClass) {
        return containingClass.getQualifiedName().replace(".", "/");
    }

    private void boxIfNeeded(PsiType type) {
        if (type instanceof PsiPrimitiveType && type != PsiType.VOID) {
            if (type == PsiType.LONG) {
                v.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            }
            else if (type == PsiType.INT) {
                v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            }
            else {
                throw new UnsupportedOperationException("Don't know how to box type " + type);
            }
        }
    }

    private void unbox(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            if (type == PsiType.INT) {
                v.invokevirtual("java/lang/Integer", "intValue", "()I");
            }
            else {
                throw new UnsupportedOperationException("Don't know how to unbox type " + type);
            }
        }
    }

    private String getMethodDescriptor(PsiMethod method) {
        Type returnType = psiTypeToAsm(method.getReturnType());
        PsiParameter[] parameters = method.getParameterList().getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = psiTypeToAsm(parameters [i].getType());
        }
        return Type.getMethodDescriptor(returnType, parameterTypes);
    }

    private Type psiTypeToAsm(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            if (type == PsiType.VOID) {
                return Type.VOID_TYPE;
            }
            if (type == PsiType.INT) {
                return Type.INT_TYPE;
            }
            if (type == PsiType.LONG) {
                return Type.LONG_TYPE;
            }
        }
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            if (psiClass == null) {
                throw new UnsupportedOperationException("unresolved PsiClassType: " + type);
            }
            return Type.getType("L" + jvmName(psiClass) + ";");
        }
        throw new UnsupportedOperationException("don't know how to map  type " + type + " to ASM");
    }

    @Override
    public void visitDotQualifiedExpression(JetDotQualifiedExpression expression) {
        JetExpression receiver = expression.getReceiverExpression();
        if (!resolvesToClassOrPackage(receiver)) {
            gen(expression.getReceiverExpression());
        }
        gen(expression.getSelectorExpression());
    }

    private boolean resolvesToClassOrPackage(JetExpression receiver) {
        if (receiver instanceof JetReferenceExpression) {
            DeclarationDescriptor declaration = bindingContext.resolveReferenceExpression((JetReferenceExpression) receiver);
            PsiElement declarationElement = bindingContext.getDeclarationPsiElement(declaration);
            if (declarationElement instanceof PsiClass) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitSafeQualifiedExpression(JetSafeQualifiedExpression expression) {
        gen(expression.getReceiverExpression());
        Label ifnull = new Label();
        Label end = new Label();
        v.dup();
        v.ifnull(ifnull);
        gen(expression.getSelectorExpression());
        v.goTo(end);
        v.mark(ifnull);
        // null is already on stack here after the dup
        JetType expressionType = bindingContext.getExpressionType(expression);
        if (expressionType.equals(JetStandardClasses.getUnitType())) {
            v.pop();
        }
        v.mark(end);
    }

    @Override
    public void visitBinaryExpression(JetBinaryExpression expression) {
        final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
        if (opToken == JetTokens.EQ) {
            generateAssignmentExpression(expression);
            return;
        }
        DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationReference());
        if (op instanceof FunctionDescriptor) {
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (cls instanceof ClassDescriptor) {
                final String className = cls.getName();
                if (className.equals("Int")) {
                    if (op.getName().equals("compareTo")) {
                        generateCompareOp(expression, opToken);
                    }
                    else {
                        int opcode;
                        if (op.getName().equals("plus")) {
                            opcode = Opcodes.IADD;
                        }
                        else if (op.getName().equals("times")) {
                            opcode = Opcodes.IMUL;
                        }
                        else {
                            throw new UnsupportedOperationException("Don't know how to generate binary op method " + op.getName());
                        }
                        generateBinaryOp(expression, (FunctionDescriptor) op, opcode);
                    }
                    return;
                }
                else {
                    throw new UnsupportedOperationException("Don't know how to generate binary op for class " + className);
                }
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate binary op " + expression);
    }

    private void generateBinaryOp(JetBinaryExpression expression, FunctionDescriptor op, int opcode) {
        JetType returnType = op.getUnsubstitutedReturnType();
        if (returnType.equals(stdlib.getIntType())) {
            gen(expression.getLeft(), Type.INT_TYPE);
            gen(expression.getRight(), Type.INT_TYPE);
            v.visitInsn(Type.INT_TYPE.getOpcode(opcode));
            myStack.push(StackValue.onStack(Type.INT_TYPE));
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate binary op with return type " + returnType);
        }
    }

    private void generateCompareOp(JetBinaryExpression expression, IElementType opToken) {
        gen(expression.getLeft(), Type.INT_TYPE);
        gen(expression.getRight(), Type.INT_TYPE);
        myStack.push(StackValue.icmp(opToken));
    }

    private void generateAssignmentExpression(JetBinaryExpression expression) {
        if (expression.getLeft() instanceof JetReferenceExpression) {
            final JetReferenceExpression lhs = (JetReferenceExpression) expression.getLeft();
            final DeclarationDescriptor declarationDescriptor = bindingContext.resolveReferenceExpression(lhs);
            final int index = myMap.getIndex(declarationDescriptor);
            final Type type = typeMapper.mapType(bindingContext.getExpressionType(lhs));
            gen(expression.getRight(), type);
            v.store(index, type);
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate assignment to " + expression.getLeft().getText());
        }
    }

    @Override
    public void visitProperty(JetProperty property) {
        PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor(property);
        int index = myMap.getIndex(propertyDescriptor);

        assert index >= 0;

        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            Type type = typeMapper.mapType(propertyDescriptor.getOutType());
            gen(initializer, type);
            v.store(index, type);
        }
    }

    private static class CompilationException extends RuntimeException {
    }
}
