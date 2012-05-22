/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.*;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.jet.codegen.JetTypeMapper.*;

/**
 * @author max
 * @author yole
 * @author alex.tkachman
 */
public class ExpressionCodegen extends JetVisitor<StackValue, StackValue> {

    private static final String CLASS_NO_PATTERN_MATCHED_EXCEPTION = "jet/NoPatternMatchedException";
    private static final String CLASS_TYPE_CAST_EXCEPTION = "jet/TypeCastException";

    private int myLastLineNumber = -1;

    final InstructionAdapter v;
    final FrameMap myFrameMap;
    final JetTypeMapper typeMapper;

    private final GenerationState state;
    private final Type returnType;

    private final BindingContext bindingContext;
    private final Map<TypeParameterDescriptor, StackValue> typeParameterExpressions = new HashMap<TypeParameterDescriptor, StackValue>();
    private final CodegenContext context;

    private final Stack<BlockStackElement> blockStackElements = new Stack<BlockStackElement>();

    static class BlockStackElement {
    }
    
    static class LoopBlockStackElement extends BlockStackElement {
        final Label continueLabel;
        final Label breakLabel;
        public JetSimpleNameExpression targetLabel;

        LoopBlockStackElement(Label breakLabel, Label continueLabel, JetSimpleNameExpression targetLabel) {
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
            this.targetLabel = targetLabel;
        }
    }

    static class FinallyBlockStackElement extends BlockStackElement {
        final JetTryExpression expression;

        FinallyBlockStackElement(JetTryExpression expression) {
            this.expression = expression;
        }
    }


    public ExpressionCodegen(MethodVisitor v,
                             FrameMap myMap,
                             Type returnType,
                             CodegenContext context,
                             GenerationState state) {
        this.myFrameMap = myMap;
        this.typeMapper = state.getInjector().getJetTypeMapper();
        this.returnType = returnType;
        this.state = state;
        this.v = new InstructionAdapter(v);
        this.bindingContext = state.getBindingContext();
        this.context = context;
    }

    StackValue castToRequiredTypeOfInterfaceIfNeeded(StackValue inner, DeclarationDescriptor provided, @Nullable ClassDescriptor required) {
        if(required == null)
            return inner;

        if(provided instanceof CallableDescriptor)
            provided = ((CallableDescriptor)provided).getReceiverParameter().getType().getConstructor().getDeclarationDescriptor();

        assert provided instanceof ClassDescriptor;

        if(!CodegenUtil.isInterface(provided) && CodegenUtil.isInterface(required)) {
            v.checkcast(asmType(required.getDefaultType()));
        }

        return inner;
    }

    public GenerationState getState() {
        return state;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public void addTypeParameter(TypeParameterDescriptor typeParameter, StackValue expression) {
        typeParameterExpressions.put(typeParameter, expression);
    }

    public StackValue genQualified(StackValue receiver, JetElement selector) {
        markLineNumber(selector);
        try {
            return selector.accept(this, receiver);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (Throwable error) {
            String message = error.getMessage();
            throw new CompilationException(message != null ? message : "null", error, selector);
        }
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
    public StackValue visitSuperExpression(JetSuperExpression expression, StackValue data) {
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
        if (descriptor instanceof ClassDescriptor) {
            return generateThisOrOuter((ClassDescriptor) descriptor);
        }
        else {
            JetType type = context.getThisDescriptor().getDefaultType();
            return StackValue.local(0, asmType(type));
        }
    }

    private Type asmType(JetType type) {
        return typeMapper.mapType(type, MapTypeMode.VALUE);
    }

    @Override
    public StackValue visitParenthesizedExpression(JetParenthesizedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getExpression());
    }

    @Override
    public StackValue visitAnnotatedExpression(JetAnnotatedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getBaseExpression());
    }

    private static boolean isEmptyExpression(JetElement expr) {
        if(expr == null)
            return true;
        if(expr instanceof JetBlockExpression) {
            JetBlockExpression blockExpression = (JetBlockExpression) expr;
            List<JetElement> statements = blockExpression.getStatements();
            if(statements.size() == 0 || statements.size() == 1 && isEmptyExpression(statements.get(0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StackValue visitIfExpression(JetIfExpression expression, StackValue receiver) {
        Type asmType = expressionType(expression);
        StackValue condition = gen(expression.getCondition());

        JetExpression thenExpression = expression.getThen();
        JetExpression elseExpression = expression.getElse();

        if (thenExpression == null && elseExpression == null) {
            throw new CompilationException("Both brunches of if/else are null", null, expression);
        }

        if (isEmptyExpression(thenExpression)) {
            if (isEmptyExpression(elseExpression)) {
                condition.put(asmType, v);
                return StackValue.onStack(asmType);
            }
            return generateSingleBranchIf(condition, elseExpression, false);
        }
        else {
            if (isEmptyExpression(elseExpression)) {
                return generateSingleBranchIf(condition, thenExpression, true);
            }
        }


        Label elseLabel = new Label();
        condition.condJump(elseLabel, true, v);   // == 0, i.e. false

        Label end = continueLabel == null ? new Label() : continueLabel;
        
        if(continueLabel != null)
            asmType = Type.VOID_TYPE;

        gen(thenExpression, asmType);

        v.goTo(end);
        v.mark(elseLabel);

        gen(elseExpression, asmType);

        if(end != continueLabel)
            v.mark(end);
        else
            v.goTo(end);

        return StackValue.onStack(asmType);
    }

    @Override
    public StackValue visitWhileExpression(JetWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        v.mark(condition);

        Label end = continueLabel != null ? continueLabel : new Label();
        blockStackElements.push(new LoopBlockStackElement(end, condition, targetLabel(expression)));

        Label savedContinueLabel = continueLabel;
        continueLabel = condition;

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(end, true, v);

        gen(expression.getBody(), Type.VOID_TYPE);
        v.goTo(condition);

        continueLabel = savedContinueLabel;
        if(end != continueLabel)
            v.mark(end);

        blockStackElements.pop();

        return StackValue.onStack(Type.VOID_TYPE);
    }

    
    @Override
    public StackValue visitDoWhileExpression(JetDoWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        v.mark(condition);

        Label end = new Label();
        
        blockStackElements.push(new LoopBlockStackElement(end, condition, targetLabel(expression)));

        gen(expression.getBody(), Type.VOID_TYPE);

        final StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(condition, false, v);

        v.mark(end);

        blockStackElements.pop();
        return StackValue.onStack(Type.VOID_TYPE);
    }

    @Override
    public StackValue visitForExpression(JetForExpression expression, StackValue receiver) {
        final JetExpression loopRange = expression.getLoopRange();
        final JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, loopRange);
        Type loopRangeType = asmType(expressionType);
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
        Type asmIterType = boxType(asmType(iteratorType));

        JetType paramType = parameterDescriptor.getType();
        Type asmParamType = asmType(paramType);

        int iteratorVar = myFrameMap.enterTemp();
        gen(expression.getLoopRange(), boxType(loopRangeType));
        invokeFunctionNoParams(iteratorDescriptor, asmIterType, v);
        v.store(iteratorVar, asmIterType);
        
        Label end = new Label();
        if(iteratorType.isNullable()) {
            v.load(iteratorVar, TYPE_OBJECT);
            v.ifnull(end);
        }

        Label begin = new Label();
        blockStackElements.push(new LoopBlockStackElement(end, begin, targetLabel(expression)));

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

        blockStackElements.pop();
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
            JetType paramType = parameterDescriptor.getType();
            Type asmParamType = asmType(paramType);

            myFrameMap.enter(parameterDescriptor, asmParamType.getSize());
            generatePrologue();

            Label condition = new Label();
            Label increment = new Label();
            v.mark(condition);

            blockStackElements.push(new LoopBlockStackElement(end, increment, targetLabel(expression)));

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

            blockStackElements.pop();
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
                v.store(myArrayVar, TYPE_OBJECT);
            }

            if(expressionType.isNullable()) {
                v.load(myArrayVar, TYPE_OBJECT);
                v.ifnull(end);
            }

            v.iconst(0);
            v.store(myIndexVar, Type.INT_TYPE);
        }

        protected void generateCondition(Type asmParamType, Label end) {
            Type arrayElParamType = state.getInjector().getJetStandardLibrary().getArray().equals(expressionType.getConstructor().getDeclarationDescriptor()) ? boxType(asmParamType): asmParamType;

            v.load(myIndexVar, Type.INT_TYPE);
            v.load(myArrayVar, TYPE_OBJECT);
            v.arraylength();
            v.ificmpge(end);

            v.load(myArrayVar, TYPE_OBJECT);
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
        private int myCountVar;
        private int myDeltaVar;
        private int myIndexVar;

        public ForInRangeLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myIndexVar = lookupLocal(parameterDescriptor);
            myCountVar = myFrameMap.enterTemp();
            myDeltaVar = myFrameMap.enterTemp();
            if(isIntRangeExpr(expression.getLoopRange())) {
                JetBinaryExpression rangeExpression = (JetBinaryExpression) expression.getLoopRange();
                //noinspection ConstantConditions
                gen(rangeExpression.getLeft(), Type.INT_TYPE);
                v.store(myIndexVar, Type.INT_TYPE);
                gen(rangeExpression.getRight(), Type.INT_TYPE);
                v.store(myCountVar, Type.INT_TYPE);

                v.load(myCountVar, Type.INT_TYPE);
                v.load(myIndexVar, Type.INT_TYPE);
                v.sub(Type.INT_TYPE);
                v.iconst(1);
                v.add(Type.INT_TYPE);
                v.store(myCountVar, Type.INT_TYPE);
                
                v.load(myCountVar, Type.INT_TYPE);
                v.iflt(end);

                v.iconst(1);
                v.store(myDeltaVar, Type.INT_TYPE);
            }
            else {
                gen(expression.getLoopRange(), loopRangeType);
                v.dup();
                v.dup();

                v.invokevirtual("jet/IntRange", "getStart", "()I");
                v.store(myIndexVar, Type.INT_TYPE);
                v.invokevirtual("jet/IntRange", "getSize", "()I");
                v.store(myCountVar, Type.INT_TYPE);

                v.invokevirtual("jet/IntRange", "getIsReversed", "()Z");
                Label down = new Label();

                v.ifne(down);
                v.iconst(1);
                Label initEnd = new Label();
                v.goTo(initEnd);
                v.mark(down);
                v.iconst(-1);
                v.mark(initEnd);
                v.store(myDeltaVar, Type.INT_TYPE);
            }
        }

        @Override
        protected void generateCondition(Type asmParamType, Label end) {
            v.load(myCountVar, Type.INT_TYPE);
            v.ifeq(end);
        }

        @Override
        protected void generateIncrement() {
            v.load(myIndexVar, Type.INT_TYPE);
            v.load(myDeltaVar, Type.INT_TYPE);
            v.add(Type.INT_TYPE);
            v.store(myIndexVar, Type.INT_TYPE);
            v.iinc(myCountVar, -1);
        }

        @Override
        protected void cleanupTemp() {
            myFrameMap.leaveTemp();
            myFrameMap.leaveTemp();
        }
    }

    @Override
    public StackValue visitBreakExpression(JetBreakExpression expression, StackValue receiver) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        
        for(int i = blockStackElements.size()-1; i >= 0; --i) {
            BlockStackElement stackElement = blockStackElements.get(i);
            if(stackElement instanceof FinallyBlockStackElement) {
                FinallyBlockStackElement finallyBlockStackElement = (FinallyBlockStackElement) stackElement;
                JetTryExpression jetTryExpression = finallyBlockStackElement.expression;
                //noinspection ConstantConditions
                gen(jetTryExpression.getFinallyBlock().getFinalExpression(), Type.VOID_TYPE);
            }
            else if(stackElement instanceof LoopBlockStackElement) {
                LoopBlockStackElement loopBlockStackElement = (LoopBlockStackElement) stackElement;
                if (labelElement == null || loopBlockStackElement.targetLabel != null && labelElement.getReferencedName().equals(loopBlockStackElement.targetLabel.getReferencedName())) {
                    v.goTo(loopBlockStackElement.breakLabel);
                    return StackValue.none();
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        
        throw new UnsupportedOperationException();
    }

    @Override
    public StackValue visitContinueExpression(JetContinueExpression expression, StackValue receiver) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();

        for(int i = blockStackElements.size()-1; i >= 0; --i) {
            BlockStackElement stackElement = blockStackElements.get(i);
            if(stackElement instanceof FinallyBlockStackElement) {
                FinallyBlockStackElement finallyBlockStackElement = (FinallyBlockStackElement) stackElement;
                JetTryExpression jetTryExpression = finallyBlockStackElement.expression;
                gen(jetTryExpression.getFinallyBlock().getFinalExpression(), Type.VOID_TYPE);
            }
            else if(stackElement instanceof LoopBlockStackElement) {
                LoopBlockStackElement loopBlockStackElement = (LoopBlockStackElement) stackElement;
                if (labelElement == null || loopBlockStackElement.targetLabel != null && labelElement.getReferencedName().equals(loopBlockStackElement.targetLabel.getReferencedName())) {
                    v.goTo(loopBlockStackElement.continueLabel);
                    return StackValue.none();
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        throw new UnsupportedOperationException();
    }

    private StackValue generateSingleBranchIf(StackValue condition, JetExpression expression, boolean inverse) {
        Label end = continueLabel != null ? continueLabel : new Label();

        condition.condJump(end, inverse, v);

        gen(expression, Type.VOID_TYPE);

        if(continueLabel != end)
            v.mark(end);
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
                    invokeAppendMethod(JL_STRING_TYPE);
                }
            }
            v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
            return StackValue.onStack(expressionType(expression));
        }
    }

    @Override
    public StackValue visitBlockExpression(JetBlockExpression expression, StackValue receiver) {
        List<JetElement> statements = expression.getStatements();
        return generateBlock(statements);
    }

    @Override
    public StackValue visitNamedFunction(JetNamedFunction function, StackValue data) {
        assert data == StackValue.none();
        StackValue closure = genClosure(function);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
        int index = myFrameMap.getIndex(descriptor);
        closure.put(TYPE_OBJECT, v);
        v.store(index, TYPE_OBJECT);
        return StackValue.none();
    }

    @Override
    public StackValue visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, StackValue receiver) {
        //noinspection ConstantConditions
        if (bindingContext.get(BindingContext.BLOCK, expression)) {
            //noinspection ConstantConditions
            return generateBlock(expression.getFunctionLiteral().getBodyExpression().getStatements());
        }
        else {
            return genClosure(expression);
        }
    }

    private StackValue genClosure(JetExpression expression) {
        ClosureCodegen closureCodegen = new ClosureCodegen(state, this, context);
        final GeneratedAnonymousClassDescriptor closure = closureCodegen.gen(expression);

        if(closureCodegen.isConst()) {
            v.invokestatic(closure.getClassname().getInternalName(), "$getInstance", "()" + closure.getClassname().getDescriptor());
        }
        else {
            v.anew(closure.getClassname().getAsmType());
            v.dup();

            final Method cons = closure.getConstructor();

            int k = 0;
            if (closure.isCaptureThis()) {
                k++;
                v.load(0, TYPE_OBJECT);
            }

            if (closure.isCaptureReceiver() != null) {
                k++;
                v.load(context.getContextDescriptor().getContainingDeclaration() instanceof NamespaceDescriptor ? 0: 1, closure.isCaptureReceiver());
            }

            for (int i = 0; i < closure.getArgs().size(); i++) {
                StackValue arg = closure.getArgs().get(i);
                arg.put(cons.getArgumentTypes()[i+k], v);
            }

            v.invokespecial(closure.getClassname().getInternalName(), "<init>", cons.getDescriptor());
        }
        return StackValue.onStack(closure.getClassname().getAsmType());
    }

    @Override
    public StackValue visitObjectLiteralExpression(JetObjectLiteralExpression expression, StackValue receiver) {
        ObjectOrClosureCodegen closureCodegen = new ObjectOrClosureCodegen(this, context, state);
        GeneratedAnonymousClassDescriptor closure = state.generateObjectLiteral(expression, closureCodegen);
        Type type = closure.getClassname().getAsmType();
        v.anew(type);
        v.dup();
        final List<Type> consArgTypes = new LinkedList<Type>(Arrays.asList(closure.getConstructor().getArgumentTypes()));

        if (consArgTypes.size() > 0) {
            v.load(0, TYPE_OBJECT);
        }

        if(closureCodegen.captureReceiver != null) {
            v.load(context.isStatic() ? 0 : 1, closureCodegen.captureReceiver);
            consArgTypes.add(closureCodegen.captureReceiver);
        }
        
        for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closureCodegen.closure.entrySet()) {
            if(entry.getKey() instanceof VariableDescriptor && !(entry.getKey() instanceof PropertyDescriptor)) {
                Type sharedVarType = typeMapper.getSharedVarType(entry.getKey());
                if(sharedVarType == null)
                    sharedVarType = state.getInjector().getJetTypeMapper().mapType(((VariableDescriptor) entry.getKey()).getType(), MapTypeMode.VALUE);
                consArgTypes.add(sharedVarType);
                entry.getValue().getOuterValue().put(sharedVarType, v);
            }
        }

        if(closureCodegen.superCall != null) {
            ConstructorDescriptor superConstructor = (ConstructorDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, closureCodegen.superCall.getCalleeExpression().getConstructorReferenceExpression());
            CallableMethod superCallable = typeMapper.mapToCallableMethod(superConstructor, OwnerKind.IMPLEMENTATION, typeMapper.hasThis0(superConstructor.getContainingDeclaration()));
            Type[] argumentTypes = superCallable.getSignature().getAsmMethod().getArgumentTypes();
            Collections.addAll(consArgTypes, argumentTypes);
            ResolvedCall resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, closureCodegen.superCall.getCalleeExpression());
            pushMethodArguments(resolvedCall, Arrays.asList(argumentTypes));
        }
        
        Method cons = new Method("<init>", Type.VOID_TYPE, consArgTypes.toArray(new Type[consArgTypes.size()]));
        v.invokespecial(closure.getClassname().getInternalName(), "<init>", cons.getDescriptor());
        return StackValue.onStack(closure.getClassname().getAsmType());
    }

    private Label continueLabel;
    
    private StackValue generateBlock(List<JetElement> statements) {
        Label blockStart = new Label();
        v.mark(blockStart);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, statement);
                assert variableDescriptor != null;

                final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
                final Type type = sharedVarType != null ? sharedVarType : asmType(variableDescriptor.getType());
                myFrameMap.enter(variableDescriptor, type.getSize());
            }
            
            if(statement instanceof JetNamedFunction) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, statement);
                myFrameMap.enter(descriptor, 1);
            }
        }

        StackValue answer = StackValue.none();
        Label savedContinueLabel = continueLabel;
        continueLabel = null;
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            JetElement statement = statements.get(i);
            if (i == statements.size() - 1 /*&& statement instanceof JetExpression && !bindingContext.get(BindingContext.STATEMENT, statement)*/) {
                continueLabel = savedContinueLabel;
                answer = gen(statement);
            }
            else {
                gen(statement, Type.VOID_TYPE);
            }
        }

        Label blockEnd = new Label();
        v.mark(blockEnd);

        for (JetElement statement : statements) {
            if(statement instanceof JetNamedFunction) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, statement);
                myFrameMap.leave(descriptor);
            }

            if (statement instanceof JetProperty) {
                JetProperty var = (JetProperty) statement;
                VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
                assert variableDescriptor != null;

                int index = myFrameMap.leave(variableDescriptor);

                final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
                final Type type = sharedVarType != null ? sharedVarType : asmType(variableDescriptor.getType());
                if(sharedVarType != null) {
                    v.aconst(null);
                    v.store(index, TYPE_OBJECT);
                }

                v.visitLocalVariable(var.getName(), type.getDescriptor(), null, blockStart, blockEnd, index);
            }
        }

        return answer;
    }

    private void markLineNumber(@NotNull JetElement statement) {
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
        for(int i = blockStackElements.size()-1; i >= 0; --i) {
            BlockStackElement stackElement = blockStackElements.get(i);
            if(stackElement instanceof FinallyBlockStackElement) {
                FinallyBlockStackElement finallyBlockStackElement = (FinallyBlockStackElement) stackElement;
                JetTryExpression jetTryExpression = finallyBlockStackElement.expression;
                blockStackElements.pop();
                gen(jetTryExpression.getFinallyBlock().getFinalExpression(), Type.VOID_TYPE);
                blockStackElements.push(finallyBlockStackElement);
            }
            else {
                break;
            }
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
        else {
            if(resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall call = (VariableAsFunctionResolvedCall) resolvedCall;
                resolvedCall = call.getVariableCall();
            }
            receiver = StackValue.receiver(resolvedCall, receiver, this, null, state);
            descriptor = resolvedCall.getResultingDescriptor();
        }

        //if (descriptor instanceof VariableAsFunctionDescriptor) {
        //    descriptor = ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        //}

        final IntrinsicMethod intrinsic = state.getInjector().getIntrinsics().getIntrinsic(descriptor);
        if (intrinsic != null) {
            final Type expectedType = expressionType(expression);
            return intrinsic.generate(this, v, expectedType, expression, Collections.<JetExpression>emptyList(), receiver, state);
        }

        assert descriptor != null;
        final DeclarationDescriptor container = descriptor.getContainingDeclaration();

        int index = lookupLocal(descriptor);
        if (index >= 0) {
            if(descriptor instanceof VariableDescriptor) {
                Type sharedVarType = typeMapper.getSharedVarType(descriptor);
                final JetType outType = ((VariableDescriptor) descriptor).getType();
                if(sharedVarType != null) {
                    return StackValue.shared(index, asmType(outType));
                }
                else {
                    return StackValue.local(index, asmType(outType));
                }
            }
            else {
                return StackValue.local(index, TYPE_OBJECT);
            }
        }

        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            
            if(propertyDescriptor.isObjectDeclaration()) {
                ClassDescriptor classDescriptor = (ClassDescriptor) propertyDescriptor.getReturnType().getConstructor().getDeclarationDescriptor();
                if(classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
                    ClassDescriptor containing = (ClassDescriptor) classDescriptor.getContainingDeclaration().getContainingDeclaration();
                    Type type = typeMapper.mapType(containing.getDefaultType(), MapTypeMode.VALUE);
                    StackValue.field(type, type.getInternalName(), classDescriptor.getName(), true).put(TYPE_OBJECT, v);

                    // todo: for now we don't generate classes for enum entries, so we need this hack
                    type = typeMapper.mapType(classDescriptor.getDefaultType(), MapTypeMode.VALUE);
                    return StackValue.onStack(type);
                }
                else {
                    Type type = typeMapper.mapType(classDescriptor.getDefaultType(), MapTypeMode.VALUE);
                    return StackValue.field(type, type.getInternalName(), "$instance", true);
                }
            }

            boolean isStatic = container instanceof NamespaceDescriptor;
            final boolean directToField = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER && contextKind() != OwnerKind.TRAIT_IMPL ;
            JetExpression r = getReceiverForSelector(expression);
            final boolean isSuper = r instanceof JetSuperExpression;
            if(propertyDescriptor.getVisibility() == Visibilities.PRIVATE && !DescriptorUtils
                    .isClassObject(propertyDescriptor.getContainingDeclaration())) {
                if(context.getClassOrNamespaceDescriptor() != propertyDescriptor.getContainingDeclaration()) {
                    DeclarationDescriptor enclosed = propertyDescriptor.getContainingDeclaration();
                    if(enclosed != null && enclosed != context.getThisDescriptor()) {
                        CodegenContext c = context;
                        while(c.getContextDescriptor() != enclosed) {
                            c = c.getParentContext();
                        }
                        propertyDescriptor = (PropertyDescriptor) c.getAccessor(propertyDescriptor);
                    }
                }
            }
            final StackValue.Property iValue = intermediateValueForProperty(propertyDescriptor, directToField, isSuper ? (JetSuperExpression)r : null);
            if(!directToField && resolvedCall != null && !isSuper) {
                receiver.put(propertyDescriptor.getReceiverParameter().exists() || isStatic
                             ? receiver.type
                             : Type.getObjectType(iValue.methodOwner), v);
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
                    receiver.put(receiverType != null && !isSuper? asmType(receiverType) : TYPE_OBJECT, v);
                    if(receiverType != null) {
                        ClassDescriptor propReceiverDescriptor = (ClassDescriptor) propertyDescriptor.getContainingDeclaration();
                        if(!CodegenUtil.isInterface(propReceiverDescriptor) && CodegenUtil.isInterface(receiverType.getConstructor().getDeclarationDescriptor())) {
                            // I hope it happens only in case of required super class for traits
                            assert propReceiverDescriptor != null;
                            v.checkcast(asmType(propReceiverDescriptor.getDefaultType()));
                        }
                    }
                }
            }
            return iValue;
        }

        if (descriptor instanceof ClassDescriptor) {
            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
            if(declaration instanceof JetClass) {
                final JetClassObject classObject = ((JetClass) declaration).getClassObject();
                if (classObject == null) {
                    throw new UnsupportedOperationException("trying to reference a class which doesn't have a class object");
                }
                final ClassDescriptor descriptor1 = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
                assert descriptor1 != null;
                final Type type = typeMapper.mapType(descriptor1.getDefaultType(), MapTypeMode.VALUE);
                return StackValue.field(type,
                                        typeMapper.mapType(((ClassDescriptor) descriptor).getDefaultType(), MapTypeMode.IMPL).getInternalName(),
                                              "$classobj",
                                              true);
            }
            else {
                // todo ?
                return StackValue.none();
            }
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            v.invokevirtual("jet/TypeInfo", "getClassObject", "()Ljava/lang/Object;");
            v.checkcast(asmType(typeParameterDescriptor.getClassObjectType()));

            return StackValue.onStack(TYPE_OBJECT);
        }

        StackValue value = context.lookupInContext(descriptor, v, StackValue.local(0, TYPE_OBJECT));
        if (value == null) {
            throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
        }

        if(value instanceof StackValue.Composed) {
            StackValue.Composed composed = (StackValue.Composed) value;
            composed.prefix.put(TYPE_OBJECT, v);
            value = composed.suffix;
        }

        if(value instanceof StackValue.FieldForSharedVar) {
            StackValue.FieldForSharedVar fieldForSharedVar = (StackValue.FieldForSharedVar) value;
            Type sharedType = StackValue.sharedTypeForType(value.type);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldForSharedVar.owner, fieldForSharedVar.name, sharedType.getDescriptor());
        }

        return value;
    }

    public int lookupLocal(DeclarationDescriptor descriptor) {
        return myFrameMap.getIndex(descriptor);
    }

    public void invokeFunctionNoParams(FunctionDescriptor functionDescriptor, Type type, InstructionAdapter v) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getOriginal().getContainingDeclaration();
        boolean isStatic = containingDeclaration instanceof NamespaceDescriptor;
        functionDescriptor = functionDescriptor.getOriginal();
        String owner;

        IntrinsicMethod intrinsic = state.getInjector().getIntrinsics().getIntrinsic(functionDescriptor);
        if(intrinsic != null) {
            intrinsic.generate(this, v, type, null, null, StackValue.onStack(TYPE_OBJECT), state);
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
            isInterface = CodegenUtil.isInterface(containingDeclaration);
        }

        v.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, owner, functionDescriptor.getName(), typeMapper.mapSignature(functionDescriptor.getName(),functionDescriptor).getAsmMethod().getDescriptor());
        StackValue.onStack(asmType(functionDescriptor.getReturnType())).coerce(type, v);
    }

    public StackValue.Property intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, @Nullable JetSuperExpression superExpression) {
        boolean isSuper = superExpression != null;

        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        assert containingDeclaration != null;
        containingDeclaration = containingDeclaration.getOriginal();

        boolean isStatic = containingDeclaration instanceof NamespaceDescriptor;
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
            if (isInsideClass && (propertyDescriptor.getGetter() == null || propertyDescriptor.getGetter().isDefault() && propertyDescriptor.getGetter().getModality() == Modality.FINAL)) {
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
                getter = typeMapper.mapGetterSignature(propertyDescriptor, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();

                if (propertyDescriptor.getGetter() == null) {
                    getter = null;
                }

                if (getter == null && propertyDescriptor.getReceiverParameter().exists()) {
                    throw new IllegalStateException();
                }

            }
            //noinspection ConstantConditions
            if (isInsideClass && (propertyDescriptor.getSetter() == null || propertyDescriptor.getSetter().isDefault() && propertyDescriptor.getSetter().getModality() == Modality.FINAL)) {
                setter = null;
            }
            else {
                JvmPropertyAccessorSignature jvmMethodSignature = typeMapper.mapSetterSignature(propertyDescriptor, OwnerKind.IMPLEMENTATION);
                setter = jvmMethodSignature != null ? jvmMethodSignature.getJvmMethodSignature().getAsmMethod() : null;

                if (propertyDescriptor.getSetter() == null) {
                    setter = null;
                }
                
                if (setter == null && propertyDescriptor.isVar() && propertyDescriptor.getReceiverParameter().exists()) {
                    throw new IllegalStateException();
                }
            }
        }

        int invokeOpcode;

        String owner;
        String ownerParam;
        boolean isInterface;
        if (isInsideClass || isStatic || propertyDescriptor.getGetter() == null) {
            owner = ownerParam = typeMapper.getOwner(propertyDescriptor, contextKind());
            isInterface = false;
            invokeOpcode = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        }
        else {
            isInterface = CodegenUtil.isInterface(containingDeclaration);
            // TODO ugly
            CallableMethod callableMethod = typeMapper.mapToCallableMethod(propertyDescriptor.getGetter(), isSuper, contextKind());
            invokeOpcode = callableMethod.getInvokeOpcode();
            owner = callableMethod.getOwner();
            ownerParam = callableMethod.getDefaultImplParam();
        }

        return StackValue.property(propertyDescriptor.getName(), owner, ownerParam, asmType(propertyDescriptor.getType()), isStatic, isInterface, isSuper, getter, setter, invokeOpcode);
    }

    @Override
    public StackValue visitCallExpression(JetCallExpression expression, StackValue receiver) {
        final JetExpression callee = expression.getCalleeExpression();
        assert callee != null;

        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, callee);
        if(resolvedCall == null) {
            throw new CompilationException("Cannot resolve: " + callee.getText(), null, expression);
        }

        DeclarationDescriptor funDescriptor = resolvedCall.getResultingDescriptor();

        if (funDescriptor instanceof ConstructorDescriptor) {
            receiver = StackValue.receiver(resolvedCall, receiver, this, null, state);
            return generateConstructorCall(expression, (JetSimpleNameExpression) callee, receiver);
        }
        else if (funDescriptor instanceof FunctionDescriptor) {
            final FunctionDescriptor fd = (FunctionDescriptor) funDescriptor;
            if(resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall call = (VariableAsFunctionResolvedCall) resolvedCall;
                ResolvedCallWithTrace<FunctionDescriptor> functionCall = call.getFunctionCall();
                return invokeFunction(expression, functionCall.getResultingDescriptor(), receiver, functionCall);
            }
            else {
                return invokeFunction(expression, fd, receiver, resolvedCall);
            }
        }
        else {
            throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);
        }
    }

    private StackValue invokeFunction(JetCallExpression expression,
            DeclarationDescriptor fd,
            StackValue receiver,
            ResolvedCall<? extends CallableDescriptor> resolvedCall) {
        boolean superCall = false;
        if (expression.getParent() instanceof JetQualifiedExpression) {
            final JetExpression receiverExpression = ((JetQualifiedExpression) expression.getParent()).getReceiverExpression();
            if (receiverExpression instanceof JetSuperExpression) {
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

            final Type callReturnType = callableMethod.getSignature().getAsmMethod().getReturnType();
            return returnValueAsStackValue((FunctionDescriptor) fd, callReturnType);
        }
        else {
            receiver = StackValue.receiver(resolvedCall, receiver, this, null, state);

            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            List<JetExpression> args = new ArrayList<JetExpression>();
            for (ValueArgument argument : expression.getValueArguments()) {
                args.add(argument.getArgumentExpression());
            }
            return intrinsic.generate(this, v, expressionType(expression), expression, args, receiver, state);
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
        final IntrinsicMethod intrinsic = state.getInjector().getIntrinsics().getIntrinsic(fd);
        if (intrinsic != null) {
            return intrinsic;
        }

        CallableMethod callableMethod;
        //if (fd instanceof VariableAsFunctionDescriptor) {
        //    assert !superCall;
        //    callableMethod = ClosureCodegen.asCallableMethod((FunctionDescriptor) fd);
        //}
        if (fd instanceof ExpressionAsFunctionDescriptor || (fd instanceof SimpleFunctionDescriptor && (fd.getContainingDeclaration() instanceof FunctionDescriptor || fd.getContainingDeclaration() instanceof ScriptDescriptor))) {
            SimpleFunctionDescriptor invoke = CodegenUtil.createInvoke((FunctionDescriptor) fd);
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

    public void invokeMethodWithArguments(CallableMethod callableMethod, JetCallElement expression, StackValue receiver) {
        final Type calleeType = callableMethod.getGenerateCalleeType();
        if (calleeType != null && expression instanceof JetCallExpression) {
            assert !callableMethod.isNeedsThis();
            gen(expression.getCalleeExpression(), calleeType);
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
        assert resolvedCall != null;

        if(resolvedCall instanceof VariableAsFunctionResolvedCall) {
            resolvedCall = ((VariableAsFunctionResolvedCall)resolvedCall).getFunctionCall();
        }

        if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor)) { // otherwise already
            receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod, state);
            receiver.put(receiver.type, v);
            if(calleeType != null) {
                StackValue.onStack(receiver.type).put(boxType(receiver.type), v);
            }
        }

        int mask = pushMethodArguments(expression, callableMethod.getValueParameterTypes());
        if(mask == 0)
            callableMethod.invoke(v);
        else
            callableMethod.invokeWithDefault(v, mask);
    }

    private void genThisAndReceiverFromResolvedCall(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall, CallableMethod callableMethod) {
        receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod, state);
        receiver.put(receiver.type, v);
    }

    public void generateFromResolvedCall(ReceiverDescriptor descriptor, Type type) {
        if(descriptor instanceof ClassReceiver) {
            Type exprType = asmType(descriptor.getType());
            ClassReceiver classReceiver = (ClassReceiver) descriptor;
            ClassDescriptor classReceiverDeclarationDescriptor = (ClassDescriptor) classReceiver.getDeclarationDescriptor();
            if(DescriptorUtils.isClassObject(classReceiverDeclarationDescriptor)) {
                ClassDescriptor containingDeclaration = (ClassDescriptor) classReceiverDeclarationDescriptor.getContainingDeclaration();
                Type classObjType = typeMapper.mapType(containingDeclaration.getDefaultType(), MapTypeMode.IMPL);
                v.getstatic(classObjType.getInternalName(), "$classobj", exprType.getDescriptor());
            }
            else {
                generateThisOrOuter(classReceiverDeclarationDescriptor).put(exprType, v);
            }
            if(type != null)
                StackValue.onStack(exprType).put(type, v);
        }
        else if(descriptor instanceof ExtensionReceiver) {
            Type exprType = asmType(descriptor.getType());
            ExtensionReceiver extensionReceiver = (ExtensionReceiver) descriptor;
            generateReceiver(extensionReceiver.getDeclarationDescriptor()).put(exprType, v);
            if(type != null)
                StackValue.onStack(exprType).put(type, v);
        }
        else if(descriptor instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver) descriptor;
            JetExpression expr = expressionReceiver.getExpression();
            Type exprType = asmType(expressionReceiver.getType());
            gen(expr, exprType);
            if(type != null)
                StackValue.onStack(exprType).put(type, v);
        }
        else if(descriptor instanceof AutoCastReceiver) {
            AutoCastReceiver autoCastReceiver = (AutoCastReceiver) descriptor;
            Type intermediateType = asmType(autoCastReceiver.getType());
            generateFromResolvedCall(autoCastReceiver.getOriginal(), intermediateType);
            StackValue.onStack(intermediateType).put(type, v);
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

        StackValue result = context.lookupInContext(provided, v, StackValue.local(0, TYPE_OBJECT));
        return castToRequiredTypeOfInterfaceIfNeeded(result, provided, null);
    }

    public StackValue generateThisOrOuter(ClassDescriptor calleeContainingClass) {
        CodegenContext cur = context;

        PsiElement psiElement = BindingContextUtils.classDescriptorToDeclaration(bindingContext, calleeContainingClass);
        boolean isObject = psiElement instanceof JetClassOrObject && CodegenUtil.isNonLiteralObject((JetClassOrObject) psiElement);
        
        cur = context;
        StackValue result = StackValue.local(0, TYPE_OBJECT);
        while (cur != null) {
            if(cur instanceof CodegenContext.MethodContext && !(cur instanceof CodegenContext.ConstructorContext))
                cur = cur.getParentContext();

            if (DescriptorUtils.isSubclass(cur.getThisDescriptor(), calleeContainingClass)) {
                Type type = asmType(calleeContainingClass.getDefaultType());
                if(!isObject || (cur.getThisDescriptor() == calleeContainingClass)) {
                    result.put(TYPE_OBJECT, v);
                    return castToRequiredTypeOfInterfaceIfNeeded(StackValue.onStack(type), cur.getThisDescriptor(), calleeContainingClass);
                }
                else {
                    v.getstatic(type.getInternalName(), "$instance", type.getDescriptor());
                }
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

    private int pushMethodArguments(@NotNull ResolvedCall resolvedCall, List<Type> valueParameterTypes) {
        @SuppressWarnings("unchecked")
        List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
        CallableDescriptor fd = resolvedCall.getResultingDescriptor();

        if (fd.getValueParameters().size() != valueArguments.size()) {
            throw new IllegalStateException();
        }

        int index = 0;
        int mask  = 0;

        for (ValueParameterDescriptor valueParameterDescriptor : fd.getValueParameters()) {
            ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor.getIndex());
            if(resolvedValueArgument instanceof ExpressionValueArgument) {
                ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;
                gen(valueArgument.getValueArgument().getArgumentExpression(), valueParameterTypes.get(index));
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
                else if(type.getSort() == Type.LONG) {
                    v.aconst(0l);
                }
                else {
                    v.iconst(0);
                }
                mask |= (1 << index);
            }
            else if(resolvedValueArgument instanceof VarargValueArgument) {
                VarargValueArgument valueArgument = (VarargValueArgument) resolvedValueArgument;
                genVarargs(valueParameterDescriptor, valueArgument);
            }
            else {
                throw new UnsupportedOperationException();
            }
            index++;
        }
        return mask;
    }

    public void genVarargs(ValueParameterDescriptor valueParameterDescriptor, VarargValueArgument valueArgument) {
        JetType outType = valueParameterDescriptor.getType();

        Type type = asmType(outType);
        assert type.getSort() == Type.ARRAY;
        Type elementType = correctElementType(type);
        List<ValueArgument> arguments = valueArgument.getArguments();
        int size = arguments.size();

        boolean hasSpread = false;
        for(int i = 0; i != size; ++i) {
            if(arguments.get(i).getSpreadElement() != null) {
                hasSpread = true;
                break;
            }
        }

        if(hasSpread) {
            if(size == 1) {
                gen(arguments.get(0).getArgumentExpression(), type);
            }
            else {
                String owner = "jet/runtime/Intrinsics$SpreadBuilder";
                v.anew(Type.getObjectType(owner));
                v.dup();
                v.invokespecial(owner, "<init>", "()V");
                for(int i = 0; i != size; ++i) {
                    v.dup();
                    ValueArgument argument = arguments.get(i);
                    if(argument.getSpreadElement() != null) {
                        gen(argument.getArgumentExpression(),JetTypeMapper.TYPE_OBJECT);
                        v.invokevirtual(owner, "addSpread", "(Ljava/lang/Object;)V");
                    }
                    else {
                        gen(argument.getArgumentExpression(), elementType);
                        v.invokevirtual(owner, "add", "(Ljava/lang/Object;)Z");
                        v.pop();
                    }
                }
                v.dup();
                v.invokevirtual(owner, "size", "()I");
                v.newarray(elementType);
                v.invokevirtual(owner, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
                v.checkcast(type);
            }
        }
        else {
            v.iconst(arguments.size());
            v.newarray(elementType);
            for(int i = 0; i != size; ++i) {
                v.dup();
                v.iconst(i);
                gen(arguments.get(i).getArgumentExpression(), elementType);
                StackValue.arrayElement(elementType, false).store(v);
            }
        }
    }

    public int pushMethodArguments(JetCallElement expression, List<Type> valueParameterTypes) {
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
        return type == null ? Type.VOID_TYPE : asmType(type);
    }

    public int indexOfLocal(JetReferenceExpression lhs) {
        final DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, lhs);
        return lookupLocal(declarationDescriptor);
    }
    
    @Override
    public StackValue visitDotQualifiedExpression(JetDotQualifiedExpression expression, StackValue receiver) {
        return genQualified(StackValue.none(), expression.getSelectorExpression());
    }

    @Override
    public StackValue visitSafeQualifiedExpression(JetSafeQualifiedExpression expression, StackValue receiver) {
        JetExpression expr = expression.getReceiverExpression();
        JetType receiverJetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getReceiverExpression());
        Type receiverType = asmType(receiverJetType);
        gen(expr, receiverType);
        assert receiverJetType != null;
        if(!receiverJetType.isNullable()) {
            StackValue propValue = genQualified(StackValue.onStack(receiverType), expression.getSelectorExpression());
            Type type = propValue.type;
            propValue.put(type, v);
            if(isPrimitive(type) && !type.equals(Type.VOID_TYPE)) {
                StackValue.valueOf(v, type);
                type = boxType(type);
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
            if(isPrimitive(type) && !type.equals(Type.VOID_TYPE)) {
                StackValue.valueOf(v, type);
                type = boxType(type);
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
                                          Arrays.asList(expression.getLeft(), expression.getRight()), receiver, state);
            }
            else {
                return invokeOperation(expression, (FunctionDescriptor)op, (CallableMethod) callable);
            }
        }
    }

    private StackValue generateIn(JetBinaryExpression expression) {
        JetExpression expr = expression.getLeft();
        if(isIntRangeExpr(expression.getRight())) {
            StackValue leftValue = StackValue.expression(Type.INT_TYPE, expression.getLeft(), this);
            JetBinaryExpression rangeExpression = (JetBinaryExpression) expression.getRight();
            boolean inverted = expression.getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IN;
            getInIntRange(leftValue, rangeExpression, inverted);
        }
        else {
            StackValue leftValue = gen(expr);
            FunctionDescriptor op = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
            assert op != null;
            leftValue.put(asmType(op.getValueParameters().get(0).getType()), v);
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
        Type leftType = asmType(leftJetType);
        JetType rightJetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, right);
        Type rightType = asmType(rightJetType);
        if(leftType == TYPE_NOTHING) {
            return genCmpWithNull(right, rightType, opToken);
        }

        if(rightType == TYPE_NOTHING) {
            return genCmpWithNull(left, leftType, opToken);
        }

        if(isPrimitive(leftType) != isPrimitive(rightType)) {
            gen(left, leftType);
            StackValue.valueOf(v, leftType);
            leftType = boxType(leftType);
            gen(right, rightType);
            StackValue.valueOf(v, rightType);
            rightType = boxType(rightType);
        }
        else {
            gen(left, leftType);
            gen(right, rightType);
        }

        if(isPrimitive(leftType)) // both are primitive
            return generateEqualsForExpressionsOnStack(opToken, leftType, rightType, false, false);

        assert leftJetType != null;
        assert rightJetType != null;
        return generateEqualsForExpressionsOnStack(opToken, leftType, rightType, leftJetType.isNullable(), rightJetType.isNullable());
    }

    private StackValue genCmpWithNull(JetExpression exp, Type expType, IElementType opToken) {
        v.iconst(1);
        gen(exp, boxType(expType));
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
        if ((isNumberPrimitive(leftType) || leftType.getSort() == Type.BOOLEAN) && leftType == rightType) {
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
            v.invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
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
                v.invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
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
                v.invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
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
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getLeft());
        assert type != null;
        final Type leftType = asmType(type);
        if(type.isNullable())  {
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
        }
        else {
            gen(expression.getLeft(), leftType);
            StackValue.onStack(leftType).put(exprType, v);
        }
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
        return isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    private StackValue generateCompareOp(JetExpression left, JetExpression right, IElementType opToken, Type operandType) {
        gen(left, operandType);
        gen(right, operandType);
        return compareExpressionsOnStack(opToken, operandType);
    }

    private StackValue compareExpressionsOnStack(IElementType opToken, Type operandType) {
        if (operandType.getSort() == Type.OBJECT) {
            v.invokeinterface("java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I");
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
                intrinsic.generate(this, v, lhsType, expression, Arrays.asList(expression.getRight()), StackValue.onStack(lhsType), state);
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
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());
        assert resolvedCall != null;

        StackValue value = gen(expression.getLeft());
        if (keepReturnValue) {
            value.dupReceiver(v);
        }
        value.put(lhsType, v);
        StackValue receiver = StackValue.onStack(lhsType);

        if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor)) { // otherwise already
            receiver = StackValue.receiver(resolvedCall, receiver, this, callable, state);
            receiver.put(receiver.type, v);
        }

        pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
        callable.invoke(v);
        if (keepReturnValue) {
            value.store(v);
        }
    }

    public void generateStringBuilderConstructor() {
        Type type = JL_STRING_BUILDER;
        v.anew(type);
        v.dup();
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        v.invokespecial("java/lang/StringBuilder", method.getName(), method.getDescriptor());
    }

    public void invokeAppend(final JetExpression expr) {
        if (expr instanceof JetBinaryExpression) {
            final JetBinaryExpression binaryExpression = (JetBinaryExpression) expr;
            if (binaryExpression.getOperationToken() == JetTokens.PLUS) {
                JetExpression left = binaryExpression.getLeft();
                JetExpression right = binaryExpression.getRight();
                Type leftType = expressionType(left);
                Type rightType = expressionType(right);

                if(leftType.equals(JetTypeMapper.JL_STRING_TYPE) && rightType.equals(JL_STRING_TYPE)) {
                    invokeAppend(left);
                    invokeAppend(right);
                    return;
                }
            }
        }
        Type exprType = expressionType(expr);
        gen(expr, exprType);
        invokeAppendMethod(exprType.getSort() == Type.ARRAY ? TYPE_OBJECT : exprType);
    }

    public void invokeAppendMethod(Type exprType) {
        Method appendDescriptor = new Method("append", JL_STRING_BUILDER,
                new Type[] { exprType.getSort() == Type.OBJECT ? TYPE_OBJECT : exprType});
        v.invokevirtual("java/lang/StringBuilder", "append", appendDescriptor.getDescriptor());
    }

    private JetSimpleNameExpression targetLabel(JetExpression expression) {
        if(expression.getParent() instanceof JetPrefixExpression) {
            JetPrefixExpression parent = (JetPrefixExpression) expression.getParent();
            JetSimpleNameExpression operationSign = parent.getOperationReference();
            if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
                return operationSign;
            }
        }
        return null;
    }
    
    @Override
    public StackValue visitPrefixExpression(JetPrefixExpression expression, StackValue receiver) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
            return genQualified(receiver, expression.getBaseExpression());
        }

        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        final Callable callable = resolveToCallable(op, false);
        if (callable instanceof IntrinsicMethod) {
            IntrinsicMethod intrinsic = (IntrinsicMethod) callable;
            return intrinsic.generate(this, v, expressionType(expression), expression,
                                      Arrays.asList(expression.getBaseExpression()), receiver, state);
        }
        else {
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (isNumberPrimitive(cls) || !(op.getName().equals("inc") || op.getName().equals("dec")) ) {
                return invokeOperation(expression, (FunctionDescriptor) op, (CallableMethod) callable);
            }
            else {
                ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());
                assert resolvedCall != null;

                StackValue value = gen(expression.getBaseExpression());
                value.dupReceiver(v);
                value.dupReceiver(v);

                Type type = expressionType(expression.getBaseExpression());
                value.put(type, v);
                ((CallableMethod)callable).invoke(v);
                value.store(v);
                value.put(type, v);
                return StackValue.onStack(type);
            }
        }
    }

    private StackValue invokeOperation(JetOperationExpression expression, FunctionDescriptor op, CallableMethod callable) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());
        assert resolvedCall != null;
        genThisAndReceiverFromResolvedCall(StackValue.none(), resolvedCall, callable);
        pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
        callable.invoke(v);
        return  returnValueAsStackValue(op, callable.getSignature().getAsmMethod().getReturnType());
    }

    @Override
    public StackValue visitPostfixExpression(JetPostfixExpression expression, StackValue receiver) {
        if(expression.getOperationReference().getReferencedNameElementType() == JetTokens.EXCLEXCL) {
            JetExpression baseExpression = expression.getBaseExpression();
            JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, baseExpression);
            StackValue base = genQualified(receiver, baseExpression);
            if(type != null && type.isNullable()) {
                base.put(base.type, v);
                v.dup();
                Label ok = new Label();
                v.ifnonnull(ok);
                v.invokestatic("jet/runtime/Intrinsics", "throwNpe", "()V");
                v.mark(ok);
                return StackValue.onStack(base.type);
            }
            else 
                return base;
        }
        DeclarationDescriptor op = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        if (op instanceof FunctionDescriptor) {
            final Type asmType = expressionType(expression);
            DeclarationDescriptor cls = op.getContainingDeclaration();
            if (op.getName().equals("inc") || op.getName().equals("dec")) {
                if (isNumberPrimitive(cls)) {
                    receiver.put(receiver.type, v);
                    JetExpression operand = expression.getBaseExpression();
                    if (operand instanceof JetReferenceExpression) {
                        final int index = indexOfLocal((JetReferenceExpression) operand);
                        if (index >= 0 && isIntPrimitive(asmType)) {
                            int increment = op.getName().equals("inc") ? 1 : -1;
                            return StackValue.postIncrement(index, increment);
                        }
                    }
                    gen(operand, asmType);                               // old value
                    generateIncrement(op, asmType, operand, receiver);   // increment in-place
                    return StackValue.onStack(asmType);                                         // old value
                }
                else {
                    ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());
                    assert resolvedCall != null;

                    final Callable callable = resolveToCallable(op, false);

                    StackValue value = gen(expression.getBaseExpression());
                    value.dupReceiver(v);

                    Type type = expressionType(expression.getBaseExpression());
                    value.put(type, v);

                    switch(value.receiverSize()) {
                        case 0:
                            if(type.getSize() == 2)
                                v.dup2();
                            else
                                v.dup();
                        break;

                        case 1:
                            if(type.getSize() == 2)
                                v.dup2X1();
                            else
                                v.dupX1();
                        break;

                        case 2:
                            if(type.getSize() == 2)
                                v.dup2X2();
                            else
                                v.dupX2();
                            break;

                        case -1:
                            throw new UnsupportedOperationException();
                    }

                    CallableMethod callableMethod = (CallableMethod) callable;
                    callableMethod.invoke(v);
                    StackValue.onStack(callableMethod.getSignature().getAsmMethod().getReturnType()).put(value.type, v);
                    value.store(v);
                    return StackValue.onStack(type);
                }
            }
        }
        throw new UnsupportedOperationException("Don't know how to generate this postfix expression");
    }

    private void generateIncrement(DeclarationDescriptor op, Type asmType, JetExpression operand, StackValue receiver) {
        int increment = op.getName().equals("inc") ? 1 : -1;
        if (operand instanceof JetReferenceExpression) {
            final int index = indexOfLocal((JetReferenceExpression) operand);
            if (index >= 0 && isIntPrimitive(asmType)) {
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
        Type varType = asmType(variableDescriptor.getType());
        if(sharedVarType != null) {
            v.anew(sharedVarType);
            v.dup();
            v.invokespecial(sharedVarType.getInternalName(), "<init>", "()V");
            v.store(index, TYPE_OBJECT);
        }

        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            if(sharedVarType == null) {
                gen(initializer, varType);
                v.store(index, varType);
            }
            else {
                v.load(index, TYPE_OBJECT);
                gen(initializer, varType);
                v.putfield(sharedVarType.getInternalName(), "ref", sharedVarType == TYPE_SHARED_VAR ? "Ljava/lang/Object;" : varType.getDescriptor());
            }
        }
        return StackValue.none();
    }

    private StackValue generateConstructorCall(JetCallExpression expression, JetSimpleNameExpression constructorReference, StackValue receiver) {
        DeclarationDescriptor constructorDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, constructorReference);
        final PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, constructorDescriptor);
        Type type;
        if (declaration instanceof PsiMethod) {
            type = generateJavaConstructorCall(expression);
        }
        else if (constructorDescriptor instanceof ConstructorDescriptor) {
            //noinspection ConstantConditions
            JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
            assert expressionType != null;
            type = typeMapper.mapType(expressionType, MapTypeMode.VALUE);
            if (type.getSort() == Type.ARRAY) {
                generateNewArray(expression, expressionType);
            }
            else {
                ClassDescriptor classDecl = (ClassDescriptor) constructorDescriptor.getContainingDeclaration();

                v.anew(type);
                v.dup();

                // TODO typechecker must verify that we're the outer class of the instance being created
                //noinspection ConstantConditions
                if(!receiver.type.equals(Type.VOID_TYPE)) {
                    receiver.put(receiver.type, v);
                }

                CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) constructorDescriptor, OwnerKind.IMPLEMENTATION, typeMapper.hasThis0(((ConstructorDescriptor) constructorDescriptor).getContainingDeclaration()));
                invokeMethodWithArguments(method, expression, StackValue.none());
            }
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate this new expression");
        }
        return StackValue.onStack(type);
    }

    private Type generateJavaConstructorCall(JetCallExpression expression) {
        JetExpression callee = expression.getCalleeExpression();
        ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, callee);
        if(resolvedCall == null) {
            assert callee != null;
            throw new CompilationException("Cannot resolve: " + callee.getText(), null, expression);
        }

        FunctionDescriptor descriptor = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        ClassDescriptor javaClass = (ClassDescriptor) descriptor.getContainingDeclaration();
        Type type = asmType(javaClass.getDefaultType());
        v.anew(type);
        v.dup();
        final CallableMethod callableMethod = typeMapper.mapToCallableMethod(descriptor, false, OwnerKind.IMPLEMENTATION);
        invokeMethodWithArguments(callableMethod, expression, StackValue.none());
        return type;
    }

    public void generateNewArray(JetCallExpression expression, JetType arrayType) {
        List<JetExpression> args = new ArrayList<JetExpression>();
        for(ValueArgument va : expression.getValueArguments())
            args.add(va.getArgumentExpression());
        args.addAll(expression.getFunctionLiteralArguments());

        boolean isArray = state.getInjector().getJetStandardLibrary().getArray().equals(arrayType.getConstructor().getDeclarationDescriptor());
        if(isArray) {
//            if (args.size() != 2 && !arrayType.getArguments().get(0).getType().isNullable()) {
//                throw new CompilationException("array constructor of non-nullable type requires two arguments");
//            }
        }
        else {
            if (args.size() != 1) {
                throw new CompilationException("primitive array constructor requires one argument", null, expression);
            }
        }

        if(isArray) {
            gen(args.get(0), Type.INT_TYPE);
            v.newarray(boxType(asmType(arrayType.getArguments().get(0).getType())));
/*
            JetType elementType = typeMapper.getGenericsElementType(arrayType);
            if(elementType != null) {
                generateTypeInfo(elementType, null);
                gen(args.get(0), Type.INT_TYPE);
                v.invokevirtual("jet/TypeInfo", "newArray", "(I)[Ljava/lang/Object;");
            }
            else {
                gen(args.get(0), Type.INT_TYPE);
                v.newarray(boxType(asmType(arrayType.getArguments().get(0).getType())));
            }
*/
        }
        else {
            Type type = typeMapper.mapType(arrayType, MapTypeMode.VALUE);
            gen(args.get(0), Type.INT_TYPE);
            v.newarray(correctElementType(type));
        }

        if(args.size() == 2) {
            int sizeIndex  = myFrameMap.enterTemp(2);
            int indexIndex = sizeIndex+1;

            v.dup();
            v.arraylength();
            v.store(sizeIndex, Type.INT_TYPE);

            v.iconst(0);
            v.store(indexIndex, Type.INT_TYPE);

            gen(args.get(1), TYPE_FUNCTION1);

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
            v.astore(TYPE_OBJECT);

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
        final Type arrayType = type == null ? Type.VOID_TYPE : asmType(type);
        final List<JetExpression> indices = expression.getIndexExpressions();
        FunctionDescriptor operationDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        assert operationDescriptor != null;
        if (arrayType.getSort() == Type.ARRAY && indices.size() == 1 && operationDescriptor.getValueParameters().get(0).getType().equals(state.getInjector().getJetStandardLibrary().getIntType())) {
            gen(array, arrayType);
            for (JetExpression index : indices) {
                gen(index, Type.INT_TYPE);
            }
            assert type != null;
            if(state.getInjector().getJetStandardLibrary().getArray().equals(type.getConstructor().getDeclarationDescriptor())) {
                JetType elementType = type.getArguments().get(0).getType();
                Type notBoxed = asmType(elementType);
                return StackValue.arrayElement(notBoxed, true);
            }
            else {
                return StackValue.arrayElement(correctElementType(arrayType), false);
            }
        }
        else {
            CallableMethod accessor = typeMapper.mapToCallableMethod(operationDescriptor, false, OwnerKind.IMPLEMENTATION);

            boolean isGetter = accessor.getSignature().getAsmMethod().getName().equals("get");

            ResolvedCall<FunctionDescriptor> resolvedSetCall = bindingContext.get(BindingContext.INDEXED_LVALUE_SET, expression);
            ResolvedCall<FunctionDescriptor> resolvedGetCall = bindingContext.get(BindingContext.INDEXED_LVALUE_GET, expression);

            FunctionDescriptor setterDescriptor = resolvedSetCall == null ? null : resolvedSetCall.getResultingDescriptor();
            FunctionDescriptor getterDescriptor = resolvedGetCall == null ? null : resolvedGetCall.getResultingDescriptor();

            Type asmType;
            Type[] argumentTypes = accessor.getSignature().getAsmMethod().getArgumentTypes();
            int index = 0;
            if(isGetter) {
                Callable callable = resolveToCallable(getterDescriptor, false);
                if(callable instanceof CallableMethod)
                    genThisAndReceiverFromResolvedCall(receiver, resolvedGetCall, (CallableMethod) callable);
                else {
                    assert getterDescriptor != null;
                    gen(array, asmType(((ClassDescriptor)getterDescriptor.getContainingDeclaration()).getDefaultType()));
                }
                
                assert getterDescriptor != null;
                if(getterDescriptor.getReceiverParameter().exists()) {
                    index++;
                }
                asmType = accessor.getSignature().getAsmMethod().getReturnType();
            }
            else {
                assert resolvedSetCall != null;
                Callable callable = resolveToCallable(resolvedSetCall.getResultingDescriptor(), false);
                if(callable instanceof CallableMethod) {
                    genThisAndReceiverFromResolvedCall(receiver, resolvedSetCall, (CallableMethod) callable);
                }
                else
                    gen(array, arrayType);

                if(setterDescriptor.getReceiverParameter().exists()) {
                    index++;
                }
                asmType = argumentTypes[argumentTypes.length-1];
            }

            for (JetExpression jetExpression : expression.getIndexExpressions()) {
                gen(jetExpression, argumentTypes[index]);
                index++;
            }
            return StackValue.collectionElement(asmType, resolvedGetCall, resolvedSetCall, this, state);
        }
    }

    @Override
    public StackValue visitThrowExpression(JetThrowExpression expression, StackValue receiver) {
        gen(expression.getThrownExpression(), TYPE_THROWABLE);
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
        Label savedContinueLabel = continueLabel;
        continueLabel = null;

        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if(finallyBlock != null) {
            blockStackElements.push(new FinallyBlockStackElement(expression));
        }

        JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
        Type expectedAsmType = asmType(jetType);
        
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
            Type descriptorType = asmType(descriptor.getType());
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
            blockStackElements.pop();
        }

        continueLabel = savedContinueLabel;

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
            JetType rightType = bindingContext.get(BindingContext.TYPE, typeReference);
            Type rightTypeAsm = boxType(asmType(rightType));
            assert rightType != null;
            JetExpression left = expression.getLeft();
            JetType leftType = bindingContext.get(BindingContext.EXPRESSION_TYPE, left);
            DeclarationDescriptor descriptor = rightType.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor || descriptor instanceof TypeParameterDescriptor) {
                StackValue value = genQualified(receiver, left);
                value.put(JetTypeMapper.boxType(value.type), v);
                assert leftType != null;

                if (opToken != JetTokens.AS_SAFE) {
                    if(leftType.isNullable()) {
                        if (!rightType.isNullable()) {
                            v.dup();
                            Label nonnull = new Label();
                            v.ifnonnull(nonnull);
                            throwNewException(CLASS_TYPE_CAST_EXCEPTION);
                            v.mark(nonnull);
                        }
                    }
                }
                else {
                    v.dup();
                    v.instanceOf(rightTypeAsm);
                    Label ok = new Label();
                    v.ifne(ok);
                    v.pop();
                    v.aconst(null);
                    v.mark(ok);
                }

                v.checkcast(rightTypeAsm);
                return StackValue.onStack(rightTypeAsm);
            }
            else {
                throw new UnsupportedOperationException("don't know how to handle non-class types in as/as?");
            }
        }
    }

    @Override
    public StackValue visitIsExpression(final JetIsExpression expression, StackValue receiver) {
        final StackValue match = StackValue.expression(TYPE_OBJECT, expression.getLeftHandSide(), this);
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
            if(expressionToMatch != null) {
                final Type subjectType = expressionToMatch.type;
                expressionToMatch.dupReceiver(v);
                expressionToMatch.put(subjectType, v);
                JetExpression condExpression = ((JetExpressionPattern) pattern).getExpression();
                Type condType = isNumberPrimitive(subjectType) ? expressionType(condExpression) : TYPE_OBJECT;
                gen(condExpression, condType);
                return generateEqualsForExpressionsOnStack(JetTokens.EQEQ, subjectType, condType, false, false);
            }
            else {
                JetExpression condExpression = ((JetExpressionPattern) pattern).getExpression();
                return gen(condExpression);
            }
        }
        else if (pattern instanceof JetWildcardPattern) {
            return StackValue.constant(!negated, Type.BOOLEAN_TYPE);
        }
        else if (pattern instanceof JetBindingPattern) {
            final JetProperty var = ((JetBindingPattern) pattern).getVariableDeclaration();
            final VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, var);
            assert variableDescriptor != null;
            final Type varType = asmType(variableDescriptor.getType());
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
        expressionToMatch.put(TYPE_OBJECT, v);
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
            final StackValue tupleField = StackValue.field(TYPE_OBJECT, tupleClassName, "_" + (i + 1), false);
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
        expressionToGen.put(TYPE_OBJECT, v);
        if (leaveExpressionOnStack) {
            v.dup();
        }
        Type type = boxType(asmType(jetType));
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

    @Override
    public StackValue visitWhenExpression(JetWhenExpression expression, StackValue receiver) {
        JetExpression expr = expression.getSubjectExpression();
        final Type subjectType = expressionType(expr);
        final Type resultType = expressionType(expression);
        final int subjectLocal = expr != null ? myFrameMap.enterTemp(subjectType.getSize()) : -1;
        if(subjectLocal != -1) {
            gen(expr, subjectType);
            v.store(subjectLocal, subjectType);
        }

        Label end = new Label();
        boolean hasElse = false;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            if(whenEntry.isElse()) {
                hasElse = true;
                break;
            }
        }

        Label nextCondition = null;
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

            v.visitLabel(thisEntry);
            gen(whenEntry.getExpression(), resultType);
            mark.dropTo();
            if (!whenEntry.isElse()) {
                v.goTo(end);
            }
        }
        if (!hasElse && nextCondition != null) {
            v.mark(nextCondition);
            throwNewException(CLASS_NO_PATTERN_MATCHED_EXCEPTION);
        }
        v.mark(end);

        myFrameMap.leaveTemp(subjectType.getSize());
        return StackValue.onStack(resultType);
    }

    private StackValue generateWhenCondition(Type subjectType, int subjectLocal, JetWhenCondition condition, @Nullable Label nextEntry) {
        if (condition instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange conditionInRange = (JetWhenConditionInRange) condition;
            JetExpression rangeExpression = conditionInRange.getRangeExpression();
            if(isIntRangeExpr(rangeExpression)) {
                getInIntRange(new StackValue.Local(subjectLocal, subjectType), (JetBinaryExpression) rangeExpression, conditionInRange.getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IN);
            }
            else {
                FunctionDescriptor op = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, conditionInRange.getOperationReference());
                genToJVMStack(rangeExpression);
                new StackValue.Local(subjectLocal, subjectType).put(TYPE_OBJECT, v);
                invokeFunctionNoParams(op, Type.BOOLEAN_TYPE, v);
            }
            return StackValue.onStack(Type.BOOLEAN_TYPE);
        }
        JetPattern pattern;
        boolean isNegated;
        if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
            pattern = patternCondition.getPattern();
            isNegated = patternCondition.isNegated();
        }
        else if (condition instanceof JetWhenConditionWithExpression) {
            pattern = ((JetWhenConditionWithExpression) condition).getPattern();
            isNegated = false;
        }
        else {
            throw new UnsupportedOperationException("unsupported kind of when condition");
        }
        return generatePatternMatch(pattern, isNegated,
                                    subjectLocal == -1 ? null : StackValue.local(subjectLocal, subjectType), nextEntry);
    }

    private boolean isIntRangeExpr(JetExpression rangeExpression) {
        if(rangeExpression instanceof JetBinaryExpression) {
            JetBinaryExpression binaryExpression = (JetBinaryExpression) rangeExpression;
            if (binaryExpression.getOperationReference().getReferencedNameElementType() == JetTokens.RANGE) {
                JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, rangeExpression);
                assert jetType != null;
                final DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
                if (isClass(descriptor, "IntRange") ||
                    isClass(descriptor, "CharRange") ||
                    isClass(descriptor, "ByteRange") ||
                    isClass(descriptor, "LongRange") ||
                    isClass(descriptor, "ShortRange")) {
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
        StringBuilder signature = new StringBuilder("(");
        for (int i = 0; i != entries.size(); ++i) {
            signature.append("Ljava/lang/Object;");
        }
        signature.append(")V");

        v.anew(tupleType);
        v.dup();
        for (JetExpression entry : entries) {
            gen(entry, TYPE_OBJECT);
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

    @Override
    public String toString() {
        return context.getContextDescriptor().toString();
    }
}
