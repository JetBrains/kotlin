package org.jetbrains.jet.codegen;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Stack;

/**
 * @author max
 */
public class ExpressionCodegen extends JetVisitor {
    private static final String CLASS_OBJECT = "java/lang/Object";
    private static final String CLASS_STRING = "java/lang/String";
    private static final String CLASS_STRING_BUILDER = "java/lang/StringBuilder";

    private final Stack<Label> myLoopStarts = new Stack<Label>();
    private final Stack<Label> myLoopEnds = new Stack<Label>();
    private final Stack<StackValue> myStack = new Stack<StackValue>();

    private final InstructionAdapter v;
    private final FrameMap myMap;
    private final JetTypeMapper typeMapper;
    private final Type returnType;
    private final BindingContext bindingContext;

    public ExpressionCodegen(MethodVisitor v, BindingContext bindingContext, FrameMap myMap, JetTypeMapper typeMapper,
                             Type returnType) {
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
        if (asmType != Type.VOID_TYPE) {
            myStack.push(StackValue.onStack(asmType));
        }
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
        myStack.pop().condJump(condition, false, v);

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

    private void generateSingleBranchIf(JetExpression expression, boolean inverse) {
        Label endLabel = new Label();

        myStack.pop().condJump(endLabel, inverse, v);

        gen(expression, Type.VOID_TYPE);

        v.mark(endLabel);
    }

    @Override
    public void visitConstantExpression(JetConstantExpression expression) {
        myStack.push(StackValue.constant(expression.getValue(), expressionType(expression)));
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
                final PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor((JetProperty) statement);
                final Type type = typeMapper.mapType(propertyDescriptor.getOutType());
                myMap.enter(propertyDescriptor, type.getSize());
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
        if (descriptor instanceof PropertyDescriptor) {
            final DeclarationDescriptor container = descriptor.getContainingDeclaration();
            if (isClass(container, "Number")) {
                Type castType = getCastType(expression.getReferencedName());
                if (castType != null) {
                    final StackValue value = myStack.pop();
                    value.put(castType, v);
                    myStack.push(StackValue.onStack(castType));
                    return;
                }
            }
        }
        PsiElement declaration = bindingContext.getDeclarationPsiElement(descriptor);
        if (declaration instanceof PsiField) {
            PsiField psiField = (PsiField) declaration;
            if (psiField.hasModifierProperty(PsiModifier.STATIC)) {
                v.visitFieldInsn(Opcodes.GETSTATIC,
                                 JetTypeMapper.jvmName(psiField.getContainingClass()),
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
                final JetType outType = ((PropertyDescriptor) descriptor).getOutType();
                myStack.push(StackValue.local(index, typeMapper.mapType(outType)));
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
            }
        }
    }

    @Nullable
    private static Type getCastType(String castMethodName) {
        if ("dbl".equals(castMethodName)) {
            return Type.DOUBLE_TYPE;
        }
        if ("flt".equals(castMethodName)) {
            return Type.FLOAT_TYPE;
        }
        if ("lng".equals(castMethodName)) {
            return Type.LONG_TYPE;
        }
        if ("int".equals(castMethodName)) {
            return Type.INT_TYPE;
        }
        if ("chr".equals(castMethodName)) {
            return Type.CHAR_TYPE;
        }
        if ("sht".equals(castMethodName)) {
            return Type.SHORT_TYPE;
        }
        if ("byt".equals(castMethodName)) {
            return Type.BYTE_TYPE;
        }
        return null;
    }

    @Override
    public void visitCallExpression(JetCallExpression expression) {
        JetExpression callee = expression.getCalleeExpression();

        if (callee instanceof JetSimpleNameExpression) {
            DeclarationDescriptor funDescriptor = bindingContext.resolveReferenceExpression((JetSimpleNameExpression) callee);
            if (funDescriptor instanceof FunctionDescriptor) {
                final DeclarationDescriptor functionParent = funDescriptor.getContainingDeclaration();
                if (isNumberPrimitive(functionParent)) {
                    if (funDescriptor.getName().equals("inv")) {
                        final StackValue value = myStack.pop();  // HACK we rely on the dot reference handler to put it on the stack
                        final Type asmType = expressionType(expression);
                        value.put(asmType, v);
                        generateInv(asmType);
                        return;
                    }
                }

                if (expression.getParent() instanceof JetDotQualifiedExpression) {
                    final JetDotQualifiedExpression parent = (JetDotQualifiedExpression) expression.getParent();
                    if (!resolvesToClassOrPackage(parent.getReceiverExpression())) {
                        // we have a receiver on stack
                        myStack.pop().put(Type.getObjectType(CLASS_OBJECT), v);
                    }
                }

                PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(funDescriptor);
                Method methodDescriptor;
                if (declarationPsiElement instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) declarationPsiElement;
                    methodDescriptor = getMethodDescriptor(psiMethod);
                    pushMethodArguments(expression, methodDescriptor);

                    final boolean isStatic = psiMethod.hasModifierProperty(PsiModifier.STATIC);
                    v.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                            JetTypeMapper.jvmName(psiMethod.getContainingClass()),
                            methodDescriptor.getName(),
                            methodDescriptor.getDescriptor());
                }
                else {
                    if (functionParent instanceof NamespaceDescriptor && declarationPsiElement instanceof JetFunction) {
                        methodDescriptor = typeMapper.mapSignature((JetFunction) declarationPsiElement);
                        pushMethodArguments(expression, methodDescriptor);
                        final String owner = NamespaceCodegen.getJVMClassName(DescriptorUtil.getFQName(functionParent));
                        v.invokestatic(owner, methodDescriptor.getName(), methodDescriptor.getDescriptor());
                    }
                    else {
                        throw new UnsupportedOperationException("don't know how to generate call to " + declarationPsiElement);
                    }
                }
                if (methodDescriptor.getReturnType() != Type.VOID_TYPE) {
                    myStack.push(StackValue.onStack(methodDescriptor.getReturnType()));
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

    private void pushMethodArguments(JetCall expression, Method method) {
        final Type[] argTypes = method.getArgumentTypes();
        List<JetArgument> args = expression.getValueArguments();
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            JetArgument arg = args.get(i);
            gen(arg.getArgumentExpression(), argTypes[i]);
        }
    }

    private static Method getMethodDescriptor(PsiMethod method) {
        Type returnType = method.isConstructor() ? Type.VOID_TYPE : psiTypeToAsm(method.getReturnType());
        PsiParameter[] parameters = method.getParameterList().getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = psiTypeToAsm(parameters [i].getType());
        }
        return new Method(method.getName(), Type.getMethodDescriptor(returnType, parameterTypes));
    }

    private Type expressionType(JetExpression expr) {
        return typeMapper.mapType(bindingContext.getExpressionType(expr));
    }

    private int indexOfLocal(JetReferenceExpression lhs) {
        final DeclarationDescriptor declarationDescriptor = bindingContext.resolveReferenceExpression(lhs);
        return myMap.getIndex(declarationDescriptor);
    }

    private static Type psiTypeToAsm(PsiType type) {
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
            if (type == PsiType.BOOLEAN) {
                return Type.BOOLEAN_TYPE;
            }
            if (type == PsiType.BYTE) {
                return Type.BYTE_TYPE;
            }
            if (type == PsiType.SHORT) {
                return Type.SHORT_TYPE;
            }
            if (type == PsiType.CHAR) {
                return Type.CHAR_TYPE;
            }
            if (type == PsiType.FLOAT) {
                return Type.FLOAT_TYPE;
            }
            if (type == PsiType.DOUBLE) {
                return Type.DOUBLE_TYPE;
            }
        }
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            if (psiClass == null) {
                throw new UnsupportedOperationException("unresolved PsiClassType: " + type);
            }
            return JetTypeMapper.psiClassType(psiClass);
        }
        throw new UnsupportedOperationException("don't know how to map type " + type + " to ASM");
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
        }
        else if (JetTokens.AUGMENTED_ASSIGNMENTS.contains(opToken)) {
            generateAugmentedAssignment(expression);
        }
        else if (opToken == JetTokens.ANDAND) {
            generateBooleanAnd(expression);
        }
        else if (opToken == JetTokens.OROR) {
            generateBooleanOr(expression);
        }
        else if (opToken == JetTokens.EQEQ || opToken == JetTokens.EXCLEQ ||
                 opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
            generateEquals(expression, opToken);
        }
        else {
            DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationReference());
            if (op instanceof FunctionDescriptor) {
                JetType returnType = bindingContext.getExpressionType(expression);
                final Type asmType = typeMapper.mapType(returnType);
                DeclarationDescriptor cls = op.getContainingDeclaration();
                if (isNumberPrimitive(cls)) {
                    if (op.getName().equals("compareTo")) {
                        generateCompareOp(expression, opToken, asmType);
                    }
                    else {
                        int opcode = opcodeForMethod(op.getName());
                        generateBinaryOp(expression, (FunctionDescriptor) op, opcode);
                    }
                    return;
                }
                else if (isClass(cls, "String") && op.getName().equals("plus")) {
                    generateConcatenation(expression);
                    return;
                }
            }
            throw new UnsupportedOperationException("Don't know how to generate binary op " + expression);
        }
    }

    private void generateBooleanAnd(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifFalse = new Label();
        v.ifeq(ifFalse);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifFalse);
        v.aconst(false);
        v.mark(end);
        myStack.push(StackValue.onStack(Type.BOOLEAN_TYPE));
    }

    private void generateBooleanOr(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifTrue = new Label();
        v.ifne(ifTrue);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifTrue);
        v.aconst(true);
        v.mark(end);
        myStack.push(StackValue.onStack(Type.BOOLEAN_TYPE));
    }

    private void generateEquals(JetBinaryExpression expression, IElementType opToken) {
        final Type leftType = expressionType(expression.getLeft());
        final Type rightType = expressionType(expression.getRight());
        if (isNumberPrimitive(leftType) && leftType == rightType) {
            generateCompareOp(expression, opToken, leftType);
        }
        else {
            gen(expression.getLeft(), leftType);
            gen(expression.getRight(), rightType);
            if (opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
                myStack.push(StackValue.cmp(opToken, leftType));
            }
            else {
                generateNullSafeEquals(opToken);
            }
        }
    }

    private void generateNullSafeEquals(IElementType opToken) {
        v.dup2();   // left right left right
        Label rightNull = new Label();
        v.ifnull(rightNull);
        Label leftNull = new Label();
        v.ifnull(leftNull);
        v.invokevirtual(CLASS_OBJECT, "equals", "(Ljava/lang/Object;)Z");
        Label end = new Label();
        v.goTo(end);
        v.mark(rightNull);
        // left right left
        Label bothNull = new Label();
        v.ifnull(bothNull);
        v.mark(leftNull);
        v.pop2();
        v.aconst(Boolean.FALSE);
        v.goTo(end);
        v.mark(bothNull);
        v.pop2();
        v.aconst(Boolean.TRUE);
        v.mark(end);

        final StackValue onStack = StackValue.onStack(Type.BOOLEAN_TYPE);
        if (opToken == JetTokens.EXCLEQ) {
            myStack.push(StackValue.not(onStack));
        }
        else {
            myStack.push(onStack);
        }
    }

    private static boolean isNumberPrimitive(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        String className = descriptor.getName();
        return className.equals("Int") || className.equals("Long") || className.equals("Short") ||
               className.equals("Byte") || className.equals("Char") || className.equals("Float") ||
               className.equals("Double");
    }

    private static boolean isClass(DeclarationDescriptor descriptor, String name) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        String className = descriptor.getName();
        return className.equals(name);
    }

    private static boolean isNumberPrimitive(Type type) {
        return isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    private static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    private static int opcodeForMethod(final String name) {
        if (name.equals("plus")) return Opcodes.IADD;
        if (name.equals("minus")) return Opcodes.ISUB;
        if (name.equals("times")) return Opcodes.IMUL;
        if (name.equals("div")) return Opcodes.IDIV;
        if (name.equals("mod")) return Opcodes.IREM;
        if (name.equals("shl")) return Opcodes.ISHL;
        if (name.equals("shr")) return Opcodes.ISHR;
        if (name.equals("ushr")) return Opcodes.IUSHR;
        if (name.equals("and")) return Opcodes.IAND;
        if (name.equals("or")) return Opcodes.IOR;
        if (name.equals("xor")) return Opcodes.IXOR;
        throw new UnsupportedOperationException("Don't know how to generate binary op method " + name);
    }

    private void generateBinaryOp(JetBinaryExpression expression, FunctionDescriptor op, int opcode) {
        JetType returnType = op.getUnsubstitutedReturnType();
        final Type asmType = typeMapper.mapType(returnType);
        if (asmType == Type.INT_TYPE || asmType == Type.LONG_TYPE ||
            asmType == Type.FLOAT_TYPE || asmType == Type.DOUBLE_TYPE) {
            gen(expression.getLeft(), asmType);
            gen(expression.getRight(), asmType);
            v.visitInsn(asmType.getOpcode(opcode));
            myStack.push(StackValue.onStack(asmType));
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate binary op with return type " + returnType);
        }
    }

    private void generateCompareOp(JetBinaryExpression expression, IElementType opToken, Type type) {
        gen(expression.getLeft(), type);
        gen(expression.getRight(), type);
        myStack.push(StackValue.cmp(opToken, type));
    }

    private void generateAssignmentExpression(JetBinaryExpression expression) {
        if (expression.getLeft() instanceof JetReferenceExpression) {
            final JetReferenceExpression lhs = (JetReferenceExpression) expression.getLeft();
            final int index = indexOfLocal(lhs);
            final Type type = typeMapper.mapType(bindingContext.getExpressionType(lhs));
            gen(expression.getRight(), type);
            v.store(index, type);
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate assignment to " + expression.getLeft().getText());
        }
    }

    private void generateAugmentedAssignment(JetBinaryExpression expression) {
        final JetExpression lhs = expression.getLeft();
        if (lhs instanceof JetReferenceExpression) {
            DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationReference());
            final JetType leftType = bindingContext.getExpressionType(lhs);
            final Type asmType = typeMapper.mapType(leftType);
            if (isNumberPrimitive(asmType)) {
                final int index = indexOfLocal((JetReferenceExpression) lhs);
                assert index >= 0;
                v.load(index, asmType);
                gen(expression.getRight(), asmType);
                int opcode = opcodeForMethod(op.getName());
                v.visitInsn(asmType.getOpcode(opcode));
                v.store(index, asmType);
            }
            else {
                throw new UnsupportedOperationException("Don't know how to generate augmented assignment for non-numeric types");
            }
        }
        else {
            throw new UnsupportedOperationException("Don't know how to generate augmented assignment to " + lhs.getText());
        }
    }

    private void generateConcatenation(JetBinaryExpression expression) {
        Type type = Type.getObjectType(CLASS_STRING_BUILDER);
        v.anew(type);
        v.dup();
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        v.invokespecial(CLASS_STRING_BUILDER, method.getName(), method.getDescriptor());
        invokeAppend(expression.getLeft());
        invokeAppend(expression.getRight());
        v.invokevirtual(CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
        myStack.push(StackValue.onStack(Type.getObjectType(CLASS_STRING)));
    }

    private void invokeAppend(final JetExpression expr) {
        Type exprType = expressionType(expr);
        gen(expr, exprType);
        Method appendDescriptor = new Method("append", Type.getObjectType(CLASS_STRING_BUILDER),
                new Type[] { exprType.getSort() == Type.OBJECT ? Type.getObjectType(CLASS_OBJECT) : exprType});
        v.invokevirtual(CLASS_STRING_BUILDER, "append", appendDescriptor.getDescriptor());
    }

    @Override
    public void visitPrefixExpression(JetPrefixExpression expression) {
        DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationSign());
        if (op instanceof FunctionDescriptor) {
            final Type asmType = expressionType(expression);
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (isNumberPrimitive(cls)) {
                if (generateUnaryOp(op, asmType, expression.getBaseExpression())) return;
            }
            else if (isClass(cls, "Boolean") && op.getName().equals("not")) {
                generateNot(expression);
                return;
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate this prefix expression");
    }

    @Override
    public void visitPostfixExpression(JetPostfixExpression expression) {
        DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationSign());
        if (op instanceof FunctionDescriptor) {
            final Type asmType = expressionType(expression);
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (isNumberPrimitive(cls) && (op.getName().equals("inc") || op.getName().equals("dec"))) {
                if (bindingContext.isStatement(expression)) {
                    generateIncrement(op, asmType, expression.getBaseExpression());
                }
                else {
                    int oldStackSize = myStack.size();
                    gen(expression.getBaseExpression(), asmType);
                    generateIncrement(op, asmType, expression.getBaseExpression());
                    myStack.push(StackValue.onStack(asmType));
                    assert myStack.size() == oldStackSize+1;
                }
                return;
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate this prefix expression");
    }

    private boolean generateUnaryOp(DeclarationDescriptor op, Type asmType, final JetExpression operand) {
        if (op.getName().equals("minus")) {
            gen(operand, asmType);
            v.neg(asmType);
            myStack.push(StackValue.onStack(asmType));
            return true;
        }
        else if (op.getName().equals("inc") || op.getName().equals("dec")) {
            final int index = generateIncrement(op, asmType, operand);
            myStack.push(StackValue.local(index, asmType));
            return true;
        }
        return false;
    }

    private void generateNot(JetPrefixExpression expression) {
        int oldStackSize = myStack.size();
        gen(expression.getBaseExpression());
        assert myStack.size() == oldStackSize+1;
        myStack.set(myStack.size()-1, StackValue.not(myStack.get(myStack.size() - 1)));
    }

    private int generateIncrement(DeclarationDescriptor op, Type asmType, JetExpression operand) {
        if (!(operand instanceof JetReferenceExpression)) {
            throw new UnsupportedOperationException("cannot increment or decrement a non-lvalue");
        }
        int increment = op.getName().equals("inc") ? 1 : -1;
        final int index = indexOfLocal((JetReferenceExpression) operand);
        if (index < 0) {
            throw new UnsupportedOperationException("don't know how to increment or decrement something which is not a local var");
        }
        if (isIntPrimitive(asmType)) {
            v.iinc(index, increment);
        }
        else {
            gen(operand, asmType);
            if (asmType == Type.LONG_TYPE) {
                v.aconst(Long.valueOf(increment));
            }
            else if (asmType == Type.FLOAT_TYPE) {
                v.aconst(Float.valueOf(increment));
            }
            else if (asmType == Type.DOUBLE_TYPE) {
                v.aconst(Double.valueOf(increment));
            }
            else {
                throw new UnsupportedOperationException("unknown type in increment: " + asmType);
            }
            v.add(asmType);
            v.store(index, asmType);
        }
        return index;
    }

    private void generateInv(Type asmType) {
        v.aconst(-1);
        v.xor(asmType);
        myStack.push(StackValue.onStack(asmType));
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

    @Override
    public void visitNewExpression(JetNewExpression expression) {
        final JetUserType constructorType = (JetUserType) expression.getTypeReference().getTypeElement();
        final JetSimpleNameExpression constructorReference = constructorType.getReferenceExpression();
        final PsiElement declaration = bindingContext.getDeclarationPsiElement(bindingContext.resolveReferenceExpression(constructorReference));
        if (declaration instanceof PsiMethod) {
            final PsiMethod constructor = (PsiMethod) declaration;
            PsiClass javaClass = constructor.getContainingClass();
            Type type = JetTypeMapper.psiClassType(javaClass);
            v.anew(type);
            v.dup();
            final Method constructorDescriptor = getMethodDescriptor(constructor);
            pushMethodArguments(expression, constructorDescriptor);
            v.invokespecial(JetTypeMapper.jvmName(javaClass), "<init>", constructorDescriptor.getDescriptor());
            myStack.push(StackValue.onStack(type));
            return;
        }

        throw new UnsupportedOperationException("don't know how to generate this new expression");
    }

    private static class CompilationException extends RuntimeException {
    }
}
