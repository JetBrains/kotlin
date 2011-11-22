package org.jetbrains.jet.codegen;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.*;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author max
 * @author yole
 * @author alex.tkachman
 */
public class ExpressionCodegen extends JetVisitor<StackValue, StackValue> {
    private static final String CLASS_OBJECT = "java/lang/Object";
    private static final String CLASS_STRING = "java/lang/String";
    public static final String CLASS_STRING_BUILDER = "java/lang/StringBuilder";
    private static final String CLASS_COMPARABLE = "java/lang/Comparable";

    private static final String CLASS_NO_PATTERN_MATCHED_EXCEPTION = "jet/NoPatternMatchedException";
    private static final String CLASS_TYPE_CAST_EXCEPTION = "jet/TypeCastException";

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type STRING_TYPE = Type.getObjectType(CLASS_STRING);

    private final Stack<Label> myContinueTargets = new Stack<Label>();
    private final Stack<Label> myBreakTargets = new Stack<Label>();

    private int myLastLineNumber = -1;

    final InstructionAdapter v;
    final FrameMap myFrameMap;
    final JetTypeMapper typeMapper;

    private final GenerationState state;
    private final Type returnType;

    private final BindingContext bindingContext;
    private final Map<TypeParameterDescriptor, StackValue> typeParameterExpressions = new HashMap<TypeParameterDescriptor, StackValue>();
    private final CodegenContext context;
    private final IntrinsicMethods intrinsics;

    private final ArrayList<JetTryExpression> stackOfFinallyBlocks = new ArrayList<JetTryExpression>();

    public ExpressionCodegen(MethodVisitor v,
                             FrameMap myMap,
                             Type returnType,
                             CodegenContext context,
                             GenerationState state) {
        this.myFrameMap = myMap;
        this.typeMapper = state.getTypeMapper();
        this.returnType = returnType;
        this.state = state;
        this.v = new InstructionAdapter(v);
        this.bindingContext = state.getBindingContext();
        this.context = context;
        this.intrinsics = state.getIntrinsics();
    }

    StackValue castToRequiredTypeOfInterfaceIfNeeded(StackValue inner, DeclarationDescriptor provided, @Nullable ClassDescriptor required) {
        if(required == null)
            return inner;

        if(provided instanceof CallableDescriptor)
            provided = ((CallableDescriptor)provided).getReceiverParameter().getType().getConstructor().getDeclarationDescriptor();

        assert provided instanceof ClassDescriptor;

        if(!CodegenUtil.isInterface(provided) && CodegenUtil.isInterface(required)) {
            v.checkcast(typeMapper.mapType(required.getDefaultType()));
        }

        return inner;
    }

    public GenerationState getState() {
        return state;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JetTypeMapper getTypeMapper() {
        return state.getTypeMapper();
    }

    public void addTypeParameter(TypeParameterDescriptor typeParameter, StackValue expression) {
        typeParameterExpressions.put(typeParameter, expression);
    }

    public StackValue genQualified(StackValue receiver, JetElement selector) {
        markLineNumber(selector);
        return selector.accept(this, receiver);
    }

    public StackValue gen(JetElement expr) {
        return genQualified(StackValue.none(), expr);
    }

    public void gen(JetElement expr, Type type) {
        StackValue value = gen(expr);
        value.put(type, v);
    }

    public void genToJVMStack(JetExpression expr) {
        gen(expr, expressionType(expr));
    }

    @Override
    public StackValue visitExpression(JetExpression expression, StackValue receiver) {
        throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
    }

    @Override
    public StackValue visitNamedFunction(JetNamedFunction function, StackValue data) {
        throw new UnsupportedOperationException("Codegen for named functions is not yet implemented");
    }

    @Override
    public StackValue visitSuperExpression(JetSuperExpression expression, StackValue data) {
//        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
//        if (descriptor instanceof ClassDescriptor) {
//            return generateThisOrOuter((ClassDescriptor) descriptor);
//        }
//        else {
//            return local0();
//        }
        return StackValue.none();
    }

    @Override
    public StackValue visitParenthesizedExpression(JetParenthesizedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getExpression());
    }

    @Override
    public StackValue visitAnnotatedExpression(JetAnnotatedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getBaseExpression());
    }

    @Override
    public StackValue visitIfExpression(JetIfExpression expression, StackValue receiver) {
        Type asmType = expressionType(expression);
        StackValue condition = gen(expression.getCondition());

        JetExpression thenExpression = expression.getThen();
        JetExpression elseExpression = expression.getElse();

        if (thenExpression == null && elseExpression == null) {
            throw new CompilationException();
        }

        if (thenExpression == null) {
            return generateSingleBranchIf(condition, elseExpression, false);
        }

        if (elseExpression == null) {
            return generateSingleBranchIf(condition, thenExpression, true);
        }


        Label elseLabel = new Label();
        condition.condJump(elseLabel, true, v);   // == 0, i.e. false

        gen(thenExpression, asmType);

        Label endLabel = new Label();
        v.goTo(endLabel);
        v.mark(elseLabel);

        gen(elseExpression, asmType);

        v.mark(endLabel);

        return StackValue.onStack(asmType);
    }

    @Override
    public StackValue visitWhileExpression(JetWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        myContinueTargets.push(condition);
        v.mark(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(end, true, v);

        gen(expression.getBody(), Type.VOID_TYPE);
        v.goTo(condition);

        v.mark(end);
        myBreakTargets.pop();
        myContinueTargets.pop();

        return StackValue.onStack(Type.VOID_TYPE);
    }

    @Override
    public StackValue visitDoWhileExpression(JetDoWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        v.mark(condition);
        myContinueTargets.push(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        gen(expression.getBody(), Type.VOID_TYPE);

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(condition, false, v);

        v.mark(end);

        myBreakTargets.pop();
        myContinueTargets.pop();
        return StackValue.onStack(Type.VOID_TYPE);
    }

    @Override
    public StackValue visitForExpression(JetForExpression expression, StackValue receiver) {
        final JetExpression loopRange = expression.getLoopRange();
        final JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, loopRange);
        Type loopRangeType = typeMapper.mapType(expressionType);
        if (loopRangeType.getSort() == Type.ARRAY) {
            new ForInArrayLoopGenerator(expression, loopRangeType).invoke();
            return StackValue.none();
        }
        else {
            assert expressionType != null;
            final DeclarationDescriptor descriptor = expressionType.getConstructor().getDeclarationDescriptor();
            if (isClass(descriptor, "IntRange")) {       // TODO IntRange subclasses (now IntRange is final)
                new ForInRangeLoopGenerator(expression, loopRangeType).invoke();
                return StackValue.none();
            }

            generateForInIterable(expression, loopRangeType);
            return StackValue.none();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void generateForInIterable(JetForExpression expression, Type loopRangeType) {
        final JetExpression loopRange = expression.getLoopRange();

        FunctionDescriptor iteratorDescriptor = bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR, loopRange);
        FunctionDescriptor nextDescriptor = bindingContext.get(BindingContext.LOOP_RANGE_NEXT, loopRange);
        DeclarationDescriptor hasNextDescriptor = bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT, loopRange);

        if(iteratorDescriptor == null)
            throw new IllegalStateException("No iterator() method " + DiagnosticUtils.atLocation(loopRange));
        if(nextDescriptor == null)
            throw new IllegalStateException("No next() method " + DiagnosticUtils.atLocation(loopRange));
        if(hasNextDescriptor == null)
            throw new IllegalStateException("No hasNext method or property" + DiagnosticUtils.atLocation(loopRange));

        final JetParameter loopParameter = expression.getLoopParameter();
        final VariableDescriptor parameterDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, loopParameter);

        JetType iteratorType = iteratorDescriptor.getReturnType();
        Type asmIterType = JetTypeMapper.boxType(typeMapper.mapType(iteratorType));

        JetType paramType = parameterDescriptor.getOutType();
        Type asmParamType = typeMapper.mapType(paramType);

        int iteratorVar = myFrameMap.enterTemp();
        gen(expression.getLoopRange(), loopRangeType);
        invokeFunctionNoParams(iteratorDescriptor, asmIterType, v);
        v.store(iteratorVar, asmIterType);
        
        Label end = new Label();
        if(iteratorType.isNullable()) {
            v.load(iteratorVar, JetTypeMapper.TYPE_OBJECT);
            v.ifnull(end);
        }

        Label begin = new Label();
        myContinueTargets.push(begin);
        myBreakTargets.push(end);

        v.mark(begin);
        v.load(iteratorVar, asmIterType);
        if(hasNextDescriptor instanceof FunctionDescriptor) {
            FunctionDescriptor hND = (FunctionDescriptor) hasNextDescriptor;
            invokeFunctionNoParams(hND, Type.BOOLEAN_TYPE, v);
        }
        else {
//            hND = ((PropertyDescriptor) hasNextDescriptor).getGetter();
//            if(hND != null)
//                invokeFunctionNoParams(hND, Type.BOOLEAN_TYPE, v);
//            else
                intermediateValueForProperty((PropertyDescriptor) hasNextDescriptor, false, null).put(Type.BOOLEAN_TYPE, v);
        }
        v.ifeq(end);

        myFrameMap.enter(parameterDescriptor, asmParamType.getSize());
        v.load(iteratorVar, asmIterType);
        invokeFunctionNoParams(nextDescriptor, asmParamType, v);

        // TODO checkcast should be generated via StackValue
        if (asmParamType.getSort() == Type.OBJECT && !"java.lang.Object".equals(asmParamType.getClassName())) {
            v.checkcast(asmParamType);
        }
        v.store(lookupLocal(parameterDescriptor), asmParamType);

        gen(expression.getBody(), Type.VOID_TYPE);

        v.goTo(begin);
        v.mark(end);

        int paramIndex = myFrameMap.leave(parameterDescriptor);
        //noinspection ConstantConditions
        v.visitLocalVariable(loopParameter.getName(), asmParamType.getDescriptor(), null, begin, end, paramIndex);
        myFrameMap.leaveTemp();
        myBreakTargets.pop();
        myContinueTargets.pop();
    }

    private OwnerKind contextKind() {
        return context.getContextKind();
    }

    private abstract class ForLoopGenerator {
        protected JetForExpression expression;
        protected Type loopRangeType;
        protected JetType expressionType;
        protected VariableDescriptor parameterDescriptor;
        Label end = new Label();

        public ForLoopGenerator(JetForExpression expression, Type loopRangeType) {
            this.expression = expression;
            this.loopRangeType = loopRangeType;
            final JetParameter loopParameter = expression.getLoopParameter();
            this.parameterDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, loopParameter);
            expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getLoopRange());
        }

        public void invoke() {
            JetType paramType = parameterDescriptor.getOutType();
            Type asmParamType = typeMapper.mapType(paramType);

            myFrameMap.enter(parameterDescriptor, asmParamType.getSize());
            generatePrologue();

            Label condition = new Label();
            Label increment = new Label();
            v.mark(condition);
            myContinueTargets.push(increment);
            myBreakTargets.push(end);

            generateCondition(asmParamType, end);

            gen(expression.getBody(), Type.VOID_TYPE);

            v.mark(increment);
            generateIncrement();
            v.goTo(condition);
            v.mark(end);

            cleanupTemp();
            final int paramIndex = myFrameMap.leave(parameterDescriptor);
            //noinspection ConstantConditions
            v.visitLocalVariable(expression.getLoopParameter().getName(), asmParamType.getDescriptor(), null, condition, end, paramIndex);
            myBreakTargets.pop();
            myContinueTargets.pop();
        }

        protected void generatePrologue() {
        }

        protected abstract void generateCondition(Type asmParamType, Label end);

        protected abstract void generateIncrement();

        protected void cleanupTemp() {
        }
    }

    private class ForInArrayLoopGenerator extends ForLoopGenerator {
        private int myIndexVar;
        private int myArrayVar;
        private boolean localArrayVar;

        public ForInArrayLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myIndexVar = myFrameMap.enterTemp();

            StackValue value = gen(expression.getLoopRange());
            if(value instanceof StackValue.Local) {
                myArrayVar = ((StackValue.Local)value).index;
                localArrayVar = true;
            }
            else {
                myArrayVar = myFrameMap.enterTemp();
                value.put(loopRangeType, v);
                v.store(myArrayVar, JetTypeMapper.TYPE_OBJECT);
            }

            if(expressionType.isNullable()) {
                v.load(myArrayVar, JetTypeMapper.TYPE_OBJECT);
                v.ifnull(end);
            }

            v.iconst(0);
            v.store(myIndexVar, Type.INT_TYPE);
        }

        protected void generateCondition(Type asmParamType, Label end) {
            Type arrayElParamType = state.getStandardLibrary().getArray().equals(expressionType.getConstructor().getDeclarationDescriptor()) ? JetTypeMapper.boxType(asmParamType): asmParamType;

            v.load(myIndexVar, Type.INT_TYPE);
            v.load(myArrayVar, JetTypeMapper.TYPE_OBJECT);
            v.arraylength();
            v.ificmpge(end);

            v.load(myArrayVar, JetTypeMapper.TYPE_OBJECT);
            v.load(myIndexVar, Type.INT_TYPE);
            v.aload(arrayElParamType);
            StackValue.onStack(arrayElParamType).put(asmParamType, v);
            v.store(lookupLocal(parameterDescriptor), asmParamType);
        }

        protected void generateIncrement() {
            v.iinc(myIndexVar, 1);
        }

        protected void cleanupTemp() {
            myFrameMap.leaveTemp(localArrayVar ? 1 : 2);
        }
    }

    private class ForInRangeLoopGenerator extends ForLoopGenerator {
        private int myEndVar;

        public ForInRangeLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myEndVar = myFrameMap.enterTemp();
            if(isIntRangeExpr(expression.getLoopRange())) {
                JetBinaryExpression rangeExpression = (JetBinaryExpression) expression.getLoopRange();
                //noinspection ConstantConditions
                gen(rangeExpression.getLeft(), Type.INT_TYPE);
                v.store(lookupLocal(parameterDescriptor), Type.INT_TYPE);
                gen(rangeExpression.getRight(), Type.INT_TYPE);
                v.store(myEndVar, Type.INT_TYPE);
            }
            else {
                gen(expression.getLoopRange(), loopRangeType);
                v.dup();

                v.invokevirtual("jet/IntRange", "getStart", "()I");
                v.store(lookupLocal(parameterDescriptor), Type.INT_TYPE);
                v.invokevirtual("jet/IntRange", "getEnd", "()I");
                v.store(myEndVar, Type.INT_TYPE);
            }
        }

        @Override
        protected void generateCondition(Type asmParamType, Label end) {
            v.load(lookupLocal(parameterDescriptor), Type.INT_TYPE);
            v.load(myEndVar, Type.INT_TYPE);
            v.ificmpgt(end);
        }

        @Override
        protected void generateIncrement() {
            v.iinc(lookupLocal(parameterDescriptor), 1);  // TODO support decreasing order
        }

        @Override
        protected void cleanupTemp() {
            myFrameMap.leaveTemp(1);
        }
    }

    @Override
    public StackValue visitBreakExpression(JetBreakExpression expression, StackValue receiver) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();

        Label label = labelElement == null ? myBreakTargets.peek() : null; // TODO:

        v.goTo(label);
        return StackValue.none();
    }

    @Override
    public StackValue visitContinueExpression(JetContinueExpression expression, StackValue receiver) {
        String labelName = expression.getLabelName();

        Label label = labelName == null ? myContinueTargets.peek() : null; // TODO:

        v.goTo(label);
        return StackValue.none();
    }

    private StackValue generateSingleBranchIf(StackValue condition, JetExpression expression, boolean inverse) {
        Label endLabel = new Label();

        condition.condJump(endLabel, inverse, v);

        gen(expression, Type.VOID_TYPE);

        v.mark(endLabel);
        return StackValue.none();
    }

    @Override
    public StackValue visitConstantExpression(JetConstantExpression expression, StackValue receiver) {
        CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        return StackValue.constant(compileTimeValue.getValue(), expressionType(expression));
    }

    @Override
    public StackValue visitStringTemplateExpression(JetStringTemplateExpression expression, StackValue receiver) {
        StringBuilder constantValue = new StringBuilder("");
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            if (entry instanceof JetLiteralStringTemplateEntry) {
                constantValue.append(entry.getText());
            }
            else if (entry instanceof JetEscapeStringTemplateEntry) {
                constantValue.append(((JetEscapeStringTemplateEntry) entry).getUnescapedValue());
            }
            else {
                constantValue = null;
                break;
            }
        }
        if (constantValue != null) {
            final Type type = expressionType(expression);
            return StackValue.constant(constantValue.toString(), type);
        }
        else {
            generateStringBuilderConstructor();
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    invokeAppend(entry.getExpression());
                }
                else {
                    String text = entry instanceof JetEscapeStringTemplateEntry
                            ? ((JetEscapeStringTemplateEntry) entry).getUnescapedValue()
                            : entry.getText();
                    v.aconst(text);
                    invokeAppendMethod(STRING_TYPE);
                }
            }
            v.invokevirtual(CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
            return StackValue.onStack(expressionType(expression));
        }
    }

    @Override
    public StackValue visitBlockExpression(JetBlockExpression expression, StackValue receiver) {
        List<JetElement> statements = expression.getStatements();
        return generateBlock(statements);
    }

    @Override
    public StackValue visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, StackValue receiver) {
        //noinspection ConstantConditions
        if (bindingContext.get(BindingContext.BLOCK, expression)) {
            //noinspection ConstantConditions
            return generateBlock(expression.getFunctionLiteral().getBodyExpression().getStatements());
        }
        else {
            ClosureCodegen closureCodegen = new ClosureCodegen(state, this, context);
            final GeneratedAnonymousClassDescriptor closure = closureCodegen.gen(expression);

            if(closureCodegen.isConst()) {
                v.invokestatic(closure.getClassname(), "$getInstance", "()L" + closure.getClassname() + ";");
            }
            else {
                v.anew(Type.getObjectType(closure.getClassname()));
                v.dup();

                final Method cons = closure.getConstructor();

                if (closure.isCaptureThis()) {
                    v.load(0, JetTypeMapper.TYPE_OBJECT);
                }

                if (closure.isCaptureReceiver() != null) {
                    v.load(context.getContextDescriptor().getContainingDeclaration() instanceof NamespaceDescriptor ? 0: 1, closure.isCaptureReceiver());
                }

                for (int i = 0; i < closure.getArgs().size(); i++) {
                    StackValue arg = closure.getArgs().get(i);
                    arg.put(cons.getArgumentTypes()[i], v);
                }

                v.invokespecial(closure.getClassname(), "<init>", cons.getDescriptor());
            }
            return StackValue.onStack(Type.getObjectType(closure.getClassname()));
        }
    }

    @Override
    public StackValue visitObjectLiteralExpression(JetObjectLiteralExpression expression, StackValue receiver) {
        ObjectOrClosureCodegen closureCodegen = new ObjectOrClosureCodegen(this, context, state);
        GeneratedAnonymousClassDescriptor closure = state.generateObjectLiteral(expression, closureCodegen);
        Type type = Type.getObjectType(closure.getClassname());
        v.anew(type);
        v.dup();
        final List<Type> consArgTypes = new LinkedList<Type>(Arrays.asList(closure.getConstructor().getArgumentTypes()));

        if (consArgTypes.size() > 0) {
            v.load(0, JetTypeMapper.TYPE_OBJECT);
        }

        if(closureCodegen.captureReceiver != null) {
            v.load(context.isStatic() ? 0 : 1, closureCodegen.captureReceiver);
            consArgTypes.add(closureCodegen.captureReceiver);
        }
        
        for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closureCodegen.closure.entrySet()) {
            if(entry.getKey() instanceof VariableDescriptor && !(entry.getKey() instanceof PropertyDescriptor)) {
                Type sharedVarType = typeMapper.getSharedVarType(entry.getKey());
                if(sharedVarType == null)
                    sharedVarType = state.getTypeMapper().mapType(((VariableDescriptor) entry.getKey()).getOutType());
                consArgTypes.add(sharedVarType);
                entry.getValue().getOuterValue().put(sharedVarType, v);
            }
        }

        Method cons = new Method("<init>", Type.VOID_TYPE, consArgTypes.toArray(new Type[consArgTypes.size()]));
        v.invokespecial(closure.getClassname(), "<init>", cons.getDescriptor());
        return StackValue.onStack(Type.getObjectType(closure.getClassname()));
    }

    private StackValue generateBlock(List<JetElement> statements) {
        Label blockStart = new Label();
        v.mark(blockStart);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, statement);
                assert variableDescriptor != null;

                final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
                final Type type = sharedVarType != null ? sharedVarType : typeMapper.mapType(variableDescriptor.getOutType());
                myFrameMap.enter(variableDescriptor, type.getSize());
            }
        }

        StackValue answer = StackValue.none();
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            JetElement statement = statements.get(i);
            if (i == statements.size() - 1 /*&& statement instanceof JetExpression && !bindingContext.get(BindingContext.STATEMENT, statement)*/) {
                answer = gen(statement);
            }
            else {
                gen(statement, Type.VOID_TYPE);
            }
        }

        Label blockEnd = new Label();
        v.mark(blockEnd);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                JetProperty var = (JetProperty) statement;
                VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
                assert variableDescriptor != null;

                int index = myFrameMap.leave(variableDescriptor);

                final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
                final Type type = sharedVarType != null ? sharedVarType : typeMapper.mapType(variableDescriptor.getOutType());
                if(sharedVarType != null) {
                    v.aconst(null);
                    v.store(index, JetTypeMapper.TYPE_OBJECT);
                }

                v.visitLocalVariable(var.getName(), type.getDescriptor(), null, blockStart, blockEnd, index);
            }
        }

        return answer;
    }

    private void markLineNumber(JetElement statement) {
        final Document document = statement.getContainingFile().getViewProvider().getDocument();
        if (document != null) {
            int lineNumber = document.getLineNumber(statement.getTextRange().getStartOffset());  // 0-based
            if (lineNumber == myLastLineNumber) {
                return;
            }
            myLastLineNumber = lineNumber;

            Label label = new Label();
            v.visitLabel(label);
            v.visitLineNumber(lineNumber + 1, label);  // 1-based
        }
    }

    private void doFinallyOnReturnOrThrow() {
        if(stackOfFinallyBlocks.size() > 0) {
            JetTryExpression jetTryExpression = stackOfFinallyBlocks.remove(stackOfFinallyBlocks.size()-1);
            gen(jetTryExpression.getFinallyBlock().getFinalExpression(), Type.VOID_TYPE);
            doFinallyOnReturnOrThrow();
            stackOfFinallyBlocks.add(jetTryExpression);
        }
    }
    
    @Override
    public StackValue visitReturnExpression(JetReturnExpression expression, StackValue receiver) {
        final JetExpression returnedExpression = expression.getReturnedExpression();
        if (returnedExpression != null) {
            gen(returnedExpression, returnType);
            doFinallyOnReturnOrThrow();
            v.areturn(returnType);
        }
        else {
            v.visitInsn(Opcodes.RETURN);
        }
        return StackValue.none();
    }

    public void returnExpression(JetExpression expr) {
        StackValue lastValue = gen(expr);
        
        if (lastValue.type != Type.VOID_TYPE) {
            lastValue.put(returnType, v);
            v.areturn(returnType);
        }
        else if (!endsWithReturn(expr)) {
            v.areturn(returnType);
        }
    }

    private static boolean endsWithReturn(JetElement bodyExpression) {
        if (bodyExpression instanceof JetBlockExpression) {
            final List<JetElement> statements = ((JetBlockExpression) bodyExpression).getStatements();
            return statements.size() > 0 && statements.get(statements.size()-1) instanceof JetReturnExpression;
        }
    
        return bodyExpression instanceof JetReturnExpression;
    }
    
    @Override
    public StackValue visitSimpleNameExpression(JetSimpleNameExpression expression, StackValue receiver) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression);

        DeclarationDescriptor descriptor;
        if(resolvedCall == null) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        }
        else
            descriptor = resolvedCall.getResultingDescriptor();

        if (descriptor instanceof NamespaceDescriptor) return StackValue.none(); // No code to generate

        if (descriptor instanceof VariableAsFunctionDescriptor) {
            descriptor = ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        }

        final IntrinsicMethod intrinsic = intrinsics.getIntrinsic(descriptor);
        if (intrinsic != null) {
            final Type expectedType = expressionType(expression);
            return intrinsic.generate(this, v, expectedType, expression, Collections.<JetExpression>emptyList(), receiver);
        }

        assert descriptor != null;
        final DeclarationDescriptor container = descriptor.getContainingDeclaration();

        PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (declaration instanceof PsiField) {
            final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            PsiField psiField = (PsiField) declaration;
            final String owner = typeMapper.getOwner(propertyDescriptor, OwnerKind.IMPLEMENTATION);
            final Type fieldType = typeMapper.mapType(propertyDescriptor.getReturnType());
            final boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
            if (!isStatic) {
                receiver.put(JetTypeMapper.TYPE_OBJECT, v);
            }
            return StackValue.field(fieldType, owner, psiField.getName(), isStatic);
        }
        else {
            int index = lookupLocal(descriptor);
            if (index >= 0) {
                Type sharedVarType = typeMapper.getSharedVarType(descriptor);
                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                if(sharedVarType != null) {
                    return StackValue.shared(index, typeMapper.mapType(outType));
                }
                else {
                    return StackValue.local(index, typeMapper.mapType(outType));
                }
            }
            else if (descriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

                //TODO: hack, will not need if resolve goes to right descriptor itself
                if (declaration instanceof JetParameter) {
                    if (PsiTreeUtil.getParentOfType(expression, JetDelegationSpecifier.class) != null) {
                        JetClass aClass = PsiTreeUtil.getParentOfType(expression, JetClass.class);
                        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, aClass);
                        assert constructorDescriptor != null;
                        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
                        for (ValueParameterDescriptor parameter : parameters) {
                            if (parameter.getName().equals(descriptor.getName())) {
                                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                                return StackValue.local(lookupLocal(parameter), typeMapper.mapType(outType));
                            }
                        }
                    }
                }

                if (declaration instanceof JetObjectDeclarationName) {
                    JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(declaration, JetObjectDeclaration.class);
                    ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, objectDeclaration);
                    return StackValue.field(typeMapper.mapType(classDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION),
                                            typeMapper.mapType(classDescriptor.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(),
                            "$instance",
                            true);
                }
                else {
                    boolean isStatic = container instanceof NamespaceDescriptorImpl;
                    final boolean directToField = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER && contextKind() != OwnerKind.TRAIT_IMPL ;
                    JetExpression r = getReceiverForSelector(expression);
                    final boolean isSuper = r instanceof JetSuperExpression;
                    if(propertyDescriptor.getVisibility() == Visibility.PRIVATE && context.getClassOrNamespaceDescriptor() != propertyDescriptor.getContainingDeclaration()) {
                        DeclarationDescriptor enclosed = propertyDescriptor.getContainingDeclaration();
                        if(enclosed != null && enclosed != context.getThisDescriptor()) {
                            CodegenContext c = context;
                            while(c.getContextDescriptor() != enclosed) {
                                c = c.getParentContext();
                            }
                            propertyDescriptor = (PropertyDescriptor) c.getAccessor(propertyDescriptor);
                        }
                    }
                    final StackValue iValue = intermediateValueForProperty(propertyDescriptor, directToField, isSuper ? (JetSuperExpression)r : null);
                    if(!directToField && resolvedCall != null && !isSuper) {
                        if (resolvedCall.getThisObject().exists()) {
                            if(resolvedCall.getReceiverArgument().exists()) {
                                generateFromResolvedCall(resolvedCall.getThisObject());
                                if(receiver == StackValue.none()) {
                                    generateFromResolvedCall(resolvedCall.getReceiverArgument());
                                }
                                else {
                                    receiver.put(typeMapper.mapType(resolvedCall.getReceiverArgument().getType()), v);
                                }
                            }
                            else {
                                if (receiver == StackValue.none()) {
                                    generateFromResolvedCall(resolvedCall.getThisObject());
                                }
                                else {
                                    ClassDescriptor containingDeclaration = (ClassDescriptor) resolvedCall.getResultingDescriptor().getContainingDeclaration();
                                    if(CodegenUtil.isInterface(containingDeclaration))
                                        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
                                    else
                                        receiver.put(typeMapper.mapType(containingDeclaration.getDefaultType()), v);
                                }
                            }
                        }
                        else {
                            if (resolvedCall.getReceiverArgument().exists()) {
                                if (receiver == StackValue.none()) {
                                    generateFromResolvedCall(resolvedCall.getReceiverArgument());
                                }
                                else {
                                    receiver.put(typeMapper.mapType(resolvedCall.getReceiverArgument().getType()), v);
                                }
                            }
                        }
                        pushTypeArguments(resolvedCall);
                    }
                    else {
                        if (!isStatic) {
                            if (receiver == StackValue.none()) {
                                if(resolvedCall == null)
                                    receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration());
                                else {
                                    if(resolvedCall.getThisObject() instanceof ExtensionReceiver)
                                        receiver = generateReceiver(((ExtensionReceiver)resolvedCall.getThisObject()).getDeclarationDescriptor());
                                    else
                                        receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration());
                                }
                            }
                            JetType receiverType = bindingContext.get(BindingContext.EXPRESSION_TYPE, r);
                            receiver.put(receiverType != null && !isSuper? typeMapper.mapType(receiverType) : JetTypeMapper.TYPE_OBJECT, v);
                            if(receiverType != null) {
                                ClassDescriptor propReceiverDescriptor = (ClassDescriptor) propertyDescriptor.getContainingDeclaration();
                                if(!CodegenUtil.isInterface(propReceiverDescriptor) && CodegenUtil.isInterface(receiverType.getConstructor().getDeclarationDescriptor())) {
                                    // I hope it happens only in case of required super class for traits
                                    assert propReceiverDescriptor != null;
                                    v.checkcast(typeMapper.mapType(propReceiverDescriptor.getDefaultType()));
                                }
                            }
                        }
                    }
                    return iValue;
                }
            }
            else if (descriptor instanceof ClassDescriptor) {
                if(declaration instanceof JetClass) {
                    final JetClassObject classObject = ((JetClass) declaration).getClassObject();
                    if (classObject == null) {
                        throw new UnsupportedOperationException("trying to reference a class which doesn't have a class object");
                    }
                    final ClassDescriptor descriptor1 = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
                    final String type = typeMapper.mapType(descriptor1.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();
                    return StackValue.field(Type.getObjectType(type),
                                            typeMapper.mapType(((ClassDescriptor) descriptor).getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(),
                                                  "$classobj",
                                                  true);
                }
                else {
                    // todo ?
                    return StackValue.none();
                }
            }
            else if (descriptor instanceof TypeParameterDescriptor) {
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
                loadTypeParameterTypeInfo(typeParameterDescriptor);
                v.invokevirtual("jet/typeinfo/TypeInfo", "getClassObject", "()Ljava/lang/Object;");
                v.checkcast(typeMapper.mapType(typeParameterDescriptor.getClassObjectType()));

                return StackValue.onStack(OBJECT_TYPE);
            }
            else {
                // receiver
                StackValue value = context.lookupInContext(descriptor, v, StackValue.local(0, JetTypeMapper.TYPE_OBJECT));
                if (value == null) {
                    throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
                }

                if(value instanceof StackValue.Composed) {
                    StackValue.Composed composed = (StackValue.Composed) value;
                    composed.prefix.put(JetTypeMapper.TYPE_OBJECT, v);
                    value = composed.suffix;
                }
                if(value instanceof StackValue.FieldForSharedVar) {
                    StackValue.FieldForSharedVar fieldForSharedVar = (StackValue.FieldForSharedVar) value;
                    Type sharedType = StackValue.sharedTypeForType(value.type);
                    v.visitFieldInsn(Opcodes.GETFIELD, fieldForSharedVar.owner, fieldForSharedVar.name, sharedType.getDescriptor());
                }
                return value;
            }
        }
    }

    public int lookupLocal(DeclarationDescriptor descriptor) {
        return myFrameMap.getIndex(descriptor);
    }

    public void invokeFunctionNoParams(FunctionDescriptor functionDescriptor, Type type, InstructionAdapter v) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        boolean isStatic = containingDeclaration instanceof NamespaceDescriptorImpl;
        functionDescriptor = functionDescriptor.getOriginal();
        String owner;

        IntrinsicMethod intrinsic = intrinsics.getIntrinsic(functionDescriptor);
        if(intrinsic != null) {
            intrinsic.generate(this, v, type, null, null, StackValue.onStack(JetTypeMapper.TYPE_OBJECT));
            return;
        }

        boolean isInterface;
        boolean isInsideClass = containingDeclaration == context.getThisDescriptor();
        if (isInsideClass || isStatic) {
            owner = typeMapper.getOwner(functionDescriptor, contextKind());
            isInterface = false;
        }
        else {
            owner = typeMapper.getOwner(functionDescriptor, OwnerKind.IMPLEMENTATION);
            if(containingDeclaration instanceof JavaClassDescriptor) {
                isInterface = CodegenUtil.isInterface(containingDeclaration);
            }
            else {
                if(containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.OBJECT)
                    isInterface = false;
                else {
                    JetClass jetClass = (JetClass) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, containingDeclaration);
                    isInterface = jetClass == null || jetClass.isTrait();
                }
            }
        }

        v.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, owner, functionDescriptor.getName(), typeMapper.mapSignature(functionDescriptor.getName(),functionDescriptor).getDescriptor());
        StackValue.onStack(typeMapper.mapType(functionDescriptor.getReturnType())).coerce(type, v);
    }

    public StackValue intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, @Nullable JetSuperExpression superExpression) {
        boolean isSuper = superExpression != null;

        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        assert containingDeclaration != null;
        containingDeclaration = containingDeclaration.getOriginal();

        boolean isStatic = containingDeclaration instanceof NamespaceDescriptorImpl;
        propertyDescriptor = propertyDescriptor.getOriginal();
        boolean isInsideClass = ((containingDeclaration == context.getThisDescriptor()) ||
                                 (context.getParentContext() instanceof CodegenContext.NamespaceContext) && context.getParentContext().getContextDescriptor() == containingDeclaration)
                                && contextKind() != OwnerKind.TRAIT_IMPL;
        Method getter;
        Method setter;
        if (forceField) {
            getter = null;
            setter = null;
        }
        else {
            //noinspection ConstantConditions
            if (isInsideClass && (propertyDescriptor.getGetter() == null || propertyDescriptor.getGetter().isDefault())) {
                getter = null;
            }
            else {
                if(isSuper) {
                    PsiElement enclosingElement = bindingContext.get(BindingContext.LABEL_TARGET, superExpression.getTargetLabel());
                    ClassDescriptor enclosed = (ClassDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, enclosingElement);
                    if(!CodegenUtil.isInterface(containingDeclaration)) {
                        if(enclosed != null && enclosed != context.getThisDescriptor()) {
                            CodegenContext c = context;
                            while(c.getContextDescriptor() != enclosed) {
                                c = c.getParentContext();
                            }
                            propertyDescriptor = (PropertyDescriptor) c.getAccessor(propertyDescriptor);
                            isSuper = false;
                        }
                    }
                }
                getter = typeMapper.mapGetterSignature(propertyDescriptor, OwnerKind.IMPLEMENTATION);
            }
            //noinspection ConstantConditions
            if (isInsideClass && (propertyDescriptor.getSetter() == null || propertyDescriptor.getSetter().isDefault())) {
                setter = null;
            }
            else {
                setter = typeMapper.mapSetterSignature(propertyDescriptor, OwnerKind.IMPLEMENTATION);
            }
        }

        String owner;
        boolean isInterface;
        if (isInsideClass || isStatic) {
            owner = typeMapper.getOwner(propertyDescriptor, contextKind());
            isInterface = false;
        }
        else {
            owner = typeMapper.getOwner(propertyDescriptor, OwnerKind.IMPLEMENTATION);
            if(containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.OBJECT)
                isInterface = false;
            else {
                JetClass jetClass = (JetClass) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, containingDeclaration);
                isInterface = jetClass == null || jetClass.isTrait();
            }
        }

        return StackValue.property(propertyDescriptor.getName(), owner, typeMapper.mapType(propertyDescriptor.getOutType()), isStatic, isInterface, isSuper, getter, setter);
    }

    @Override
    public StackValue visitCallExpression(JetCallExpression expression, StackValue receiver) {
        final JetExpression callee = expression.getCalleeExpression();
        DeclarationDescriptor funDescriptor = resolveCalleeDescriptor(expression);

        if (funDescriptor instanceof ConstructorDescriptor) {
            return generateConstructorCall(expression, (JetSimpleNameExpression) callee, receiver);
        }
        else if (funDescriptor instanceof FunctionDescriptor) {
            final FunctionDescriptor fd = (FunctionDescriptor) funDescriptor;
            return invokeFunction(expression, fd, receiver);
        }
        else {
            throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);
        }
    }

    private StackValue invokeFunction(JetCallExpression expression, DeclarationDescriptor fd, StackValue receiver) {
        boolean superCall = false;
        if (expression.getParent() instanceof JetQualifiedExpression) {
            final JetExpression receiverExpression = ((JetQualifiedExpression) expression.getParent()).getReceiverExpression();
            if (receiverExpression instanceof JetSuperExpression) {
                assert receiver == StackValue.none();
                superCall = true;
                receiver = StackValue.thisOrOuter(this, context.getThisDescriptor());
                JetSuperExpression superExpression = (JetSuperExpression) receiverExpression;
                PsiElement enclosingElement = bindingContext.get(BindingContext.LABEL_TARGET, superExpression.getTargetLabel());
                ClassDescriptor enclosed = (ClassDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, enclosingElement);
                if(!CodegenUtil.isInterface(fd.getContainingDeclaration())) {
                    if(enclosed != null && enclosed != context.getThisDescriptor()) {
                        CodegenContext c = context;
                        while(c.getContextDescriptor() != enclosed) {
                            c = c.getParentContext();
                        }
                        fd = c.getAccessor(fd);
                        superCall = false;
                        receiver = StackValue.thisOrOuter(this, enclosed);
                    }
                }
            }
        }

        Callable callable = resolveToCallable(fd, superCall);
        if (callable instanceof CallableMethod) {
            final CallableMethod callableMethod = (CallableMethod) callable;
            invokeMethodWithArguments(callableMethod, expression, receiver);

            final Type callReturnType = callableMethod.getSignature().getReturnType();
            return returnValueAsStackValue((FunctionDescriptor) fd, callReturnType);
        }
        else {
            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            List<JetExpression> args = new ArrayList<JetExpression>();
            for (ValueArgument argument : expression.getValueArguments()) {
                args.add(argument.getArgumentExpression());
            }
            return intrinsic.generate(this, v, expressionType(expression), expression, args, receiver);
        }
    }

    private StackValue returnValueAsStackValue(FunctionDescriptor fd, Type callReturnType) {
        if (callReturnType != Type.VOID_TYPE) {
            final Type retType = typeMapper.mapReturnType(fd.getReturnType());
            StackValue.onStack(callReturnType).upcast(retType, v);
            return StackValue.onStack(retType);
        }
        return StackValue.none();
    }

    Callable resolveToCallable(DeclarationDescriptor fd, boolean superCall) {
        final IntrinsicMethod intrinsic = intrinsics.getIntrinsic(fd);
        if (intrinsic != null) {
            return intrinsic;
        }

        CallableMethod callableMethod;
        if (fd instanceof VariableAsFunctionDescriptor) {
            assert !superCall;
            callableMethod = ClosureCodegen.asCallableMethod((FunctionDescriptor) fd);
        }
        else if (fd instanceof ExpressionAsFunctionDescriptor) {
            FunctionDescriptor invoke = CodegenUtil.createInvoke((ExpressionAsFunctionDescriptor) fd);
            callableMethod = ClosureCodegen.asCallableMethod(invoke);
        }
        else if (fd instanceof FunctionDescriptor) {
            callableMethod = typeMapper.mapToCallableMethod((FunctionDescriptor) fd, superCall, OwnerKind.IMPLEMENTATION);
        }
        else {
            throw new UnsupportedOperationException("can't resolve declaration to callable: " + fd);
        }
        return callableMethod;
    }

    private DeclarationDescriptor resolveCalleeDescriptor(JetCallExpression call) {
        JetExpression callee = call.getCalleeExpression();
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, callee);
        if(resolvedCall == null) {
            assert callee != null;
            throw new CompilationException("Cannot resolve: " + callee.getText());
        }

        return resolvedCall.getResultingDescriptor();
    }

    public void invokeMethodWithArguments(CallableMethod callableMethod, JetCallElement expression, StackValue receiver) {
        final Type calleeType = callableMethod.getGenerateCalleeType();
        if (calleeType != null && expression instanceof JetCallExpression) {
            assert !callableMethod.isNeedsThis();
            gen(expression.getCalleeExpression(), calleeType);
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
        assert resolvedCall != null;

        genThisAndReceiverFromResolvedCall(callableMethod, receiver, resolvedCall);

        pushTypeArguments(resolvedCall);
        int mask = pushMethodArguments(expression, callableMethod.getValueParameterTypes());
        if(mask == 0)
            callableMethod.invoke(v);
        else
            callableMethod.invokeWithDefault(v, mask);
    }

    private void genThisAndReceiverFromResolvedCall(CallableMethod callableMethod, StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall) {
        if (callableMethod.isNeedsThis()) {
            if(callableMethod.isNeedsReceiver()) {
                generateFromResolvedCall(resolvedCall.getThisObject());
                if(receiver == StackValue.none()) {
                    generateFromResolvedCall(resolvedCall.getReceiverArgument());
                }
                else {
                    receiver.put(typeMapper.mapType(resolvedCall.getReceiverArgument().getType()), v);
                }
            }
            else {
                if (receiver == StackValue.none()) {
                    generateFromResolvedCall(resolvedCall.getThisObject());
                }
                else {
                    receiver.put(JetTypeMapper.TYPE_OBJECT, v);
                }
            }
        }
        else {
            if (callableMethod.isNeedsReceiver()) {
                if (receiver == StackValue.none()) {
                    generateFromResolvedCall(resolvedCall.getReceiverArgument());
                }
                else
                    receiver.put(callableMethod.getSignature().getArgumentTypes()[0], v);
            }
        }
    }

    protected void generateFromResolvedCall(ReceiverDescriptor descriptor) {
        if(descriptor instanceof ClassReceiver) {
            ClassReceiver classReceiver = (ClassReceiver) descriptor;
            generateThisOrOuter((ClassDescriptor) classReceiver.getDeclarationDescriptor()).put(typeMapper.mapType(descriptor.getType()), v);
        }
        else if(descriptor instanceof ExtensionReceiver) {
            ExtensionReceiver extensionReceiver = (ExtensionReceiver) descriptor;
            generateReceiver(extensionReceiver.getDeclarationDescriptor()).put(typeMapper.mapType(descriptor.getType()), v);
        }
        else if(descriptor instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver) descriptor;
            genToJVMStack(expressionReceiver.getExpression());
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    private static JetExpression getReceiverForSelector(PsiElement expression) {
        if (expression.getParent() instanceof JetDotQualifiedExpression && !isReceiver(expression)) {
            final JetDotQualifiedExpression parent = (JetDotQualifiedExpression) expression.getParent();
            return parent.getReceiverExpression();
        }
        return null;
    }

    private StackValue generateReceiver(DeclarationDescriptor provided) {
        assert context instanceof CodegenContext.ReceiverContext;
        CodegenContext.ReceiverContext cur = (CodegenContext.ReceiverContext) context;
        if (cur.getReceiverDescriptor() == provided) {
            StackValue result = cur.getReceiverExpression(typeMapper);
            return castToRequiredTypeOfInterfaceIfNeeded(result, provided, null);
        }

        StackValue result = context.lookupInContext(provided, v, StackValue.local(0, JetTypeMapper.TYPE_OBJECT));
        return castToRequiredTypeOfInterfaceIfNeeded(result, provided, null);
    }

    public StackValue generateThisOrOuter(ClassDescriptor calleeContainingClass) {
        CodegenContext cur = context;

        StackValue result = StackValue.local(0, JetTypeMapper.TYPE_OBJECT);
        while (cur != null) {
            if(cur instanceof CodegenContext.MethodContext && !(cur instanceof CodegenContext.ConstructorContext))
                cur = cur.getParentContext();

            if (CodegenUtil.isSubclass(cur.getThisDescriptor(), calleeContainingClass)) {
                Type type = typeMapper.mapType(calleeContainingClass.getDefaultType());
                result.put(JetTypeMapper.TYPE_OBJECT, v);
                return castToRequiredTypeOfInterfaceIfNeeded(StackValue.onStack(type), cur.getThisDescriptor(), calleeContainingClass);
            }

            result = cur.getOuterExpression(result);

            if(cur instanceof CodegenContext.ConstructorContext) {
                cur = cur.getParentContext();
            }
            cur = cur.getParentContext();
        }
        
        throw new UnsupportedOperationException();
    }

    private static boolean isReceiver(PsiElement expression) {
        final PsiElement parent = expression.getParent();
        if (parent instanceof JetQualifiedExpression) {
            final JetExpression receiverExpression = ((JetQualifiedExpression) parent).getReceiverExpression();
            return expression == receiverExpression;
        }
        return false;
    }

    private int pushMethodArguments(ResolvedCall resolvedCall, List<Type> valueParameterTypes) {
        Map<ValueParameterDescriptor,ResolvedValueArgument> valueArguments = resolvedCall.getValueArguments();
        CallableDescriptor fd = resolvedCall.getResultingDescriptor();

        int index = 0;
        int mask  = 0;
        for (ValueParameterDescriptor valueParameterDescriptor : fd.getValueParameters()) {
            ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor);
            if(resolvedValueArgument instanceof ExpressionValueArgument) {
                ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;
                gen(valueArgument.getExpression(), valueParameterTypes.get(index));
            }
            else if(resolvedValueArgument instanceof DefaultValueArgument) {
                Type type = valueParameterTypes.get(index);
                if(type.getSort() == Type.OBJECT||type.getSort() == Type.ARRAY)
                    v.aconst(null);
                else if(type.getSort() == Type.FLOAT) {
                    v.aconst(0f);
                }
                else if(type.getSort() == Type.DOUBLE) {
                    v.aconst(0d);
                }
                else {
                    v.iconst(0);
                }
                mask |= (1 << index);
            }
            else if(resolvedValueArgument instanceof VarargValueArgument) {
                VarargValueArgument valueArgument = (VarargValueArgument) resolvedValueArgument;
                JetType outType = valueParameterDescriptor.getOutType();

                Type type = typeMapper.mapType(outType);
                assert type.getSort() == Type.ARRAY;
                Type elementType = JetTypeMapper.correctElementType(type);
                int size = valueArgument.getArgumentExpressions().size();

                v.iconst(valueArgument.getArgumentExpressions().size());
                v.newarray(elementType);
                for(int i = 0; i != size; ++i) {
                    v.dup();
                    v.iconst(i);
                    gen(valueArgument.getArgumentExpressions().get(i), elementType);
                    StackValue.arrayElement(elementType, false).store(v);
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
            index++;
        }
        return mask;
    }

    private int pushMethodArguments(JetCallElement expression, List<Type> valueParameterTypes) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
        if(resolvedCall != null) {
            return pushMethodArguments(resolvedCall, valueParameterTypes);
        }
        else {
            List<? extends ValueArgument> args = expression.getValueArguments();
            for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
                ValueArgument arg = args.get(i);
                gen(arg.getArgumentExpression(), valueParameterTypes.get(i));
            }
            return 0;
        }
    }

    public Type expressionType(JetExpression expr) {
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expr);
        return type == null ? Type.VOID_TYPE : typeMapper.mapType(type);
    }

    public int indexOfLocal(JetReferenceExpression lhs) {
        final DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, lhs);
        return lookupLocal(declarationDescriptor);
    }
    
    

    @Override
    public StackValue visitDotQualifiedExpression(JetDotQualifiedExpression expression, StackValue receiver) {
        StackValue receiverValue = resolvesToClassOrPackage(expression.getReceiverExpression())
                                   ? StackValue.none()
                                   : genQualified(receiver, expression.getReceiverExpression());
        return genQualified(receiverValue, expression.getSelectorExpression());
    }

    private boolean resolvesToClassOrPackage(JetExpression receiver) {
        if (receiver instanceof JetReferenceExpression) {
            DeclarationDescriptor declaration = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) receiver);
            PsiElement declarationElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declaration);
            if (declarationElement instanceof PsiClass) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StackValue visitSafeQualifiedExpression(JetSafeQualifiedExpression expression, StackValue receiver) {
        JetExpression expr = expression.getReceiverExpression();
        JetType receiverJetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getReceiverExpression());
        Type receiverType = typeMapper.mapType(receiverJetType);
        gen(expr, receiverType);
        if(receiverType.getSort() != Type.OBJECT && receiverType.getSort() != Type.ARRAY) {
            StackValue propValue = genQualified(StackValue.onStack(receiverType), expression.getSelectorExpression());
            Type type = propValue.type;
            propValue.put(type, v);
            if(JetTypeMapper.isPrimitive(type) && !type.equals(Type.VOID_TYPE)) {
                StackValue.valueOf(v, type);
                type = JetTypeMapper.boxType(type);
            }

            return StackValue.onStack(type);
        }
        else {
            Label ifnull = new Label();
            Label end = new Label();
            v.dup();
            v.ifnull(ifnull);
            StackValue propValue = genQualified(StackValue.onStack(receiverType), expression.getSelectorExpression());
            Type type = propValue.type;
            propValue.put(type, v);
            if(JetTypeMapper.isPrimitive(type) && !type.equals(Type.VOID_TYPE)) {
                StackValue.valueOf(v, type);
                type = JetTypeMapper.boxType(type);
            }
            v.goTo(end);

            v.mark(ifnull);
            v.pop();
            if(!propValue.type.equals(Type.VOID_TYPE)) {
                v.aconst(null);
            }
            v.mark(end);

            return StackValue.onStack(type);
        }
    }

    @Override
    public StackValue visitPredicateExpression(JetPredicateExpression expression, StackValue receiver) {
        JetExpression expr = expression.getReceiverExpression();
        StackValue value = gen(expr);
        Type receiverType = expressionType(expr);
        value.put(receiverType, v);
        Label ifFalse = new Label();
        Label end = new Label();
        v.dup();
        StackValue result = genQualified(StackValue.onStack(receiverType), expression.getSelectorExpression());
        result.condJump(ifFalse, true, v);
        v.goTo(end);
        v.mark(ifFalse);
        v.pop();
        v.aconst(null);
        v.mark(end);
        return StackValue.onStack(expressionType(expression));
    }

    @Override
    public StackValue visitBinaryExpression(JetBinaryExpression expression, StackValue receiver) {
        final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
        if (opToken == JetTokens.EQ) {
            return generateAssignmentExpression(expression);
        }
        else if (JetTokens.AUGMENTED_ASSIGNMENTS.contains(opToken)) {
            return generateAugmentedAssignment(expression);
        }
        else if (opToken == JetTokens.ANDAND) {
            return generateBooleanAnd(expression);
        }
        else if (opToken == JetTokens.OROR) {
            return generateBooleanOr(expression);
        }
        else if (opToken == JetTokens.EQEQ || opToken == JetTokens.EXCLEQ ||
                 opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
            return generateEquals(expression.getLeft(), expression.getRight(), opToken);
        }
        else if (opToken == JetTokens.LT || opToken == JetTokens.LTEQ ||
                 opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
            return generateCompareOp(expression.getLeft(), expression.getRight(), opToken, expressionType(expression.getLeft()));
        }
        else if (opToken == JetTokens.ELVIS) {
            return generateElvis(expression);
        }
        else if(opToken == JetTokens.IN_KEYWORD || opToken == JetTokens.NOT_IN) {
            return generateIn (expression);
        }
        else {
            DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
            final Callable callable = resolveToCallable(op, false);
            if (callable instanceof IntrinsicMethod) {
                IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
                return intrinsic.generate(this, v, expressionType(expression), expression,
                                          Arrays.asList(expression.getLeft(), expression.getRight()), receiver);
            }
            else {
                ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());

                CallableMethod callableMethod = (CallableMethod) callable;
                genThisAndReceiverFromResolvedCall(callableMethod, StackValue.none(), resolvedCall);
                pushTypeArguments(resolvedCall);
                pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());
                callableMethod.invoke(v);
                return  returnValueAsStackValue((FunctionDescriptor) op, callableMethod.getSignature().getReturnType());
            }
        }
    }

    private StackValue generateIn(JetBinaryExpression expression) {
        JetExpression expr = expression.getLeft();
        StackValue leftValue = gen(expr);
        if(isIntRangeExpr(expression.getRight())) {
            JetBinaryExpression rangeExpression = (JetBinaryExpression) expression.getRight();
            boolean inverted = expression.getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IN;
            getInIntRange(leftValue, rangeExpression, inverted);
        }
        else {
            FunctionDescriptor op = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
            assert op != null;
            leftValue.put(typeMapper.mapType(op.getValueParameters().get(0).getOutType()), v);
            genToJVMStack(expression.getRight());
            v.swap();
            invokeFunctionNoParams(op, Type.BOOLEAN_TYPE, v);
        }
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private void getInIntRange(StackValue leftValue, JetBinaryExpression rangeExpression, boolean inverted) {
        v.iconst(1);
        // 1
        leftValue.put(Type.INT_TYPE, v);
        // 1 l
        v.dup2();
        // 1 l 1 l

        //noinspection ConstantConditions
        gen(rangeExpression.getLeft(), Type.INT_TYPE);
        // 1 l 1 l r
        Label lok = new Label();
        v.ificmpge(lok);
        // 1 l 1
        v.pop();
        v.iconst(0);
        v.mark(lok);
        // 1 l c
        v.dupX2();
        // c 1 l c
        v.pop();
        // c 1 l

        gen(rangeExpression.getRight(), Type.INT_TYPE);
        // c 1 l r
        Label rok = new Label();
        v.ificmple(rok);
        // c 1
        v.pop();
        v.iconst(0);
        v.mark(rok);
        // c c

        v.and(Type.INT_TYPE);
        if(inverted) {
            v.iconst(1);
            v.xor(Type.INT_TYPE);
        }
    }

    private StackValue generateBooleanAnd(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifFalse = new Label();
        v.ifeq(ifFalse);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifFalse);
        v.iconst(0);
        v.mark(end);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateBooleanOr(JetBinaryExpression expression) {
        gen(expression.getLeft(), Type.BOOLEAN_TYPE);
        Label ifTrue = new Label();
        v.ifne(ifTrue);
        gen(expression.getRight(), Type.BOOLEAN_TYPE);
        Label end = new Label();
        v.goTo(end);
        v.mark(ifTrue);
        v.iconst(1);
        v.mark(end);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateEquals(JetExpression left, JetExpression right, IElementType opToken) {
        JetType leftJetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, left);
        Type leftType = typeMapper.mapType(leftJetType);
        JetType rightJetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, right);
        Type rightType = typeMapper.mapType(rightJetType);
        if(leftType == JetTypeMapper.TYPE_NOTHING) {
            return genCmpWithNull(right, rightType, opToken);
        }

        if(rightType == JetTypeMapper.TYPE_NOTHING) {
            return genCmpWithNull(left, leftType, opToken);
        }

        if(JetTypeMapper.isPrimitive(leftType) != JetTypeMapper.isPrimitive(rightType)) {
            gen(left, leftType);
            StackValue.valueOf(v, leftType);
            leftType = JetTypeMapper.boxType(leftType);
            gen(right, rightType);
            StackValue.valueOf(v, rightType);
            rightType = JetTypeMapper.boxType(rightType);
        }
        else {
            gen(left, leftType);
            gen(right, rightType);
        }

        if(JetTypeMapper.isPrimitive(leftType)) // both are primitive
            return generateEqualsForExpressionsOnStack(opToken, leftType, rightType, false, false);

        assert leftJetType != null;
        assert rightJetType != null;
        return generateEqualsForExpressionsOnStack(opToken, leftType, rightType, leftJetType.isNullable(), rightJetType.isNullable());
    }

    private StackValue genCmpWithNull(JetExpression exp, Type expType, IElementType opToken) {
        v.iconst(1);
        gen(exp, JetTypeMapper.boxType(expType));
        Label ok = new Label();
        if(JetTokens.EQEQ == opToken || JetTokens.EQEQEQ == opToken) {
            v.ifnull(ok);
        }
        else {
            v.ifnonnull(ok);
        }
        v.pop();
        v.iconst(0);
        v.mark(ok);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    public StackValue generateEqualsForExpressionsOnStack(IElementType opToken, Type leftType, Type rightType, boolean leftNullable, boolean rightNullable) {
        if (isNumberPrimitive(leftType) && leftType == rightType) {
            return compareExpressionsOnStack(opToken, leftType);
        }
        else {
            if (opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
                return StackValue.cmp(opToken, leftType);
            }
            else {
                return generateNullSafeEquals(opToken, leftNullable, rightNullable);
            }
        }
    }

    private StackValue generateNullSafeEquals(IElementType opToken, boolean leftNullable, boolean rightNullable) {
        if(!leftNullable) {
            v.invokevirtual(CLASS_OBJECT, "equals", "(Ljava/lang/Object;)Z");
            if (opToken == JetTokens.EXCLEQ) {
                v.iconst(1);
                v.xor(Type.INT_TYPE);
            }
        }
        else {
            if(rightNullable) {
                v.dup2();   // left right left right
                Label rightNull = new Label();
                v.ifnull(rightNull);
                Label leftNull = new Label();
                v.ifnull(leftNull);
                v.invokevirtual(CLASS_OBJECT, "equals", "(Ljava/lang/Object;)Z");
                if (opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ) {
                    v.iconst(1);
                    v.xor(Type.INT_TYPE);
                }
                Label end = new Label();
                v.goTo(end);
                v.mark(rightNull);
                // left right left
                Label bothNull = new Label();
                v.ifnull(bothNull);
                v.mark(leftNull);
                v.pop2();
                v.iconst(opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ ? 1 : 0);
                v.goTo(end);
                v.mark(bothNull);
                v.pop2();
                v.iconst(opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ ? 0 : 1);
                v.mark(end);
            }
            else {
                v.dup2();   // left right left right
                v.pop();
                Label leftNull = new Label();
                v.ifnull(leftNull);
                v.invokevirtual(CLASS_OBJECT, "equals", "(Ljava/lang/Object;)Z");
                if (opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ) {
                    v.iconst(1);
                    v.xor(Type.INT_TYPE);
                }
                Label end = new Label();
                v.goTo(end);
                // left right
                v.mark(leftNull);
                v.pop2();
                v.iconst(opToken == JetTokens.EXCLEQ ? 1 : 0);
                v.mark(end);
            }
        }

        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateElvis(JetBinaryExpression expression) {
        final Type exprType = expressionType(expression);
        final Type leftType = expressionType(expression.getLeft());
        gen(expression.getLeft(), leftType);
        v.dup();
        Label end = new Label();
        Label ifNull = new Label();
        v.ifnull(ifNull);
        StackValue.onStack(leftType).put(exprType, v);
        v.goTo(end);
        v.mark(ifNull);
        v.pop();
        gen(expression.getRight(), exprType);
        v.mark(end);
        return StackValue.onStack(exprType);
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
        return JetTypeMapper.isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    private StackValue generateCompareOp(JetExpression left, JetExpression right, IElementType opToken, Type operandType) {
        gen(left, operandType);
        gen(right, operandType);
        return compareExpressionsOnStack(opToken, operandType);
    }

    private StackValue compareExpressionsOnStack(IElementType opToken, Type operandType) {
        if (operandType.getSort() == Type.OBJECT) {
            v.invokeinterface(CLASS_COMPARABLE, "compareTo", "(Ljava/lang/Object;)I");
            v.iconst(0);
            operandType = Type.INT_TYPE;
        }
        return StackValue.cmp(opToken, operandType);
    }

    private StackValue generateAssignmentExpression(JetBinaryExpression expression) {
        StackValue stackValue = gen(expression.getLeft());
        gen(expression.getRight(), stackValue.type);
        stackValue.store(v);
        return StackValue.none();
    }

    private StackValue generateAugmentedAssignment(JetBinaryExpression expression) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        final Callable callable = resolveToCallable(op, false);
        final JetExpression lhs = expression.getLeft();

//        if(lhs instanceof JetArrayAccessExpression) {
//            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) lhs;
//            if(arrayAccessExpression.getIndexExpressions().size() != 1) {
//                throw new UnsupportedOperationException("Augmented assignment with multi-index");
//            }
//        }

        Type lhsType = expressionType(lhs);
        //noinspection ConstantConditions
        if (bindingContext.get(BindingContext.VARIABLE_REASSIGNMENT, expression)) {
            if (callable instanceof IntrinsicMethod) {
                StackValue value = gen(lhs);              // receiver
                value.dupReceiver(v);                                        // receiver receiver
                value.put(lhsType, v);                                          // receiver lhs
                final IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
                //noinspection NullableProblems
                intrinsic.generate(this, v, lhsType, expression, Arrays.asList(expression.getRight()), null);
                value.store(v);
            }
            else {
                callAugAssignMethod(expression, (CallableMethod) callable, lhsType, true);
            }
        }
        else {
            assert op != null;
            final boolean keepReturnValue = !((FunctionDescriptor) op).getReturnType().equals(JetStandardClasses.getUnitType());
            callAugAssignMethod(expression, (CallableMethod) callable, lhsType, keepReturnValue);
        }
        
        return StackValue.none();
    }

    private void callAugAssignMethod(JetBinaryExpression expression, CallableMethod callable, Type lhsType, final boolean keepReturnValue) {
        StackValue value = gen(expression.getLeft());
        if (keepReturnValue) {
            value.dupReceiver(v);
        }
        value.put(lhsType, v);
        genToJVMStack(expression.getRight());
        callable.invoke(v);
        if (keepReturnValue) {
            value.store(v);
        }
    }

    public void generateStringBuilderConstructor() {
        Type type = JetTypeMapper.JL_STRING_BUILDER;
        v.anew(type);
        v.dup();
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        v.invokespecial(CLASS_STRING_BUILDER, method.getName(), method.getDescriptor());
    }

    public void invokeAppend(final JetExpression expr) {
        if (expr instanceof JetBinaryExpression) {
            final JetBinaryExpression binaryExpression = (JetBinaryExpression) expr;
            if (binaryExpression.getOperationToken() == JetTokens.PLUS) {
                invokeAppend(binaryExpression.getLeft());
                invokeAppend(binaryExpression.getRight());
                return;
            }
        }
        Type exprType = expressionType(expr);
        gen(expr, exprType);
        invokeAppendMethod(exprType.getSort() == Type.ARRAY ? JetTypeMapper.TYPE_OBJECT : exprType);
    }

    public void invokeAppendMethod(Type exprType) {
        Method appendDescriptor = new Method("append", JetTypeMapper.JL_STRING_BUILDER,
                new Type[] { exprType.getSort() == Type.OBJECT ? JetTypeMapper.TYPE_OBJECT : exprType});
        v.invokevirtual(CLASS_STRING_BUILDER, "append", appendDescriptor.getDescriptor());
    }

    @Override
    public StackValue visitPrefixExpression(JetPrefixExpression expression, StackValue receiver) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationSign());
        final Callable callable = resolveToCallable(op, false);
        if (callable instanceof IntrinsicMethod) {
            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            return intrinsic.generate(this, v, expressionType(expression), expression,
                                      Arrays.asList(expression.getBaseExpression()), receiver);
        }
        else {
            CallableMethod callableMethod = (CallableMethod) callable;
            genToJVMStack(expression.getBaseExpression());
            callableMethod.invoke(v);
            return returnValueAsStackValue((FunctionDescriptor) op, callableMethod.getSignature().getReturnType());
        }
    }

    @Override
    public StackValue visitPostfixExpression(JetPostfixExpression expression, StackValue receiver) {
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationSign());
        if (op instanceof FunctionDescriptor) {
            final Type asmType = expressionType(expression);
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (isNumberPrimitive(cls) && (op.getName().equals("inc") || op.getName().equals("dec"))) {
                receiver.put(receiver.type, v);
                gen(expression.getBaseExpression(), asmType);                               // old value
                generateIncrement(op, asmType, expression.getBaseExpression(), receiver);   // increment in-place
                return StackValue.onStack(asmType);                                         // old value
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate this prefix expression");
    }

    private void generateIncrement(DeclarationDescriptor op, Type asmType, JetExpression operand, StackValue receiver) {
        int increment = op.getName().equals("inc") ? 1 : -1;
        if (operand instanceof JetReferenceExpression) {
            final int index = indexOfLocal((JetReferenceExpression) operand);
            if (index >= 0 && JetTypeMapper.isIntPrimitive(asmType)) {
                v.iinc(index, increment);
                return;
            }
        }
        StackValue value = genQualified(receiver, operand);
        value.dupReceiver(v);
        value.put(asmType, v);
        if (asmType == Type.LONG_TYPE) {
            //noinspection UnnecessaryBoxing
            v.lconst(increment);
        }
        else if (asmType == Type.FLOAT_TYPE) {
            //noinspection UnnecessaryBoxing
            v.fconst(increment);
        }
        else if (asmType == Type.DOUBLE_TYPE) {
            //noinspection UnnecessaryBoxing
            v.dconst(increment);
        }
        else {
            v.iconst(increment);
        }
        v.add(asmType);
        value.store(v);
    }

    @Override
    public StackValue visitProperty(JetProperty property, StackValue receiver) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        int index = lookupLocal(variableDescriptor);

        assert index >= 0;

        final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
        assert variableDescriptor != null;
        Type varType = typeMapper.mapType(variableDescriptor.getOutType());
        if(sharedVarType != null) {
            v.anew(sharedVarType);
            v.dup();
            v.invokespecial(sharedVarType.getInternalName(), "<init>", "()V");
            v.store(index, JetTypeMapper.TYPE_OBJECT);
        }

        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            if(sharedVarType == null) {
                gen(initializer, varType);
                v.store(index, varType);
            }
            else {
                v.load(index, JetTypeMapper.TYPE_OBJECT);
                gen(initializer, varType);
                v.putfield(sharedVarType.getInternalName(), "ref", sharedVarType == JetTypeMapper.TYPE_SHARED_VAR ? "Ljava/lang/Object;" : varType.getDescriptor());
            }
        }
        return StackValue.none();
    }

    private StackValue generateConstructorCall(JetCallExpression expression, JetSimpleNameExpression constructorReference, StackValue receiver) {
        DeclarationDescriptor constructorDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, constructorReference);
        final PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, constructorDescriptor);
        Type type;
        if (declaration instanceof PsiMethod) {
            type = generateJavaConstructorCall(expression);
        }
        else if (constructorDescriptor instanceof ConstructorDescriptor) {
            //noinspection ConstantConditions
            JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
            assert expressionType != null;
            type = typeMapper.mapType(expressionType, OwnerKind.IMPLEMENTATION);
            if (type.getSort() == Type.ARRAY) {
                generateNewArray(expression, expressionType);
            } else {
                ClassDescriptor classDecl = (ClassDescriptor) constructorDescriptor.getContainingDeclaration();

                receiver.put(receiver.type, v);
                v.anew(type);

                // TODO typechecker must verify that we're the outer class of the instance being created
                //noinspection ConstantConditions
                if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
                    if(!receiver.type.equals(Type.VOID_TYPE)) {
                        // class object is in receiver
                        v.dupX1();
                        v.swap();
                    }
                    else {
                        // this$0 need to be put on stack
                        v.dup();
                        v.load(0, typeMapper.mapType(classDecl.getDefaultType(), OwnerKind.IMPLEMENTATION));
                    }
                }
                else {
                    // regular case
                    v.dup();
                }

                CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) constructorDescriptor, OwnerKind.IMPLEMENTATION);
                invokeMethodWithArguments(method, expression, StackValue.none());
            }
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate this new expression");
        }
        return StackValue.onStack(type);
    }

    private void pushTypeArguments(ResolvedCall<? extends CallableDescriptor> resolvedCall) {
        if(resolvedCall != null) {
            Map<TypeParameterDescriptor, JetType> typeArguments = resolvedCall.getTypeArguments();
            CallableDescriptor resultingDescriptor = resolvedCall.getCandidateDescriptor();
            for (TypeParameterDescriptor typeParameterDescriptor : resultingDescriptor.getTypeParameters()) {
                if(typeParameterDescriptor.isReified()) {
                    JetType jetType = typeArguments.get(typeParameterDescriptor);
                    generateTypeInfo(jetType);
                }
            }
        }
        else {
            throw new UnsupportedOperationException();
//            for (JetTypeProjection jetTypeArgument : expression.getTypeArguments()) {
//                pushTypeArgument(jetTypeArgument);
//            }
        }
    }

    public void pushTypeArgument(JetTypeProjection jetTypeArgument) {
        JetType typeArgument = bindingContext.get(BindingContext.TYPE, jetTypeArgument.getTypeReference());
        generateTypeInfo(typeArgument);
    }

    private Type generateJavaConstructorCall(JetCallExpression expression) {
        FunctionDescriptor descriptor = (FunctionDescriptor) resolveCalleeDescriptor(expression);
        ClassDescriptor javaClass = (ClassDescriptor) descriptor.getContainingDeclaration();
        Type type = typeMapper.mapType(javaClass.getDefaultType());
        v.anew(type);
        v.dup();
        final CallableMethod callableMethod = typeMapper.mapToCallableMethod(descriptor, false, OwnerKind.IMPLEMENTATION);
        invokeMethodWithArguments(callableMethod, expression, StackValue.none());
        return type;
    }

    private void generateNewArray(JetCallExpression expression, JetType arrayType) {
        List<? extends ValueArgument> args = expression.getValueArguments();

        boolean isArray = state.getStandardLibrary().getArray().equals(arrayType.getConstructor().getDeclarationDescriptor());
        if(isArray) {
            if (args.size() != 2 && !arrayType.getArguments().get(0).getType().isNullable()) {
                throw new CompilationException("array constructor of non-nullable type requires two arguments");
            }
        }
        else {
            if (args.size() != 1) {
                throw new CompilationException("primitive array constructor requires one argument");
            }
        }

        if(isArray) {
            JetType elementType = typeMapper.getGenericsElementType(arrayType);
            if(elementType != null) {
                generateTypeInfo(elementType);
                gen(args.get(0).getArgumentExpression(), Type.INT_TYPE);
                v.invokevirtual("jet/typeinfo/TypeInfo", "newArray", "(I)[Ljava/lang/Object;");
            }
            else {
                gen(args.get(0).getArgumentExpression(), Type.INT_TYPE);
                v.newarray(JetTypeMapper.boxType(typeMapper.mapType(arrayType.getArguments().get(0).getType())));
            }
        }
        else {
            Type type = typeMapper.mapType(arrayType, OwnerKind.IMPLEMENTATION);
            gen(args.get(0).getArgumentExpression(), Type.INT_TYPE);
            v.newarray(JetTypeMapper.correctElementType(type));
        }

        if(args.size() == 2) {
            int sizeIndex  = myFrameMap.enterTemp(2);
            int indexIndex = sizeIndex+1;

            v.dup();
            v.arraylength();
            v.store(sizeIndex, Type.INT_TYPE);

            v.iconst(0);
            v.store(indexIndex, Type.INT_TYPE);

            gen(args.get(1).getArgumentExpression(), JetTypeMapper.TYPE_FUNCTION1);

            Label begin = new Label();
            Label end = new Label();
            v.visitLabel(begin);
            v.load(indexIndex, Type.INT_TYPE);
            v.load(sizeIndex,  Type.INT_TYPE);
            v.ificmpge(end);

            v.dup2();
            v.load(indexIndex, Type.INT_TYPE);
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            v.invokevirtual("jet/Function1", "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
            v.load(indexIndex, Type.INT_TYPE);
            v.iinc(indexIndex, 1);
            v.swap();
            v.astore(JetTypeMapper.TYPE_OBJECT);

            v.goTo(begin);
            v.visitLabel(end);
            v.pop();

            myFrameMap.leaveTemp(2);
        }
    }

    @Override
    public StackValue visitArrayAccessExpression(JetArrayAccessExpression expression, StackValue receiver) {
        final JetExpression array = expression.getArrayExpression();
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, array);
        final Type arrayType = type == null ? Type.VOID_TYPE : typeMapper.mapType(type);
        final List<JetExpression> indices = expression.getIndexExpressions();
        FunctionDescriptor operationDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        assert operationDescriptor != null;
        if (arrayType.getSort() == Type.ARRAY && indices.size() == 1 && operationDescriptor.getValueParameters().get(0).getOutType().equals(state.getStandardLibrary().getIntType())) {
            gen(array, arrayType);
            for (JetExpression index : indices) {
                gen(index, Type.INT_TYPE);
            }
            assert type != null;
            if(state.getStandardLibrary().getArray().equals(type.getConstructor().getDeclarationDescriptor())) {
                JetType elementType = type.getArguments().get(0).getType();
                Type notBoxed = typeMapper.mapType(elementType);
                return StackValue.arrayElement(notBoxed, true);
            }
            else {
                return StackValue.arrayElement(JetTypeMapper.correctElementType(arrayType), false);
            }
        }
        else {
            CallableMethod accessor = typeMapper.mapToCallableMethod(operationDescriptor, false, OwnerKind.IMPLEMENTATION);

            boolean isGetter = accessor.getSignature().getName().equals("get");

            ResolvedCall<FunctionDescriptor> resolvedSetCall = bindingContext.get(BindingContext.INDEXED_LVALUE_SET, expression);
            ResolvedCall<FunctionDescriptor> resolvedGetCall = bindingContext.get(BindingContext.INDEXED_LVALUE_GET, expression);

            FunctionDescriptor setterDescriptor = resolvedSetCall == null ? null : resolvedSetCall.getResultingDescriptor();
            FunctionDescriptor getterDescriptor = resolvedGetCall == null ? null : resolvedGetCall.getResultingDescriptor();

            Type asmType;
            Type[] argumentTypes = accessor.getSignature().getArgumentTypes();
            int index = 0;
            if(isGetter) {
                Callable callable = resolveToCallable(getterDescriptor, false);
                if(callable instanceof CallableMethod)
                    genThisAndReceiverFromResolvedCall((CallableMethod) callable, receiver, resolvedGetCall);
                else
                    gen(array, typeMapper.mapType(((ClassDescriptor)getterDescriptor.getContainingDeclaration()).getDefaultType()));
                
                assert getterDescriptor != null;
                if(getterDescriptor.getReceiverParameter().exists()) {
                    index++;
                }
                for (TypeParameterDescriptor typeParameterDescriptor : getterDescriptor.getTypeParameters()) {
                    if(typeParameterDescriptor.isReified()) {
                        generateTypeInfo(resolvedGetCall.getTypeArguments().get(typeParameterDescriptor));
                        index++;
                    }
                }
                asmType = accessor.getSignature().getReturnType();
            }
            else {
                Callable callable = resolveToCallable(resolvedSetCall.getResultingDescriptor(), false);
                if(callable instanceof CallableMethod)
                    genThisAndReceiverFromResolvedCall((CallableMethod) callable, receiver, resolvedSetCall);
                else
                    gen(array, arrayType);

                assert setterDescriptor != null;
                if(setterDescriptor.getReceiverParameter().exists()) {
                    index++;
                }
                for (TypeParameterDescriptor typeParameterDescriptor : setterDescriptor.getOriginal().getTypeParameters()) {
                    if(typeParameterDescriptor.isReified()) {
                        generateTypeInfo(resolvedSetCall.getTypeArguments().get(typeParameterDescriptor));
                        index++;
                    }
                }
                asmType = argumentTypes[argumentTypes.length-1];
            }

            for (JetExpression jetExpression : expression.getIndexExpressions()) {
                gen(jetExpression, argumentTypes[index]);
                index++;
            }
            return StackValue.collectionElement(asmType, resolvedGetCall, resolvedSetCall, this);
        }
    }

    @Override
    public StackValue visitThrowExpression(JetThrowExpression expression, StackValue receiver) {
        gen(expression.getThrownExpression(), JetTypeMapper.TYPE_OBJECT);
        doFinallyOnReturnOrThrow();
        v.athrow();
        return StackValue.none();
    }

    @Override
    public StackValue visitThisExpression(JetThisExpression expression, StackValue receiver) {
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
        if (descriptor instanceof ClassDescriptor) {
            return generateThisOrOuter((ClassDescriptor) descriptor);
        }
        else {
            if(descriptor instanceof FunctionDescriptor || descriptor instanceof PropertyDescriptor) {
                return generateReceiver(descriptor);
            }
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public StackValue visitTryExpression(JetTryExpression expression, StackValue receiver) {
        /*
The "returned" value of try expression with no finally is either the last expression in the try block or the last expression in the catch block
(or blocks).

If finally block is present, its last expression is the value of try expression.
         */
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if(finallyBlock != null) {
            stackOfFinallyBlocks.add(expression);
        }

        JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
        Type expectedAsmType = typeMapper.mapType(jetType);
        
        Label tryStart = new Label();
        v.mark(tryStart);
        v.nop(); // prevent verify error on empty try
        if(finallyBlock == null)
            gen(expression.getTryBlock(), expectedAsmType);
        else
            gen(expression.getTryBlock(), Type.VOID_TYPE);
        Label tryEnd = new Label();
        v.mark(tryEnd);
        if (finallyBlock != null) {
            gen(finallyBlock.getFinalExpression(), expectedAsmType);
        }
        Label end = new Label();
        v.goTo(end);         // TODO don't generate goto if there's no code following try/catch
        for (JetCatchClause clause : expression.getCatchClauses()) {
            Label clauseStart = new Label();
            v.mark(clauseStart);

            VariableDescriptor descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, clause.getCatchParameter());
            assert descriptor != null;
            Type descriptorType = typeMapper.mapType(descriptor.getOutType());
            myFrameMap.enter(descriptor, 1);
            int index = lookupLocal(descriptor);
            v.store(index, descriptorType);

            gen(clause.getCatchBody(), finallyBlock != null ? Type.VOID_TYPE : expectedAsmType);

            myFrameMap.leave(descriptor);

            if (finallyBlock != null) {
                gen(finallyBlock.getFinalExpression(), expectedAsmType);
            }

            v.goTo(end);     // TODO don't generate goto if there's no code following try/catch

            v.visitTryCatchBlock(tryStart, tryEnd, clauseStart, descriptorType.getInternalName());
        }
        if (finallyBlock != null) {
            Label finallyStart = new Label();
            v.mark(finallyStart);

            gen(finallyBlock.getFinalExpression(), Type.VOID_TYPE);

            v.athrow();

            v.visitTryCatchBlock(tryStart, tryEnd, finallyStart, null);
        }
        v.mark(end);
        v.nop();

        if(finallyBlock != null) {
            stackOfFinallyBlocks.remove(stackOfFinallyBlocks.size()-1);
        }

        return StackValue.onStack(expectedAsmType);
    }

    @Override
    public StackValue visitBinaryWithTypeRHSExpression(final JetBinaryExpressionWithTypeRHS expression, StackValue receiver) {
        JetSimpleNameExpression operationSign = expression.getOperationSign();
        IElementType opToken = operationSign.getReferencedNameElementType();
        if (opToken == JetTokens.COLON) {
            return gen(expression.getLeft());
        }
        else {
            JetTypeReference typeReference = expression.getRight();
            JetType jetType = bindingContext.get(BindingContext.TYPE, typeReference);
            assert jetType != null;
            DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor || descriptor instanceof TypeParameterDescriptor) {
                Type type = JetTypeMapper.boxType(typeMapper.mapType(jetType));
                generateInstanceOf(StackValue.expression(OBJECT_TYPE, expression.getLeft(), this), jetType, true);
                Label isInstance = new Label();
                v.ifne(isInstance);
                v.pop();
                if (opToken == JetTokens.AS_SAFE) {
                    v.aconst(null);
                }
                else {
                    throwNewException(CLASS_TYPE_CAST_EXCEPTION);
                }
                v.mark(isInstance);
                v.checkcast(type);
                return StackValue.onStack(type);
            }
            else {
                throw new UnsupportedOperationException("don't know how to handle non-class types in as/as?");
            }
        }
    }

    @Override
    public StackValue visitIsExpression(final JetIsExpression expression, StackValue receiver) {
        final StackValue match = StackValue.expression(OBJECT_TYPE, expression.getLeftHandSide(), this);
        return generatePatternMatch(expression.getPattern(), expression.isNegated(), match, null);
    }

    // on entering the function, expressionToMatch is already placed on stack, and we should consume it
    private StackValue generatePatternMatch(JetPattern pattern, boolean negated, StackValue expressionToMatch,
                                            @Nullable Label nextEntry) {
        if (pattern instanceof JetTypePattern) {
            JetTypeReference typeReference = ((JetTypePattern) pattern).getTypeReference();
            JetType jetType = bindingContext.get(BindingContext.TYPE, typeReference);
            expressionToMatch.dupReceiver(v);
            generateInstanceOf(expressionToMatch, jetType, false);
            StackValue value = StackValue.onStack(Type.BOOLEAN_TYPE);
            return negated ? StackValue.not(value) : value;
        }
        else if (pattern instanceof JetTuplePattern) {
            return generateTuplePatternMatch((JetTuplePattern) pattern, negated, expressionToMatch, nextEntry);
        }
        else if (pattern instanceof JetExpressionPattern) {
            final Type subjectType = expressionToMatch.type;
            expressionToMatch.dupReceiver(v);
            expressionToMatch.put(subjectType, v);
            JetExpression condExpression = ((JetExpressionPattern) pattern).getExpression();
            Type condType = isNumberPrimitive(subjectType) ? expressionType(condExpression) : OBJECT_TYPE;
            gen(condExpression, condType);
            return generateEqualsForExpressionsOnStack(JetTokens.EQEQ, subjectType, condType, false, false);
        }
        else if (pattern instanceof JetWildcardPattern) {
            return StackValue.constant(!negated, Type.BOOLEAN_TYPE);
        }
        else if (pattern instanceof JetBindingPattern) {
            final JetProperty var = ((JetBindingPattern) pattern).getVariableDeclaration();
            final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
            assert variableDescriptor != null;
            final Type varType = typeMapper.mapType(variableDescriptor.getOutType());
            myFrameMap.enter(variableDescriptor, varType.getSize());
            expressionToMatch.dupReceiver(v);
            expressionToMatch.put(varType, v);
            final int varIndex = myFrameMap.getIndex(variableDescriptor);
            v.store(varIndex, varType);
            return generateWhenCondition(varType, varIndex, ((JetBindingPattern) pattern).getCondition(), null);
        }
        else {
            throw new UnsupportedOperationException("Unsupported pattern type: " + pattern);
        }
    }

    private StackValue generateTuplePatternMatch(JetTuplePattern pattern, boolean negated, StackValue expressionToMatch,
                                                 @Nullable Label nextEntry) {
        final List<JetTuplePatternEntry> entries = pattern.getEntries();

        Label lblFail = new Label();
        Label lblDone = new Label();
        expressionToMatch.dupReceiver(v);
        expressionToMatch.put(OBJECT_TYPE, v);
        v.dup();
        final String tupleClassName = "jet/Tuple" + entries.size();
        Type tupleType = Type.getObjectType(tupleClassName);
        v.instanceOf(tupleType);
        Label lblCheck = new Label();
        v.ifne(lblCheck);
        Label lblPopAndFail = new Label();
        v.mark(lblPopAndFail);
        v.pop();
        v.goTo(lblFail);

        v.mark(lblCheck);
        for (int i = 0; i < entries.size(); i++) {
            final StackValue tupleField = StackValue.field(OBJECT_TYPE, tupleClassName, "_" + (i + 1), false);
            final StackValue stackValue = generatePatternMatch(entries.get(i).getPattern(), false, tupleField, nextEntry);
            stackValue.condJump(lblPopAndFail, true, v);
        }

        v.pop();  // delete extra copy of expressionToMatch
        if (negated && nextEntry != null) {
            v.goTo(nextEntry);
        }
        else {
            v.iconst(!negated?1:0);
        }
        v.goTo(lblDone);
        v.mark(lblFail);
        if (!negated && nextEntry != null) {
            v.goTo(nextEntry);
        }
        else {
            v.iconst(negated?1:0);
        }
        v.mark(lblDone);
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private void generateInstanceOf(StackValue expressionToGen, JetType jetType, boolean leaveExpressionOnStack) {
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        boolean javaClass = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor) instanceof PsiClass;
        if (!javaClass && (jetType.getArguments().size() > 0 || !(descriptor instanceof ClassDescriptor))) {
            generateTypeInfo(jetType);
            expressionToGen.put(OBJECT_TYPE, v);
            if (leaveExpressionOnStack) {
                v.dupX1();
            }
            v.invokevirtual("jet/typeinfo/TypeInfo", "isInstance", "(Ljava/lang/Object;)Z");
        }
        else {
            expressionToGen.put(OBJECT_TYPE, v);
            if (leaveExpressionOnStack) {
                v.dup();
            }
            Type type = JetTypeMapper.boxType(typeMapper.mapType(jetType));
            if(jetType.isNullable()) {
                Label nope = new Label();
                Label end = new Label();

                v.dup();
                v.ifnull(nope);
                v.instanceOf(type);
                v.goTo(end);
                v.mark(nope);
                v.pop();
                v.iconst(1);
                v.mark(end);
            }
            else {
                v.instanceOf(type);
            }
        }
    }

    public void generateTypeInfo(JetType jetType) {
        String knownTypeInfo = typeMapper.isKnownTypeInfo(jetType);
        if(knownTypeInfo != null) {
            v.getstatic("jet/typeinfo/TypeInfo", knownTypeInfo, "Ljet/typeinfo/TypeInfo;");
            return;
        }

        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            loadTypeParameterTypeInfo((TypeParameterDescriptor) declarationDescriptor);
            return;
        }

        if(!CodegenUtil.hasTypeInfoField(jetType) && !(bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, jetType.getConstructor().getDeclarationDescriptor()) instanceof PsiClass)) {
            // TODO: we need some better checks here
            v.getstatic(typeMapper.mapType(jetType, OwnerKind.IMPLEMENTATION).getInternalName(), "$staticTypeInfo", "Ljet/typeinfo/TypeInfo;");
            return;
        }
        
        boolean hasUnsubstituted = TypeUtils.hasUnsubstitutedTypeParameters(jetType);
        if(!hasUnsubstituted) {
            int typeInfoConstantIndex = context.getTypeInfoConstantIndex(jetType);
            v.invokestatic(context.getNamespaceClassName(), "$getCachedTypeInfo$" + typeInfoConstantIndex, "()Ljet/typeinfo/TypeInfo;");
            return;
        }

        final Type jvmType = typeMapper.mapType(jetType);

        v.aconst(jvmType);
        v.iconst(jetType.isNullable()?1:0);
        List<TypeProjection> arguments = jetType.getArguments();
        if (arguments.size() > 0) {
            v.iconst(arguments.size());
            v.newarray(JetTypeMapper.TYPE_TYPEINFOPROJECTION);

            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                TypeProjection argument = arguments.get(i);
                v.dup();
                v.iconst(i);
                generateTypeInfo(argument.getType());
                genTypeInfoToProjection(v, argument.getProjectionKind());
                v.astore(JetTypeMapper.TYPE_OBJECT);
            }
            v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z[Ljet/typeinfo/TypeInfoProjection;)Ljet/typeinfo/TypeInfo;");
        }
        else {
            v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z)Ljet/typeinfo/TypeInfo;");
        }
    }

    public static void genTypeInfoToProjection(InstructionAdapter v, Variance variance) {
        if(variance == Variance.INVARIANT)
            v.checkcast(JetTypeMapper.TYPE_TYPEINFOPROJECTION);
        else if(variance == Variance.IN_VARIANCE)
            v.invokestatic("jet/typeinfo/TypeInfo", "inProjection", "(Ljet/typeinfo/TypeInfo;)Ljet/typeinfo/TypeInfoProjection;");
        else if(variance == Variance.OUT_VARIANCE)
            v.invokestatic("jet/typeinfo/TypeInfo", "outProjection", "(Ljet/typeinfo/TypeInfo;)Ljet/typeinfo/TypeInfoProjection;");
        else
            throw new UnsupportedOperationException(variance.toString());
    }

    private void loadTypeParameterTypeInfo(TypeParameterDescriptor typeParameterDescriptor) {
        final StackValue value = typeParameterExpressions.get(typeParameterDescriptor);
        if (value != null) {
            value.put(JetTypeMapper.TYPE_TYPEINFO, v);
            return;
        }
        DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
        if (context.getThisDescriptor() != null) {
            ClassDescriptor descriptor = context.getThisDescriptor();
            assert containingDeclaration != null;
            JetType defaultType = ((ClassDescriptor)containingDeclaration).getDefaultType();
            Type ownerType = typeMapper.mapType(defaultType);
            ownerType = JetTypeMapper.boxType(ownerType);
            if (containingDeclaration == context.getThisDescriptor()) {
                if(!CodegenUtil.isInterface(descriptor)) {
                    if (CodegenUtil.hasTypeInfoField(defaultType)) {
                        v.load(0, JetTypeMapper.TYPE_OBJECT);
                        v.getfield(ownerType.getInternalName(), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
                    }
                    else {
                        v.getstatic(ownerType.getInternalName(), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
                    }
                }
                else {
                    v.load(0, JetTypeMapper.TYPE_OBJECT);
                    v.invokeinterface("jet/JetObject", "getTypeInfo", "()Ljet/typeinfo/TypeInfo;");
                }
            }
            else {
                v.load(0, JetTypeMapper.TYPE_OBJECT);
                v.invokeinterface("jet/JetObject", "getTypeInfo", "()Ljet/typeinfo/TypeInfo;");
                while(descriptor != containingDeclaration) {
                    descriptor = CodegenUtil.getOuterClassDescriptor(descriptor);
                    v.invokevirtual("jet/typeinfo/TypeInfo", "getOuterTypeInfo", "()Ljet/typeinfo/TypeInfo;");
                }
            }
            v.aconst(ownerType);
            v.iconst(typeParameterDescriptor.getIndex());
            v.invokevirtual("jet/typeinfo/TypeInfo", "getArgumentType", "(Ljava/lang/Class;I)Ljet/typeinfo/TypeInfo;");
            return;
        }
        throw new UnsupportedOperationException("don't know what this type parameter resolves to");
    }

    @Override
    public StackValue visitWhenExpression(JetWhenExpression expression, StackValue receiver) {
        JetExpression expr = expression.getSubjectExpression();
        final Type subjectType = expressionType(expr);
        final int subjectLocal = myFrameMap.enterTemp(subjectType.getSize());
        gen(expr, subjectType);
        v.store(subjectLocal, subjectType);

        Label end = new Label();
        Label nextCondition = null;
        boolean hasElse = false;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            if (nextCondition != null) {
                v.mark(nextCondition);
            }
            nextCondition = new Label();
            FrameMap.Mark mark = myFrameMap.mark();
            Label thisEntry = new Label();
            if (!whenEntry.isElse()) {
                final JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    StackValue conditionValue = generateWhenCondition(subjectType, subjectLocal, conditions[i], nextCondition);
                    conditionValue.condJump(nextCondition, true, v);
                    if (i < conditions.length - 1) {
                        v.goTo(thisEntry);
                        v.mark(nextCondition);
                        nextCondition = new Label();
                    }
                }
            }
            else {
                hasElse = true;
            }
            v.visitLabel(thisEntry);
            genToJVMStack(whenEntry.getExpression());
            mark.dropTo();
            v.goTo(end);
        }
        if (!hasElse && nextCondition != null) {
            v.mark(nextCondition);
            throwNewException(CLASS_NO_PATTERN_MATCHED_EXCEPTION);
        }
        v.mark(end);

        myFrameMap.leaveTemp(subjectType.getSize());
        return StackValue.onStack(expressionType(expression));
    }

    private StackValue generateWhenCondition(Type subjectType, int subjectLocal, JetWhenCondition condition, @Nullable Label nextEntry) {
        StackValue conditionValue;
        if (condition instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange conditionInRange = (JetWhenConditionInRange) condition;
            JetExpression rangeExpression = conditionInRange.getRangeExpression();
            if(isIntRangeExpr(rangeExpression)) {
                getInIntRange(new StackValue.Local(subjectLocal, subjectType), (JetBinaryExpression) rangeExpression, conditionInRange.getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IN);
            }
            else {
                FunctionDescriptor op = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, conditionInRange.getOperationReference());
                genToJVMStack(rangeExpression);
                new StackValue.Local(subjectLocal, subjectType).put(JetTypeMapper.TYPE_OBJECT, v);
                invokeFunctionNoParams(op, Type.BOOLEAN_TYPE, v);
            }
            return StackValue.onStack(Type.BOOLEAN_TYPE);
        }
        else if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
            JetPattern pattern = patternCondition.getPattern();
            conditionValue = generatePatternMatch(pattern, patternCondition.isNegated(),
                                                  StackValue.local(subjectLocal, subjectType), nextEntry);
        }
        else if (condition instanceof JetWhenConditionCall) {
            final JetExpression call = ((JetWhenConditionCall) condition).getCallSuffixExpression();
            if (call instanceof JetCallExpression) {
                v.load(subjectLocal, subjectType);
                final DeclarationDescriptor declarationDescriptor = resolveCalleeDescriptor((JetCallExpression) call);
                if (!(declarationDescriptor instanceof FunctionDescriptor)) {
                    throw new UnsupportedOperationException("expected function descriptor in when condition with call, found " + declarationDescriptor);
                }
                conditionValue = invokeFunction((JetCallExpression) call, declarationDescriptor, StackValue.onStack(subjectType));
            }
            else if (call instanceof JetSimpleNameExpression) {
                final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call);
                if (descriptor instanceof PropertyDescriptor) {
                    v.load(subjectLocal, subjectType);
                    conditionValue = intermediateValueForProperty((PropertyDescriptor) descriptor, false, null);
                }
                else {
                    throw new UnsupportedOperationException("unknown simple name resolve result: " + descriptor);
                }
            }
            else {
                throw new UnsupportedOperationException("unsupported kind of call suffix");
            }
        }
        else {
            throw new UnsupportedOperationException("unsupported kind of when condition");
        }
        return conditionValue;
    }

    private boolean isIntRangeExpr(JetExpression rangeExpression) {
        if(rangeExpression instanceof JetBinaryExpression) {
            JetBinaryExpression binaryExpression = (JetBinaryExpression) rangeExpression;
            if (binaryExpression.getOperationReference().getReferencedNameElementType() == JetTokens.RANGE) {
                JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, rangeExpression);
                assert jetType != null;
                final DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
                if (isClass(descriptor, "IntRange")) {
                    return true;
                }
            }
        }
        return false ;
    }

    @Override
    public StackValue visitTupleExpression(JetTupleExpression expression, StackValue receiver) {
        final List<JetExpression> entries = expression.getEntries();
        if (entries.size() > 22) {
            throw new UnsupportedOperationException("tuple too large");
        }
        if(entries.size() == 0) {
            v.visitFieldInsn(Opcodes.GETSTATIC, "jet/Tuple0", "INSTANCE", "Ljet/Tuple0;");
            return StackValue.onStack(Type.getObjectType("jet/Tuple0"));
        }

        final String className = "jet/Tuple" + entries.size();
        Type tupleType = Type.getObjectType(className);
        StringBuilder signature = new StringBuilder("(Ljet/typeinfo/TypeInfo;");
        for (int i = 0; i != entries.size(); ++i) {
            signature.append("Ljava/lang/Object;");
        }
        signature.append(")V");

        v.anew(tupleType);
        v.dup();
        generateTypeInfo(new ProjectionErasingJetType(bindingContext.get(BindingContext.EXPRESSION_TYPE, expression)));
        for (JetExpression entry : entries) {
            gen(entry, OBJECT_TYPE);
        }
        v.invokespecial(className, "<init>", signature.toString());
        return StackValue.onStack(tupleType);
    }

    private void throwNewException(final String className) {
        v.anew(Type.getObjectType(className));
        v.dup();
        v.invokespecial(className, "<init>", "()V");
        v.athrow();
    }

    private static class CompilationException extends RuntimeException {
        private CompilationException() {
        }

        private CompilationException(String message) {
            super(message);
        }
    }

    @Override
    public String toString() {
        return context.getContextDescriptor().toString();
    }
}
