/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.backend.common.CodegenUtil;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.*;
import org.jetbrains.jet.codegen.inline.*;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.when.SwitchCodegen;
import org.jetbrains.jet.codegen.when.SwitchCodegenUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.evaluate.EvaluatePackage;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.annotations.AnnotationsPackage;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.Approximation;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.JvmCodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.getNotNull;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getResolvedCall;
import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getResolvedCallWithAssert;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.*;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.TraitImpl;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.jetbrains.org.objectweb.asm.Opcodes.GETFIELD;

public class ExpressionCodegen extends JetVisitor<StackValue, StackValue> implements LocalLookup {
    private static final Set<DeclarationDescriptor> INTEGRAL_RANGES = KotlinBuiltIns.getInstance().getIntegralRanges();

    private final GenerationState state;
    final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;

    public final InstructionAdapter v;
    public final FrameMap myFrameMap;
    private final MethodContext context;
    private final Type returnType;

    private final CodegenStatementVisitor statementVisitor = new CodegenStatementVisitor(this);
    private final MemberCodegen<?> parentCodegen;
    private final TailRecursionCodegen tailRecursionCodegen;
    public final CallGenerator defaultCallGenerator = new CallGenerator.DefaultCallGenerator(this);

    private final Stack<BlockStackElement> blockStackElements = new Stack<BlockStackElement>();

    /*
     * When we create a temporary variable to hold some value not to compute it many times
     * we put it into this map to emit access to that variable instead of evaluating the whole expression
     */
    public final Map<JetElement, StackValue> tempVariables = Maps.newHashMap();

    private int myLastLineNumber = -1;

    public ExpressionCodegen(
            @NotNull MethodVisitor mv,
            @NotNull FrameMap frameMap,
            @NotNull Type returnType,
            @NotNull MethodContext context,
            @NotNull GenerationState state,
            @NotNull MemberCodegen<?> parentCodegen
    ) {
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = state.getBindingContext();
        this.v = new InstructionAdapter(mv);
        this.myFrameMap = frameMap;
        this.context = context;
        this.returnType = returnType;
        this.parentCodegen = parentCodegen;
        this.tailRecursionCodegen = new TailRecursionCodegen(context, this, this.v, state);
    }

    static class BlockStackElement {
    }

    static class LoopBlockStackElement extends BlockStackElement {
        final Label continueLabel;
        final Label breakLabel;
        public final JetSimpleNameExpression targetLabel;

        LoopBlockStackElement(Label breakLabel, Label continueLabel, JetSimpleNameExpression targetLabel) {
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
            this.targetLabel = targetLabel;
        }
    }

    static class FinallyBlockStackElement extends BlockStackElement {
        List<Label> gaps = new ArrayList<Label>();

        final JetTryExpression expression;

        FinallyBlockStackElement(JetTryExpression expression) {
            this.expression = expression;
        }

        private void addGapLabel(Label label){
            gaps.add(label);
        }
    }

    @NotNull
    public GenerationState getState() {
        return state;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public MemberCodegen<?> getParentCodegen() {
        return parentCodegen;
    }

    @NotNull
    public ClassDescriptor generateObjectLiteral(@NotNull JetObjectLiteralExpression literal) {
        JetObjectDeclaration objectDeclaration = literal.getObjectDeclaration();

        ClassDescriptor classDescriptor = bindingContext.get(CLASS, objectDeclaration);
        assert classDescriptor != null;

        Type asmType = asmTypeForAnonymousClass(bindingContext, objectDeclaration);
        ClassBuilder classBuilder = state.getFactory().newVisitor(
                OtherOrigin(objectDeclaration, classDescriptor),
                asmType,
                literal.getContainingFile()
        );

        ClassContext objectContext = context.intoAnonymousClass(classDescriptor, this, OwnerKind.IMPLEMENTATION);
        new ImplementationBodyCodegen(objectDeclaration, objectContext, classBuilder, state, getParentCodegen()).generate();

        return classDescriptor;
    }

    @NotNull
    private StackValue castToRequiredTypeOfInterfaceIfNeeded(
            StackValue inner,
            @NotNull ClassDescriptor provided,
            @NotNull ClassDescriptor required
    ) {
        if (!isInterface(provided) && isInterface(required)) {
            inner.put(OBJECT_TYPE, v);
            Type type = asmType(required.getDefaultType());
            v.checkcast(type);
            return StackValue.onStack(type);
        }

        return inner;
    }

    public StackValue genQualified(StackValue receiver, JetElement selector) {
        return genQualified(receiver, selector, this);
    }

    private StackValue genQualified(StackValue receiver, JetElement selector, JetVisitor<StackValue, StackValue> visitor) {
        if (tempVariables.containsKey(selector)) {
            throw new IllegalStateException("Inconsistent state: expression saved to a temporary variable is a selector");
        }
        if (!(selector instanceof JetBlockExpression)) {
            markStartLineNumber(selector);
        }
        try {
            if (selector instanceof JetExpression) {
                JetExpression expression = (JetExpression) selector;
                SamType samType = bindingContext.get(CodegenBinding.SAM_VALUE, expression);
                if (samType != null) {
                    return genSamInterfaceValue(expression, samType, visitor);
                }
            }

            StackValue stackValue = selector.accept(visitor, receiver);

            Approximation.Info approximationInfo = null;
            if (selector instanceof JetExpression) {
                approximationInfo = bindingContext.get(BindingContext.EXPRESSION_RESULT_APPROXIMATION, (JetExpression) selector);
            }

            return genNotNullAssertions(state, stackValue, approximationInfo);
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
        StackValue tempVar = tempVariables.get(expr);
        return tempVar != null ? tempVar : genQualified(StackValue.none(), expr);
    }

    public void gen(JetElement expr, Type type) {
        StackValue value = Type.VOID_TYPE.equals(type) ? genStatement(expr) : gen(expr);
        value.put(type, v);
    }

    public void genToJVMStack(JetExpression expr) {
        gen(expr, expressionType(expr));
    }

    private StackValue genStatement(JetElement statement) {
        return genQualified(StackValue.none(), statement, statementVisitor);
    }

    @Override
    public StackValue visitClass(@NotNull JetClass klass, StackValue data) {
        return visitClassOrObject(klass);
    }

    private StackValue visitClassOrObject(JetClassOrObject declaration) {
        ClassDescriptor descriptor = bindingContext.get(CLASS, declaration);
        assert descriptor != null;

        Type asmType = asmTypeForAnonymousClass(bindingContext, declaration);
        ClassBuilder classBuilder = state.getFactory().newVisitor(OtherOrigin(declaration, descriptor), asmType, declaration.getContainingFile());

        ClassContext objectContext = context.intoAnonymousClass(descriptor, this, OwnerKind.IMPLEMENTATION);
        new ImplementationBodyCodegen(declaration, objectContext, classBuilder, state, getParentCodegen()).generate();

        if (declaration instanceof JetClass && ((JetClass) declaration).isTrait()) {
            Type traitImplType = state.getTypeMapper().mapTraitImpl(descriptor);
            ClassBuilder traitImplBuilder = state.getFactory().newVisitor(TraitImpl(declaration, descriptor), traitImplType, declaration.getContainingFile());
            ClassContext traitImplContext = context.intoAnonymousClass(descriptor, this, OwnerKind.TRAIT_IMPL);
            new TraitImplBodyCodegen(declaration, traitImplContext, traitImplBuilder, state, parentCodegen).generate();
        }

        return StackValue.none();
    }

    @Override
    public StackValue visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, StackValue data) {
        return visitClassOrObject(declaration);
    }

    @Override
    public StackValue visitExpression(@NotNull JetExpression expression, StackValue receiver) {
        throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
    }

    @Override
    public StackValue visitSuperExpression(@NotNull JetSuperExpression expression, StackValue data) {
        return StackValue.thisOrOuter(this, getSuperCallLabelTarget(expression), true, true);
    }

    @NotNull
    private ClassDescriptor getSuperCallLabelTarget(JetSuperExpression expression) {
        PsiElement labelPsi = bindingContext.get(LABEL_TARGET, expression.getTargetLabel());
        ClassDescriptor labelTarget = (ClassDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, labelPsi);
        DeclarationDescriptor descriptor = bindingContext.get(REFERENCE_TARGET, expression.getInstanceReference());
        // "super<descriptor>@labelTarget"
        if (labelTarget != null) {
            return labelTarget;
        }
        assert descriptor instanceof ClassDescriptor : "Don't know how to generate super-call to not a class";
        return getParentContextSubclassOf((ClassDescriptor) descriptor, context).getThisDescriptor();
    }

    @NotNull
    private Type asmType(@NotNull JetType type) {
        return typeMapper.mapType(type);
    }

    @NotNull
    public Type expressionType(JetExpression expression) {
        JetType type = bindingContext.get(EXPRESSION_TYPE, expression);
        return type == null ? Type.VOID_TYPE : asmType(type);
    }

    @Override
    public StackValue visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getExpression());
    }

    @Override
    public StackValue visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, StackValue receiver) {
        return genQualified(receiver, expression.getBaseExpression());
    }

    private static boolean isEmptyExpression(@Nullable JetElement expr) {
        if (expr == null) {
            return true;
        }
        if (expr instanceof JetBlockExpression) {
            JetBlockExpression blockExpression = (JetBlockExpression) expr;
            List<JetElement> statements = blockExpression.getStatements();
            if (statements.size() == 0 || statements.size() == 1 && isEmptyExpression(statements.get(0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StackValue visitIfExpression(@NotNull JetIfExpression expression, StackValue receiver) {
        return generateIfExpression(expression, false);
    }

    /* package */ StackValue generateIfExpression(JetIfExpression expression, boolean isStatement) {
        Type asmType = isStatement ? Type.VOID_TYPE : expressionType(expression);
        StackValue condition = gen(expression.getCondition());

        JetExpression thenExpression = expression.getThen();
        JetExpression elseExpression = expression.getElse();

        if (isEmptyExpression(thenExpression)) {
            if (isEmptyExpression(elseExpression)) {
                condition.put(asmType, v);
                return StackValue.onStack(asmType);
            }
            return generateSingleBranchIf(condition, expression, elseExpression, false, isStatement);
        }
        else {
            if (isEmptyExpression(elseExpression)) {
                return generateSingleBranchIf(condition, expression, thenExpression, true, isStatement);
            }
        }

        Label elseLabel = new Label();
        condition.condJump(elseLabel, true, v);   // == 0, i.e. false

        Label end = new Label();

        gen(thenExpression, asmType);

        v.goTo(end);
        v.mark(elseLabel);

        gen(elseExpression, asmType);

        markLineNumber(expression, isStatement);

        v.mark(end);

        return StackValue.onStack(asmType);
    }

    @Override
    public StackValue visitWhileExpression(@NotNull JetWhileExpression expression, StackValue receiver) {
        Label condition = new Label();
        v.mark(condition);

        Label end = new Label();
        blockStackElements.push(new LoopBlockStackElement(end, condition, targetLabel(expression)));

        StackValue conditionValue = gen(expression.getCondition());
        conditionValue.condJump(end, true, v);

        generateLoopBody(expression.getBody());

        v.goTo(condition);

        v.mark(end);

        blockStackElements.pop();

        return StackValue.onStack(Type.VOID_TYPE);
    }


    @Override
    public StackValue visitDoWhileExpression(@NotNull JetDoWhileExpression expression, StackValue receiver) {
        Label beginLoopLabel = new Label();
        v.mark(beginLoopLabel);

        Label breakLabel = new Label();
        Label continueLabel = new Label();

        blockStackElements.push(new LoopBlockStackElement(breakLabel, continueLabel, targetLabel(expression)));

        JetExpression body = expression.getBody();
        JetExpression condition = expression.getCondition();
        StackValue conditionValue;

        if (body instanceof JetBlockExpression) {
            // If body's a block, it can contain variable declarations which may be used in the condition of a do-while loop.
            // We handle this case separately because otherwise such variable will be out of the frame map after the block ends
            List<JetElement> doWhileStatements = ((JetBlockExpression) body).getStatements();

            List<JetElement> statements = new ArrayList<JetElement>(doWhileStatements.size() + 1);
            statements.addAll(doWhileStatements);
            statements.add(condition);

            conditionValue = generateBlock(statements, false, continueLabel);
        }
        else {
            if (body != null) {
                gen(body, Type.VOID_TYPE);
            }
            v.mark(continueLabel);
            conditionValue = gen(condition);
        }

        conditionValue.condJump(beginLoopLabel, false, v);
        v.mark(breakLabel);

        blockStackElements.pop();
        return StackValue.none();
    }

    @Override
    public StackValue visitForExpression(@NotNull JetForExpression forExpression, StackValue receiver) {
        // Is it a "1..2" or so
        RangeCodegenUtil.BinaryCall binaryCall = RangeCodegenUtil.getRangeAsBinaryCall(forExpression);
        if (binaryCall != null) {
            ResolvedCall<?> resolvedCall = getResolvedCall(binaryCall.op, bindingContext);
            if (resolvedCall != null) {
                if (RangeCodegenUtil.isOptimizableRangeTo(resolvedCall.getResultingDescriptor())) {
                    generateForLoop(new ForInRangeLiteralLoopGenerator(forExpression, binaryCall));
                    return StackValue.none();
                }
            }
        }

        JetExpression loopRange = forExpression.getLoopRange();
        JetType loopRangeType = bindingContext.get(EXPRESSION_TYPE, loopRange);
        assert loopRangeType != null;
        Type asmLoopRangeType = asmType(loopRangeType);
        if (asmLoopRangeType.getSort() == Type.ARRAY) {
            generateForLoop(new ForInArrayLoopGenerator(forExpression));
            return StackValue.none();
        }

        if (RangeCodegenUtil.isRange(loopRangeType)) {
            generateForLoop(new ForInRangeInstanceLoopGenerator(forExpression));
            return StackValue.none();
        }

        if (RangeCodegenUtil.isProgression(loopRangeType)) {
            generateForLoop(new ForInProgressionExpressionLoopGenerator(forExpression));
            return StackValue.none();
        }

        generateForLoop(new IteratorForLoopGenerator(forExpression));
        return StackValue.none();
    }

    private OwnerKind contextKind() {
        return context.getContextKind();
    }

    private void generateForLoop(AbstractForLoopGenerator generator) {
        Label loopExit = new Label();
        Label loopEntry = new Label();
        Label continueLabel = new Label();

        generator.beforeLoop();
        generator.checkEmptyLoop(loopExit);

        v.mark(loopEntry);
        generator.checkPreCondition(loopExit);

        generator.beforeBody();
        blockStackElements.push(new LoopBlockStackElement(loopExit, continueLabel, targetLabel(generator.forExpression)));
        generator.body();
        blockStackElements.pop();
        v.mark(continueLabel);
        generator.afterBody(loopExit);

        v.goTo(loopEntry);

        v.mark(loopExit);
        generator.afterLoop();
    }

    private abstract class AbstractForLoopGenerator {

        // for (e : E in c) {...}
        protected final JetForExpression forExpression;
        private final Label bodyStart = new Label();
        private final Label bodyEnd = new Label();
        private final List<Runnable> leaveVariableTasks = Lists.newArrayList();

        protected final JetType elementType;
        protected final Type asmElementType;

        protected int loopParameterVar;

        private AbstractForLoopGenerator(@NotNull JetForExpression forExpression) {
            this.forExpression = forExpression;
            this.elementType = getElementType(forExpression);
            this.asmElementType = asmType(elementType);
        }

        @NotNull
        private JetType getElementType(JetForExpression forExpression) {
            JetExpression loopRange = forExpression.getLoopRange();
            assert loopRange != null;
            ResolvedCall<FunctionDescriptor> nextCall = getNotNull(bindingContext,
                                                                   LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange,
                                                                   "No next() function " + DiagnosticUtils.atLocation(loopRange));
            //noinspection ConstantConditions
            return nextCall.getResultingDescriptor().getReturnType();
        }

        public void beforeLoop() {
            JetParameter loopParameter = forExpression.getLoopParameter();
            if (loopParameter != null) {
                // E e = tmp<iterator>.next()
                final VariableDescriptor parameterDescriptor = bindingContext.get(VALUE_PARAMETER, loopParameter);
                @SuppressWarnings("ConstantConditions") final Type asmTypeForParameter = asmType(parameterDescriptor.getType());
                loopParameterVar = myFrameMap.enter(parameterDescriptor, asmTypeForParameter);
                scheduleLeaveVariable(new Runnable() {
                    @Override
                    public void run() {
                        myFrameMap.leave(parameterDescriptor);
                        v.visitLocalVariable(parameterDescriptor.getName().asString(),
                                             asmTypeForParameter.getDescriptor(), null,
                                             bodyStart, bodyEnd,
                                             loopParameterVar);
                    }
                });
            }
            else {
                JetMultiDeclaration multiParameter = forExpression.getMultiParameter();
                assert multiParameter != null;

                // E tmp<e> = tmp<iterator>.next()
                loopParameterVar = createLoopTempVariable(asmElementType);
            }
        }

        public abstract void checkEmptyLoop(@NotNull Label loopExit);

        public abstract void checkPreCondition(@NotNull Label loopExit);

        public void beforeBody() {
            v.mark(bodyStart);

            assignToLoopParameter();

            if (forExpression.getLoopParameter() == null) {
                JetMultiDeclaration multiParameter = forExpression.getMultiParameter();
                assert multiParameter != null;

                generateMultiVariables(multiParameter.getEntries());
            }
        }

        private void generateMultiVariables(List<JetMultiDeclarationEntry> entries) {
            for (JetMultiDeclarationEntry variableDeclaration : entries) {
                final VariableDescriptor componentDescriptor = bindingContext.get(VARIABLE, variableDeclaration);

                @SuppressWarnings("ConstantConditions") final Type componentAsmType = asmType(componentDescriptor.getReturnType());
                final int componentVarIndex = myFrameMap.enter(componentDescriptor, componentAsmType);
                scheduleLeaveVariable(new Runnable() {
                    @Override
                    public void run() {
                        myFrameMap.leave(componentDescriptor);
                        v.visitLocalVariable(componentDescriptor.getName().asString(),
                                             componentAsmType.getDescriptor(), null,
                                             bodyStart, bodyEnd,
                                             componentVarIndex);
                    }
                });

                ResolvedCall<FunctionDescriptor> resolvedCall = bindingContext.get(COMPONENT_RESOLVED_CALL, variableDeclaration);
                assert resolvedCall != null : "Resolved call is null for " + variableDeclaration.getText();
                Call call = makeFakeCall(new TransientReceiver(elementType));
                invokeFunction(call, resolvedCall, StackValue.local(loopParameterVar, asmElementType));

                v.store(componentVarIndex, componentAsmType);
            }
        }

        protected abstract void assignToLoopParameter();

        protected abstract void increment(@NotNull Label loopExit);

        public void body() {
            generateLoopBody(forExpression.getBody());
        }

        private void scheduleLeaveVariable(Runnable runnable) {
            leaveVariableTasks.add(runnable);
        }

        protected int createLoopTempVariable(final Type type) {
            int varIndex = myFrameMap.enterTemp(type);
            scheduleLeaveVariable(new Runnable() {
                @Override
                public void run() {
                    myFrameMap.leaveTemp(type);
                }
            });
            return varIndex;
        }

        public void afterBody(@NotNull Label loopExit) {
            markStartLineNumber(forExpression);

            increment(loopExit);

            v.mark(bodyEnd);
        }

        public void afterLoop() {
            for (Runnable task : Lists.reverse(leaveVariableTasks)) {
                task.run();
            }
        }

        // This method consumes range/progression from stack
        // The result is stored to local variable
        protected void generateRangeOrProgressionProperty(Type loopRangeType, String getterName, Type elementType, int varToStore) {
            Type boxedType = boxType(elementType);
            v.invokevirtual(loopRangeType.getInternalName(), getterName, "()" + boxedType.getDescriptor(), false);
            StackValue.coerce(boxedType, elementType, v);
            v.store(varToStore, elementType);
        }
    }

    private void generateLoopBody(@Nullable JetExpression body) {
        if (body != null) {
            gen(body, Type.VOID_TYPE);
        }
    }

    private class IteratorForLoopGenerator extends AbstractForLoopGenerator {

        private int iteratorVarIndex;
        private final ResolvedCall<FunctionDescriptor> iteratorCall;
        private final ResolvedCall<FunctionDescriptor> nextCall;
        private final Type asmTypeForIterator;

        private IteratorForLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);

            JetExpression loopRange = forExpression.getLoopRange();
            assert loopRange != null;
            this.iteratorCall = getNotNull(bindingContext,
                                           LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRange,
                                           "No .iterator() function " + DiagnosticUtils.atLocation(loopRange));

            JetType iteratorType = iteratorCall.getResultingDescriptor().getReturnType();
            assert iteratorType != null;
            this.asmTypeForIterator = asmType(iteratorType);

            this.nextCall = getNotNull(bindingContext,
                                       LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange,
                                       "No next() function " + DiagnosticUtils.atLocation(loopRange));
        }

        @Override
        public void beforeLoop() {
            super.beforeLoop();

            // Iterator<E> tmp<iterator> = c.iterator()

            iteratorVarIndex = createLoopTempVariable(asmTypeForIterator);

            invokeFunction(iteratorCall, StackValue.none());
            v.store(iteratorVarIndex, asmTypeForIterator);
        }

        @Override
        public void checkEmptyLoop(@NotNull Label loopExit) {
        }

        @Override
        public void checkPreCondition(@NotNull Label loopExit) {
            // tmp<iterator>.hasNext()

            JetExpression loopRange = forExpression.getLoopRange();
            @SuppressWarnings("ConstantConditions") ResolvedCall<FunctionDescriptor> hasNextCall = getNotNull(bindingContext,
                                                                      LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, loopRange,
                                                                      "No hasNext() function " + DiagnosticUtils.atLocation(loopRange));
            @SuppressWarnings("ConstantConditions") Call fakeCall = makeFakeCall(new TransientReceiver(iteratorCall.getResultingDescriptor().getReturnType()));
            invokeFunction(fakeCall, hasNextCall, StackValue.local(iteratorVarIndex, asmTypeForIterator));

            JetType type = hasNextCall.getResultingDescriptor().getReturnType();
            assert type != null && JetTypeChecker.DEFAULT.isSubtypeOf(type, KotlinBuiltIns.getInstance().getBooleanType());

            Type asmType = asmType(type);
            StackValue.coerce(asmType, Type.BOOLEAN_TYPE, v);
            v.ifeq(loopExit);
        }

        @Override
        protected void assignToLoopParameter() {
            @SuppressWarnings("ConstantConditions") Call fakeCall =
                    makeFakeCall(new TransientReceiver(iteratorCall.getResultingDescriptor().getReturnType()));
            invokeFunction(fakeCall, nextCall, StackValue.local(iteratorVarIndex, asmTypeForIterator));
            //noinspection ConstantConditions
            v.store(loopParameterVar, asmType(nextCall.getResultingDescriptor().getReturnType()));
        }

        @Override
        protected void increment(@NotNull Label loopExit) {
        }
    }

    private class ForInArrayLoopGenerator extends AbstractForLoopGenerator {
        private int indexVar;
        private int arrayVar;
        private final JetType loopRangeType;

        private ForInArrayLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);
            loopRangeType = bindingContext.get(EXPRESSION_TYPE, forExpression.getLoopRange());
        }

        @Override
        public void beforeLoop() {
            super.beforeLoop();

            indexVar = createLoopTempVariable(Type.INT_TYPE);

            JetExpression loopRange = forExpression.getLoopRange();
            StackValue value = gen(loopRange);
            Type asmLoopRangeType = asmType(loopRangeType);
            if (value instanceof StackValue.Local && value.type.equals(asmLoopRangeType)) {
                arrayVar = ((StackValue.Local) value).index; // no need to copy local variable into another variable
            }
            else {
                arrayVar = createLoopTempVariable(OBJECT_TYPE);
                value.put(asmLoopRangeType, v);
                v.store(arrayVar, OBJECT_TYPE);
            }

            v.iconst(0);
            v.store(indexVar, Type.INT_TYPE);
        }

        @Override
        public void checkEmptyLoop(@NotNull Label loopExit) {
        }

        @Override
        public void checkPreCondition(@NotNull Label loopExit) {
            v.load(indexVar, Type.INT_TYPE);
            v.load(arrayVar, OBJECT_TYPE);
            v.arraylength();
            v.ificmpge(loopExit);
        }

        @Override
        protected void assignToLoopParameter() {
            Type arrayElParamType;
            if (KotlinBuiltIns.getInstance().isArray(loopRangeType)) {
                arrayElParamType = boxType(asmElementType);
            }
            else {
                arrayElParamType = asmElementType;
            }

            v.load(arrayVar, OBJECT_TYPE);
            v.load(indexVar, Type.INT_TYPE);
            v.aload(arrayElParamType);
            StackValue.onStack(arrayElParamType).put(asmElementType, v);
            v.store(loopParameterVar, asmElementType);
        }

        @Override
        protected void increment(@NotNull Label loopExit) {
            v.iinc(indexVar, 1);
        }
    }

    private abstract class AbstractForInProgressionOrRangeLoopGenerator extends AbstractForLoopGenerator {
        protected int endVar;

        // For integer progressions instead of comparing loopParameterVar with endVar at the beginning of an iteration we check whether
        // loopParameterVar == finalVar at the end of the iteration (and also if there should be any iterations at all, before the loop)
        protected final boolean isIntegerProgression;

        private AbstractForInProgressionOrRangeLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);

            switch (asmElementType.getSort()) {
                case Type.INT:
                case Type.BYTE:
                case Type.SHORT:
                case Type.CHAR:
                case Type.LONG:
                    isIntegerProgression = true;
                    break;

                case Type.DOUBLE:
                case Type.FLOAT:
                    isIntegerProgression = false;
                    break;

                default:
                    throw new IllegalStateException("Unexpected range element type: " + asmElementType);
            }
        }

        @Override
        public void beforeLoop() {
            super.beforeLoop();

            endVar = createLoopTempVariable(asmElementType);
        }

        // Index of the local variable holding the actual last value of the loop parameter.
        // For ranges it equals end, for progressions it's a function of start, end and increment
        protected abstract int getFinalVar();

        protected void checkPostCondition(@NotNull Label loopExit) {
            int finalVar = getFinalVar();
            assert isIntegerProgression && finalVar != -1 :
                    "Post-condition should be checked only in case of integer progressions, finalVar = " + finalVar;

            v.load(loopParameterVar, asmElementType);
            v.load(finalVar, asmElementType);
            if (asmElementType.getSort() == Type.LONG) {
                v.lcmp();
                v.ifeq(loopExit);
            }
            else {
                v.ificmpeq(loopExit);
            }
        }
    }

    private abstract class AbstractForInRangeLoopGenerator extends AbstractForInProgressionOrRangeLoopGenerator {
        private AbstractForInRangeLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);
        }

        @Override
        public void beforeLoop() {
            super.beforeLoop();

            storeRangeStartAndEnd();
        }

        protected abstract void storeRangeStartAndEnd();

        @Override
        protected int getFinalVar() {
            return endVar;
        }

        @Override
        public void checkPreCondition(@NotNull Label loopExit) {
            if (isIntegerProgression) return;

            v.load(loopParameterVar, asmElementType);
            v.load(endVar, asmElementType);

            v.cmpg(asmElementType);
            v.ifgt(loopExit);
        }

        @Override
        public void checkEmptyLoop(@NotNull Label loopExit) {
            if (!isIntegerProgression) return;

            v.load(loopParameterVar, asmElementType);
            v.load(endVar, asmElementType);
            if (asmElementType.getSort() == Type.LONG) {
                v.lcmp();
                v.ifgt(loopExit);
            }
            else {
                v.ificmpgt(loopExit);
            }
        }

        @Override
        protected void assignToLoopParameter() {
        }

        @Override
        protected void increment(@NotNull Label loopExit) {
            if (isIntegerProgression) {
                checkPostCondition(loopExit);
            }

            if (asmElementType == Type.INT_TYPE) {
                v.iinc(loopParameterVar, 1);
            }
            else {
                v.load(loopParameterVar, asmElementType);
                genIncrement(asmElementType, 1, v);
                v.store(loopParameterVar, asmElementType);
            }
        }
    }

    private class ForInRangeLiteralLoopGenerator extends AbstractForInRangeLoopGenerator {
        private final RangeCodegenUtil.BinaryCall rangeCall;

        private ForInRangeLiteralLoopGenerator(
                @NotNull JetForExpression forExpression,
                @NotNull RangeCodegenUtil.BinaryCall rangeCall
        ) {
            super(forExpression);
            this.rangeCall = rangeCall;
        }

        @Override
        protected void storeRangeStartAndEnd() {
            gen(rangeCall.left, asmElementType);
            v.store(loopParameterVar, asmElementType);

            gen(rangeCall.right, asmElementType);
            v.store(endVar, asmElementType);
        }
    }

    private class ForInRangeInstanceLoopGenerator extends AbstractForInRangeLoopGenerator {
        private ForInRangeInstanceLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);
        }

        @Override
        protected void storeRangeStartAndEnd() {
            JetType loopRangeType = bindingContext.get(EXPRESSION_TYPE, forExpression.getLoopRange());
            assert loopRangeType != null;
            Type asmLoopRangeType = asmType(loopRangeType);
            gen(forExpression.getLoopRange(), asmLoopRangeType);
            v.dup();

            generateRangeOrProgressionProperty(asmLoopRangeType, "getStart", asmElementType, loopParameterVar);
            generateRangeOrProgressionProperty(asmLoopRangeType, "getEnd", asmElementType, endVar);
        }
    }

    private class ForInProgressionExpressionLoopGenerator extends AbstractForInProgressionOrRangeLoopGenerator {
        private int incrementVar;
        private Type incrementType;

        private int finalVar;

        private ForInProgressionExpressionLoopGenerator(@NotNull JetForExpression forExpression) {
            super(forExpression);
        }

        @Override
        protected int getFinalVar() {
            return finalVar;
        }

        @Override
        public void beforeLoop() {
            super.beforeLoop();

            incrementVar = createLoopTempVariable(asmElementType);

            JetType loopRangeType = bindingContext.get(EXPRESSION_TYPE, forExpression.getLoopRange());
            assert loopRangeType != null;
            Type asmLoopRangeType = asmType(loopRangeType);

            Collection<VariableDescriptor> incrementProp = loopRangeType.getMemberScope().getProperties(Name.identifier("increment"));
            assert incrementProp.size() == 1 : loopRangeType + " " + incrementProp.size();
            incrementType = asmType(incrementProp.iterator().next().getType());

            gen(forExpression.getLoopRange(), asmLoopRangeType);
            v.dup();
            v.dup();

            generateRangeOrProgressionProperty(asmLoopRangeType, "getStart", asmElementType, loopParameterVar);
            generateRangeOrProgressionProperty(asmLoopRangeType, "getEnd", asmElementType, endVar);
            generateRangeOrProgressionProperty(asmLoopRangeType, "getIncrement", incrementType, incrementVar);

            storeFinalVar();
        }

        private void storeFinalVar() {
            if (!isIntegerProgression) {
                finalVar = -1;
                return;
            }

            v.load(loopParameterVar, asmElementType);
            v.load(endVar, asmElementType);
            v.load(incrementVar, incrementType);

            Type methodParamType = asmElementType.getSort() == Type.LONG ? Type.LONG_TYPE : Type.INT_TYPE;
            v.invokestatic("kotlin/internal/InternalPackage", "getProgressionFinalElement",
                           Type.getMethodDescriptor(methodParamType, methodParamType, methodParamType, methodParamType), false);

            finalVar = createLoopTempVariable(asmElementType);
            v.store(finalVar, asmElementType);
        }

        @Override
        public void checkPreCondition(@NotNull Label loopExit) {
            if (isIntegerProgression) return;

            v.load(loopParameterVar, asmElementType);
            v.load(endVar, asmElementType);
            v.load(incrementVar, incrementType);

            Label negativeIncrement = new Label();
            Label afterIf = new Label();

            if (incrementType.getSort() == Type.DOUBLE) {
                v.dconst(0.0);
            }
            else {
                v.fconst(0.0f);
            }
            v.cmpl(incrementType);
            v.ifle(negativeIncrement); // if increment < 0, jump

            // increment > 0
            v.cmpg(asmElementType); // if loop parameter is NaN, exit from loop, as well
            v.ifgt(loopExit);
            v.goTo(afterIf);

            // increment < 0
            v.mark(negativeIncrement);
            v.cmpl(asmElementType); // if loop parameter is NaN, exit from loop, as well
            v.iflt(loopExit);
            v.mark(afterIf);
        }

        @Override
        public void checkEmptyLoop(@NotNull Label loopExit) {
            if (!isIntegerProgression) return;

            v.load(loopParameterVar, asmElementType);
            v.load(endVar, asmElementType);
            v.load(incrementVar, incrementType);

            Label negativeIncrement = new Label();
            Label afterIf = new Label();

            if (asmElementType.getSort() == Type.LONG) {
                v.lconst(0L);
                v.lcmp();
                v.ifle(negativeIncrement); // if increment < 0, jump

                // increment > 0
                v.lcmp();
                v.ifgt(loopExit);
                v.goTo(afterIf);

                // increment < 0
                v.mark(negativeIncrement);
                v.lcmp();
                v.iflt(loopExit);
                v.mark(afterIf);
            }
            else {
                v.ifle(negativeIncrement); // if increment < 0, jump

                // increment > 0
                v.ificmpgt(loopExit);
                v.goTo(afterIf);

                // increment < 0
                v.mark(negativeIncrement);
                v.ificmplt(loopExit);
                v.mark(afterIf);
            }
        }

        @Override
        protected void assignToLoopParameter() {
        }

        @Override
        protected void increment(@NotNull Label loopExit) {
            if (isIntegerProgression) {
                checkPostCondition(loopExit);
            }

            v.load(loopParameterVar, asmElementType);
            v.load(incrementVar, asmElementType);
            v.add(asmElementType);

            if (asmElementType == Type.BYTE_TYPE || asmElementType == Type.SHORT_TYPE || asmElementType == Type.CHAR_TYPE) {
                StackValue.coerce(Type.INT_TYPE, asmElementType, v);
            }

            v.store(loopParameterVar, asmElementType);
        }
    }


    @Override
    public StackValue visitBreakExpression(@NotNull JetBreakExpression expression, StackValue receiver) {
        return generateBreakOrContinueExpression(expression, true);
    }

    @Override
    public StackValue visitContinueExpression(@NotNull JetContinueExpression expression, StackValue receiver) {
        return generateBreakOrContinueExpression(expression, false);
    }

    @NotNull
    private StackValue generateBreakOrContinueExpression(@NotNull JetExpressionWithLabel expression, boolean isBreak) {
        assert expression instanceof JetContinueExpression || expression instanceof JetBreakExpression;

        if (!blockStackElements.isEmpty()) {
            BlockStackElement stackElement = blockStackElements.peek();

            if (stackElement instanceof FinallyBlockStackElement) {
                FinallyBlockStackElement finallyBlockStackElement = (FinallyBlockStackElement) stackElement;
                //noinspection ConstantConditions
                genFinallyBlockOrGoto(finallyBlockStackElement, null);
            }
            else if (stackElement instanceof LoopBlockStackElement) {
                LoopBlockStackElement loopBlockStackElement = (LoopBlockStackElement) stackElement;
                JetSimpleNameExpression labelElement = expression.getTargetLabel();
                //noinspection ConstantConditions
                if (labelElement == null ||
                    loopBlockStackElement.targetLabel != null &&
                    labelElement.getReferencedName().equals(loopBlockStackElement.targetLabel.getReferencedName())) {
                    v.goTo(isBreak ? loopBlockStackElement.breakLabel : loopBlockStackElement.continueLabel);
                    return StackValue.none();
                }
            }
            else {
                throw new UnsupportedOperationException("Wrong BlockStackElement in processing stack");
            }

            blockStackElements.pop();
            StackValue result = generateBreakOrContinueExpression(expression, isBreak);
            blockStackElements.push(stackElement);
            return result;
        }

        throw new UnsupportedOperationException("Target label for break/continue not found");
    }

    private StackValue generateSingleBranchIf(
            StackValue condition,
            JetIfExpression ifExpression,
            JetExpression expression,
            boolean inverse,
            boolean isStatement
    ) {
        Label elseLabel = new Label();
        condition.condJump(elseLabel, inverse, v);

        if (isStatement) {
            gen(expression, Type.VOID_TYPE);
            v.mark(elseLabel);
            return StackValue.none();
        }
        else {
            Type targetType = expressionType(ifExpression);

            gen(expression, targetType);

            Label end = new Label();
            v.goTo(end);

            v.mark(elseLabel);
            StackValue.putUnitInstance(v);

            markStartLineNumber(ifExpression);
            v.mark(end);
            return StackValue.onStack(targetType);
        }
    }

    @Override
    public StackValue visitConstantExpression(@NotNull JetConstantExpression expression, StackValue receiver) {
        CompileTimeConstant<?> compileTimeValue = getCompileTimeConstant(expression, bindingContext);
        assert compileTimeValue != null;
        return StackValue.constant(compileTimeValue.getValue(), expressionType(expression));
    }

    @Nullable
    public static CompileTimeConstant getCompileTimeConstant(@NotNull JetExpression expression, @NotNull BindingContext bindingContext) {
        CompileTimeConstant<?> compileTimeValue = bindingContext.get(COMPILE_TIME_VALUE, expression);
        if (compileTimeValue instanceof IntegerValueTypeConstant) {
            JetType expectedType = bindingContext.get(EXPRESSION_TYPE, expression);
            assert expectedType != null : "Expression is not type checked: " + expression.getText();
            return EvaluatePackage.createCompileTimeConstantWithType((IntegerValueTypeConstant) compileTimeValue, expectedType);
        }
        return compileTimeValue;
    }

    @Override
    public StackValue visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, StackValue receiver) {
        StringBuilder constantValue = new StringBuilder("");
        JetStringTemplateEntry[] entries = expression.getEntries();

        if (entries.length == 1 && entries[0] instanceof JetStringTemplateEntryWithExpression) {
            JetExpression expr = entries[0].getExpression();
            return genToString(v, gen(expr), expressionType(expr));
        }

        for (JetStringTemplateEntry entry : entries) {
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
            Type type = expressionType(expression);
            return StackValue.constant(constantValue.toString(), type);
        }
        else {
            genStringBuilderConstructor(v);
            for (JetStringTemplateEntry entry : entries) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    invokeAppend(entry.getExpression());
                }
                else {
                    String text = entry instanceof JetEscapeStringTemplateEntry
                                  ? ((JetEscapeStringTemplateEntry) entry).getUnescapedValue()
                                  : entry.getText();
                    v.aconst(text);
                    genInvokeAppendMethod(v, JAVA_STRING_TYPE);
                }
            }
            v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            return StackValue.onStack(AsmTypeConstants.JAVA_STRING_TYPE);
        }
    }

    @Override
    public StackValue visitBlockExpression(@NotNull JetBlockExpression expression, StackValue receiver) {
        return generateBlock(expression, false);
    }

    @Override
    public StackValue visitNamedFunction(@NotNull JetNamedFunction function, StackValue data) {
        assert data == StackValue.none();

        if (JetPsiUtil.isScriptDeclaration(function)) {
            return StackValue.none();
        }

        StackValue closure = genClosure(function, null, KotlinSyntheticClass.Kind.LOCAL_FUNCTION);
        DeclarationDescriptor descriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, function);
        int index = lookupLocalIndex(descriptor);
        closure.put(OBJECT_TYPE, v);
        v.store(index, OBJECT_TYPE);
        return StackValue.none();
    }

    @Override
    public StackValue visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, StackValue receiver) {
        if (Boolean.TRUE.equals(bindingContext.get(BLOCK, expression))) {
            return gen(expression.getFunctionLiteral().getBodyExpression());
        }
        else {
            return genClosure(expression.getFunctionLiteral(), null, KotlinSyntheticClass.Kind.ANONYMOUS_FUNCTION);
        }
    }

    @NotNull
    private StackValue genClosure(
            JetDeclarationWithBody declaration,
            @Nullable SamType samType,
            @NotNull KotlinSyntheticClass.Kind kind
    ) {
        FunctionDescriptor descriptor = bindingContext.get(FUNCTION, declaration);
        assert descriptor != null : "Function is not resolved to descriptor: " + declaration.getText();

        ClosureCodegen closureCodegen = new ClosureCodegen(
                state, declaration, descriptor, samType, context, kind, this,
                new FunctionGenerationStrategy.FunctionDefault(state, descriptor, declaration), parentCodegen
        );
        closureCodegen.gen();

        return closureCodegen.putInstanceOnStack(v, this);
    }

    @Override
    public StackValue visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, StackValue receiver) {
        ClassDescriptor classDescriptor = generateObjectLiteral(expression);
        Type type = typeMapper.mapType(classDescriptor);

        v.anew(type);
        v.dup();

        pushClosureOnStack(classDescriptor, true, defaultCallGenerator);

        //noinspection ConstantConditions
        ResolvedCall<ConstructorDescriptor> superCall = bindingContext.get(CLOSURE, classDescriptor).getSuperCall();
        if (superCall != null) {
            // For an anonymous object, we should also generate all non-default arguments that it captures for its super call
            ConstructorDescriptor superConstructor = superCall.getResultingDescriptor();
            List<ValueParameterDescriptor> superValueParameters = superConstructor.getValueParameters();
            int params = superValueParameters.size();
            List<Type> superMappedTypes = typeMapper.mapToCallableMethod(superConstructor).getValueParameterTypes();
            assert superMappedTypes.size() >= params : String.format("Incorrect number of mapped parameters vs arguments: %d < %d for %s",
                                                                     superMappedTypes.size(), params, classDescriptor);

            List<ResolvedValueArgument> valueArguments = new ArrayList<ResolvedValueArgument>(params);
            List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(params);
            List<Type> mappedTypes = new ArrayList<Type>(params);
            for (ValueParameterDescriptor parameter : superValueParameters) {
                ResolvedValueArgument argument = superCall.getValueArguments().get(parameter);
                if (!(argument instanceof DefaultValueArgument)) {
                    valueArguments.add(argument);
                    valueParameters.add(parameter);
                    mappedTypes.add(superMappedTypes.get(parameter.getIndex()));
                }
            }
            ArgumentGenerator argumentGenerator = new CallBasedArgumentGenerator(this, defaultCallGenerator, valueParameters, mappedTypes);

            argumentGenerator.generate(valueArguments);
        }

        ConstructorDescriptor constructorDescriptor = bindingContext.get(CONSTRUCTOR, expression.getObjectDeclaration());
        assert constructorDescriptor != null : "Unresolved constructor: " + expression.getText();
        JvmMethodSignature constructor = typeMapper.mapSignature(constructorDescriptor);
        v.invokespecial(type.getInternalName(), "<init>", constructor.getAsmMethod().getDescriptor(), false);
        return StackValue.onStack(type);
    }

    public void pushClosureOnStack(@NotNull ClassDescriptor classDescriptor, boolean putThis, @NotNull CallGenerator callGenerator) {
        CalculatedClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        if (closure == null) return;

        int paramIndex = 0;

        if (putThis) {
            ClassDescriptor captureThis = closure.getCaptureThis();
            if (captureThis != null) {
                StackValue thisOrOuter = generateThisOrOuter(captureThis, false);
                assert !isPrimitive(thisOrOuter.type) : "This or outer should be non primitive: " + thisOrOuter.type;
                callGenerator.putCapturedValueOnStack(thisOrOuter, thisOrOuter.type, paramIndex++);
            }
        }

        JetType captureReceiver = closure.getCaptureReceiverType();
        if (captureReceiver != null) {
            Type asmType = typeMapper.mapType(captureReceiver);
            StackValue.Local capturedReceiver = StackValue.local(AsmUtil.getReceiverIndex(context, context.getContextDescriptor()), asmType);
            callGenerator.putCapturedValueOnStack(capturedReceiver, capturedReceiver.type, paramIndex++);
        }

        for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closure.getCaptureVariables().entrySet()) {
            Type sharedVarType = typeMapper.getSharedVarType(entry.getKey());
            if (sharedVarType == null) {
                sharedVarType = typeMapper.mapType((VariableDescriptor) entry.getKey());
            }
            StackValue capturedVar = entry.getValue().getOuterValue(this);
            callGenerator.putCapturedValueOnStack(capturedVar, sharedVarType, paramIndex++);
        }

        ResolvedCall<ConstructorDescriptor> superCall = closure.getSuperCall();
        if (superCall != null) {
            pushClosureOnStack(
                    superCall.getResultingDescriptor().getContainingDeclaration(),
                    putThis && closure.getCaptureThis() == null,
                    callGenerator
            );
        }
    }

    /* package */ StackValue generateBlock(@NotNull JetBlockExpression expression, boolean isStatement) {
        return generateBlock(expression.getStatements(), isStatement, null);
    }

    private StackValue generateBlock(List<JetElement> statements, boolean isStatement, Label labelBeforeLastExpression) {
        Label blockEnd = new Label();

        List<Function<StackValue, Void>> leaveTasks = Lists.newArrayList();

        StackValue answer = StackValue.none();

        for (Iterator<JetElement> iterator = statements.iterator(); iterator.hasNext(); ) {
            JetElement possiblyLabeledStatement = iterator.next();

            JetElement statement = possiblyLabeledStatement instanceof JetExpression
                                   ? JetPsiUtil.safeDeparenthesize((JetExpression) possiblyLabeledStatement, true)
                                   : possiblyLabeledStatement;


            if (statement instanceof JetNamedDeclaration) {
                JetNamedDeclaration declaration = (JetNamedDeclaration) statement;
                if (JetPsiUtil.isScriptDeclaration(declaration)) {
                    continue;
                }
            }

            if (statement instanceof JetMultiDeclaration) {
                JetMultiDeclaration multiDeclaration = (JetMultiDeclaration) statement;
                for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    generateLocalVariableDeclaration(entry, blockEnd, leaveTasks);
                }
            }

            if (statement instanceof JetVariableDeclaration) {
                generateLocalVariableDeclaration((JetVariableDeclaration) statement, blockEnd, leaveTasks);
            }

            if (statement instanceof JetNamedFunction) {
                generateLocalFunctionDeclaration((JetNamedFunction) statement, leaveTasks);
            }

            boolean isExpression = !iterator.hasNext() && !isStatement;
            if (isExpression && labelBeforeLastExpression != null) {
                v.mark(labelBeforeLastExpression);
            }

            StackValue result = isExpression ? gen(possiblyLabeledStatement) : genStatement(possiblyLabeledStatement);

            if (!iterator.hasNext()) {
                answer = result;
            }
            else {
                result.put(Type.VOID_TYPE, v);
            }
        }

        v.mark(blockEnd);

        for (Function<StackValue, Void> task : Lists.reverse(leaveTasks)) {
            task.fun(answer);
        }

        return answer;
    }

    private void generateLocalVariableDeclaration(
            @NotNull JetVariableDeclaration variableDeclaration,
            final @NotNull Label blockEnd,
            @NotNull List<Function<StackValue, Void>> leaveTasks
    ) {
        final VariableDescriptor variableDescriptor = bindingContext.get(VARIABLE, variableDeclaration);
        assert variableDescriptor != null;

        final Label scopeStart = new Label();
        v.mark(scopeStart);

        final Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
        final Type type = sharedVarType != null ? sharedVarType : asmType(variableDescriptor.getType());
        int index = myFrameMap.enter(variableDescriptor, type);

        if (sharedVarType != null) {
            v.anew(sharedVarType);
            v.dup();
            v.invokespecial(sharedVarType.getInternalName(), "<init>", "()V", false);
            v.store(index, OBJECT_TYPE);
        }

        leaveTasks.add(new Function<StackValue, Void>() {
            @Override
            public Void fun(StackValue answer) {
                int index = myFrameMap.leave(variableDescriptor);

                if (sharedVarType != null) {
                    if (answer instanceof StackValue.Shared && index == ((StackValue.Shared) answer).getIndex()) {
                        ((StackValue.Shared) answer).releaseOnPut();
                    }
                    else {
                        v.aconst(null);
                        v.store(index, OBJECT_TYPE);
                    }
                }
                v.visitLocalVariable(variableDescriptor.getName().asString(), type.getDescriptor(), null, scopeStart, blockEnd,
                                     index);
                return null;
            }
        });
    }

    private void generateLocalFunctionDeclaration(
            @NotNull JetNamedFunction namedFunction,
            @NotNull List<Function<StackValue, Void>> leaveTasks
    ) {
        final DeclarationDescriptor descriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, namedFunction);
        myFrameMap.enter(descriptor, OBJECT_TYPE);

        leaveTasks.add(new Function<StackValue, Void>() {
            @Override
            public Void fun(StackValue value) {
                myFrameMap.leave(descriptor);
                return null;
            }
        });
    }

    public void markStartLineNumber(@NotNull JetElement element) {
        markLineNumber(element, false);
    }

    public void markLineNumber(@NotNull JetElement statement, boolean markEndOffset) {
        Integer lineNumber = CodegenUtil.getLineNumberForElement(statement, markEndOffset);
        if (lineNumber == null || lineNumber == myLastLineNumber) {
            return;
        }
        myLastLineNumber = lineNumber;

        Label label = new Label();
        v.visitLabel(label);
        v.visitLineNumber(lineNumber, label);
    }

    private void doFinallyOnReturn() {
        if(!blockStackElements.isEmpty()) {
            BlockStackElement stackElement = blockStackElements.peek();
            if (stackElement instanceof FinallyBlockStackElement) {
                FinallyBlockStackElement finallyBlockStackElement = (FinallyBlockStackElement) stackElement;
                genFinallyBlockOrGoto(finallyBlockStackElement, null);
            }
            else if (stackElement instanceof LoopBlockStackElement) {

            } else {
                throw new UnsupportedOperationException("Wrong BlockStackElement in processing stack");
            }

            blockStackElements.pop();
            doFinallyOnReturn();
            blockStackElements.push(stackElement);
        }
    }

    public boolean hasFinallyBlocks() {
        for (BlockStackElement element : blockStackElements) {
            if (element instanceof FinallyBlockStackElement) {
                return true;
            }
        }
        return false;
    }

    private void genFinallyBlockOrGoto(
            @Nullable FinallyBlockStackElement finallyBlockStackElement,
            @Nullable Label tryCatchBlockEnd
    ) {
        if (finallyBlockStackElement != null) {
            assert finallyBlockStackElement.gaps.size() % 2 == 0 : "Finally block gaps are inconsistent";

            BlockStackElement topOfStack = blockStackElements.pop();
            assert topOfStack == finallyBlockStackElement : "Top element of stack doesn't equals processing finally block";

            JetTryExpression jetTryExpression = finallyBlockStackElement.expression;
            Label finallyStart = new Label();
            v.mark(finallyStart);
            finallyBlockStackElement.addGapLabel(finallyStart);

            //noinspection ConstantConditions
            gen(jetTryExpression.getFinallyBlock().getFinalExpression(), Type.VOID_TYPE);
        }

        if (tryCatchBlockEnd != null) {
            v.goTo(tryCatchBlockEnd);
        }

        if (finallyBlockStackElement != null) {
            Label finallyEnd = new Label();
            v.mark(finallyEnd);
            finallyBlockStackElement.addGapLabel(finallyEnd);

            blockStackElements.push(finallyBlockStackElement);
        }
    }

    @Override
    public StackValue visitReturnExpression(@NotNull JetReturnExpression expression, StackValue receiver) {
        JetExpression returnedExpression = expression.getReturnedExpression();
        CallableMemberDescriptor descriptor = getContext().getContextDescriptor();
        NonLocalReturnInfo nonLocalReturn = getNonLocalReturnInfo(descriptor, expression);
        boolean isNonLocalReturn = nonLocalReturn != null;
        if (isNonLocalReturn && !state.isInlineEnabled()) {
            throw new CompilationException("Non local returns requires enabled inlining", null, expression);
        }

        Type returnType = isNonLocalReturn ? nonLocalReturn.returnType : this.returnType;
        if (returnedExpression != null) {
            gen(returnedExpression, returnType);
        }

        generateFinallyBlocksIfNeeded(returnType);

        if (isNonLocalReturn) {
            InlineCodegenUtil.generateGlobalReturnFlag(v, nonLocalReturn.labelName);
        }
        v.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

        return StackValue.none();
    }

    public void generateFinallyBlocksIfNeeded(Type returnType) {
        if (hasFinallyBlocks()) {
            if (!Type.VOID_TYPE.equals(returnType)) {
                int returnValIndex = myFrameMap.enterTemp(returnType);
                StackValue.local(returnValIndex, returnType).store(returnType, v);
                doFinallyOnReturn();
                StackValue.local(returnValIndex, returnType).put(returnType, v);
                myFrameMap.leaveTemp(returnType);
            }
            else {
                doFinallyOnReturn();
            }
        }
    }

    @Nullable
    private NonLocalReturnInfo getNonLocalReturnInfo(@NotNull CallableMemberDescriptor descriptor, @NotNull JetReturnExpression expression) {
        //call inside lambda
        if (isLocalFunOrLambda(descriptor) && descriptor.getName().isSpecial()) {
            if (expression.getLabelName() == null) {
                //non labeled return couldn't be local in lambda
                FunctionDescriptor containingFunction =
                        BindingContextUtils.getContainingFunctionSkipFunctionLiterals(descriptor, true).getFirst();
                //ROOT_LABEL to prevent clashing with existing labels
                return new NonLocalReturnInfo(typeMapper.mapReturnType(containingFunction), InlineCodegenUtil.ROOT_LABEL);
            }

            PsiElement element = bindingContext.get(LABEL_TARGET, expression.getTargetLabel());
            if (element != DescriptorToSourceUtils.callableDescriptorToDeclaration(context.getContextDescriptor())) {
                DeclarationDescriptor elementDescriptor = typeMapper.getBindingContext().get(DECLARATION_TO_DESCRIPTOR, element);
                assert element != null : "Expression should be not null " + expression.getText();
                assert elementDescriptor != null : "Descriptor should be not null: " + element.getText();
                return new NonLocalReturnInfo(typeMapper.mapReturnType((CallableDescriptor) elementDescriptor), expression.getLabelName());
            }
        }
        return null;
    }

    public void returnExpression(JetExpression expr) {
        boolean isBlockedNamedFunction = expr instanceof JetBlockExpression && expr.getParent() instanceof JetNamedFunction;

        // If generating body for named block-bodied function, generate it as sequence of statements
        gen(expr, isBlockedNamedFunction ? Type.VOID_TYPE : returnType);

        // If it does not end with return we should return something
        // because if we don't there can be VerifyError (specific cases with Nothing-typed expressions)
        if (!endsWithReturn(expr)) {
            markLineNumber(expr, true);

            if (isBlockedNamedFunction && !Type.VOID_TYPE.equals(expressionType(expr))) {
                StackValue.none().put(returnType, v);
            }

            v.areturn(returnType);
        }
    }

    private static boolean endsWithReturn(JetElement bodyExpression) {
        if (bodyExpression instanceof JetBlockExpression) {
            List<JetElement> statements = ((JetBlockExpression) bodyExpression).getStatements();
            return statements.size() > 0 && statements.get(statements.size() - 1) instanceof JetReturnExpression;
        }

        return bodyExpression instanceof JetReturnExpression;
    }

    @Override
    public StackValue visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, StackValue receiver) {
        ResolvedCall<?> resolvedCall = getResolvedCall(expression, bindingContext);

        DeclarationDescriptor descriptor;
        if (resolvedCall == null) {
            descriptor = bindingContext.get(REFERENCE_TARGET, expression);
        }
        else {
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall call = (VariableAsFunctionResolvedCall) resolvedCall;
                resolvedCall = call.getVariableCall();
            }
            receiver = StackValue.receiver(resolvedCall, receiver, this, null);
            descriptor = resolvedCall.getResultingDescriptor();
            if (descriptor instanceof FakeCallableDescriptorForObject) {
                descriptor = ((FakeCallableDescriptorForObject) descriptor).getReferencedDescriptor();
            }
        }

        assert descriptor != null : "Couldn't find descriptor for '" + expression.getText() + "'";
        descriptor = descriptor.getOriginal();

        if (descriptor instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor memberDescriptor = DescriptorUtils.unwrapFakeOverride((CallableMemberDescriptor) descriptor);

            IntrinsicMethod intrinsic = state.getIntrinsics().getIntrinsic(memberDescriptor);
            if (intrinsic != null) {
                Type returnType = typeMapper.mapType(memberDescriptor);
                intrinsic.generate(this, v, returnType, expression, Collections.<JetExpression>emptyList(), receiver);
                return StackValue.onStack(returnType);
            }
        }

        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

            boolean directToField =
                    expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER && contextKind() != OwnerKind.TRAIT_IMPL;
            JetExpression r = getReceiverForSelector(expression);
            boolean isSuper = r instanceof JetSuperExpression;
            propertyDescriptor = accessiblePropertyDescriptor(propertyDescriptor);
            StackValue.Property iValue =
                intermediateValueForProperty(propertyDescriptor, directToField, isSuper ? (JetSuperExpression) r : null);
            if (directToField) {
                receiver = StackValue.receiverWithoutReceiverArgument(receiver);
            }

            //pop receiver via put(VOID_TYPE) in case of access to backing field that moved to outer class!!!
            receiver.put(!iValue.isPropertyWithBackingFieldInOuterClass() ? receiver.type : Type.VOID_TYPE, v);

            return iValue;
        }

        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if (classDescriptor.getKind() == ClassKind.OBJECT || classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                return StackValue.singleton(classDescriptor, typeMapper);
            }
            if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
                DeclarationDescriptor enumClass = classDescriptor.getContainingDeclaration();
                assert DescriptorUtils.isEnumClass(enumClass) : "Enum entry should be declared in enum class: " + descriptor;
                Type type = typeMapper.mapType((ClassDescriptor) enumClass);
                return StackValue.field(type, type, descriptor.getName().asString(), true);
            }
            ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
            if (classObjectDescriptor != null) {
                return StackValue.singleton(classObjectDescriptor, typeMapper);
            }
            return StackValue.none();
        }

        StackValue localOrCaptured = findLocalOrCapturedValue(descriptor);
        if (localOrCaptured != null) {
            return localOrCaptured;
        }

        if (descriptor instanceof ValueParameterDescriptor && descriptor.getContainingDeclaration() instanceof ScriptDescriptor) {
            ScriptDescriptor scriptDescriptor = (ScriptDescriptor) descriptor.getContainingDeclaration();
            Type scriptClassType = asmTypeForScriptDescriptor(bindingContext, scriptDescriptor);
            ValueParameterDescriptor valueParameterDescriptor = (ValueParameterDescriptor) descriptor;
            ClassDescriptor scriptClass = bindingContext.get(CLASS_FOR_SCRIPT, scriptDescriptor);
            StackValue script = StackValue.thisOrOuter(this, scriptClass, false, false);
            script.put(script.type, v);
            Type fieldType = typeMapper.mapType(valueParameterDescriptor);
            return StackValue.field(fieldType, scriptClassType, valueParameterDescriptor.getName().getIdentifier(), false);
        }

        throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
    }

    @Nullable
    public StackValue findLocalOrCapturedValue(@NotNull DeclarationDescriptor descriptor) {
        int index = lookupLocalIndex(descriptor);
        if (index >= 0) {
            return stackValueForLocal(descriptor, index);
        }

        StackValue value = context.lookupInContext(descriptor, StackValue.local(0, OBJECT_TYPE), state, false);
        if (value == null) return null;

        if (context.isSpecialStackValue(value)) {
            return value;
        }

        if (value instanceof StackValue.Composed) {
            StackValue.Composed composed = (StackValue.Composed) value;
            composed.prefix.put(OBJECT_TYPE, v);
            value = composed.suffix;
        }

        if (value instanceof StackValue.FieldForSharedVar) {
            StackValue.FieldForSharedVar fieldForSharedVar = (StackValue.FieldForSharedVar) value;
            Type sharedType = StackValue.sharedTypeForType(value.type);
            v.visitFieldInsn(GETFIELD, fieldForSharedVar.owner.getInternalName(), fieldForSharedVar.name,
                             sharedType.getDescriptor());
        }

        return value;
    }


    private StackValue stackValueForLocal(DeclarationDescriptor descriptor, int index) {
        if (descriptor instanceof VariableDescriptor) {
            Type sharedVarType = typeMapper.getSharedVarType(descriptor);
            JetType outType = ((VariableDescriptor) descriptor).getType();
            if (sharedVarType != null) {
                return StackValue.shared(index, asmType(outType));
            }
            else {
                return StackValue.local(index, asmType(outType));
            }
        }
        else {
            return StackValue.local(index, OBJECT_TYPE);
        }
    }

    @Override
    public boolean lookupLocal(DeclarationDescriptor descriptor) {
        return lookupLocalIndex(descriptor) != -1;
    }

    public int lookupLocalIndex(DeclarationDescriptor descriptor) {
        return myFrameMap.getIndex(descriptor);
    }

    @Nullable
    private static JetType getPropertyDelegateType(@NotNull PropertyDescriptor descriptor, @NotNull BindingContext bindingContext) {
        PropertyGetterDescriptor getter = descriptor.getGetter();
        if (getter != null) {
            Call call = bindingContext.get(DELEGATED_PROPERTY_CALL, getter);
            return call != null ? call.getExplicitReceiver().getType() : null;
        }
        return null;
    }

    @NotNull
    public StackValue.Property intermediateValueForProperty(
            @NotNull PropertyDescriptor propertyDescriptor,
            boolean forceField,
            @Nullable JetSuperExpression superExpression
    ) {
        return intermediateValueForProperty(propertyDescriptor, forceField, superExpression, MethodKind.GENERAL);
    }

    public StackValue.Property intermediateValueForProperty(
            @NotNull PropertyDescriptor propertyDescriptor,
            boolean forceField,
            @Nullable JetSuperExpression superExpression,
            @NotNull MethodKind methodKind
    ) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();

        boolean isBackingFieldInAnotherClass = AsmUtil.isPropertyWithBackingFieldInOuterClass(propertyDescriptor);
        boolean isStatic = DescriptorUtils.isStaticDeclaration(propertyDescriptor);
        boolean isSuper = superExpression != null;
        boolean isExtensionProperty = propertyDescriptor.getExtensionReceiverParameter() != null;

        JetType delegateType = getPropertyDelegateType(propertyDescriptor, bindingContext);
        boolean isDelegatedProperty = delegateType != null;

        CallableMethod callableGetter = null;
        CallableMethod callableSetter = null;

        boolean skipPropertyAccessors = forceField && !isBackingFieldInAnotherClass;

        CodegenContext backingFieldContext = context.getParentContext();

        if (isBackingFieldInAnotherClass && forceField) {
            //delegate call to classObject owner : OWNER
            backingFieldContext = context.findParentContextWithDescriptor(containingDeclaration.getContainingDeclaration());
            int flags = AsmUtil.getVisibilityForSpecialPropertyBackingField(propertyDescriptor, isDelegatedProperty);
            skipPropertyAccessors = (flags & ACC_PRIVATE) == 0 || methodKind == MethodKind.SYNTHETIC_ACCESSOR || methodKind == MethodKind.INITIALIZER;
            if (!skipPropertyAccessors) {
                propertyDescriptor = (PropertyDescriptor) backingFieldContext.getAccessor(propertyDescriptor, true, delegateType);
            }
            isStatic = true;
        }

        if (!skipPropertyAccessors) {
            if (couldUseDirectAccessToProperty(propertyDescriptor, true, isDelegatedProperty, context)) {
                callableGetter = null;
            }
            else {
                if (isSuper && !isInterface(containingDeclaration)) {
                    ClassDescriptor owner = getSuperCallLabelTarget(superExpression);
                    CodegenContext c = context.findParentContextWithDescriptor(owner);
                    assert c != null : "Couldn't find a context for a super-call: " + propertyDescriptor;
                    if (c != context.getParentContext()) {
                        propertyDescriptor = (PropertyDescriptor) c.getAccessor(propertyDescriptor);
                    }
                }

                propertyDescriptor = accessiblePropertyDescriptor(propertyDescriptor);

                PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
                if (getter != null) {
                    callableGetter = typeMapper.mapToCallableMethod(getter, isSuper || MethodKind.SYNTHETIC_ACCESSOR == methodKind, context);
                }
            }

            if (propertyDescriptor.isVar()) {
                PropertySetterDescriptor setter = propertyDescriptor.getSetter();
                if (setter != null) {
                    if (couldUseDirectAccessToProperty(propertyDescriptor, false, isDelegatedProperty, context)) {
                        callableSetter = null;
                    }
                    else {
                        callableSetter = typeMapper.mapToCallableMethod(setter, isSuper || MethodKind.SYNTHETIC_ACCESSOR == methodKind, context);
                    }
                }
            }
        }

        Type owner;
        CallableMethod callableMethod = callableGetter != null ? callableGetter : callableSetter;

        propertyDescriptor = DescriptorUtils.unwrapFakeOverride(propertyDescriptor);
        if (callableMethod == null) {
            owner = typeMapper.mapOwner(isBackingFieldInAnotherClass ? propertyDescriptor.getContainingDeclaration() : propertyDescriptor,
                                        isCallInsideSameModuleAsDeclared(propertyDescriptor, context, state.getOutDirectory()));
        }
        else {
            owner = callableMethod.getOwner();
        }

        String fieldName;
        if (isExtensionProperty && !isDelegatedProperty) {
            fieldName = null;
        }
        else if (propertyDescriptor.getContainingDeclaration() == backingFieldContext.getContextDescriptor()) {
            assert backingFieldContext instanceof FieldOwnerContext
                    : "Actual context is " + backingFieldContext + " but should be instance of FieldOwnerContext";
            fieldName = ((FieldOwnerContext) backingFieldContext).getFieldName(propertyDescriptor, isDelegatedProperty);
        }
        else {
            fieldName = JvmAbi.getDefaultFieldNameForProperty(propertyDescriptor.getName(), isDelegatedProperty);
        }

        return StackValue.property(propertyDescriptor, owner,
                            typeMapper.mapType(isDelegatedProperty && forceField ? delegateType : propertyDescriptor.getOriginal().getType()),
                            isStatic, fieldName, callableGetter, callableSetter, state);

    }

    @Override
    public StackValue visitCallExpression(@NotNull JetCallExpression expression, StackValue receiver) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
        CallableDescriptor funDescriptor = resolvedCall.getResultingDescriptor();

        if (!(funDescriptor instanceof FunctionDescriptor)) {
            throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);
        }

        funDescriptor = accessibleFunctionDescriptor((FunctionDescriptor) funDescriptor);

        if (funDescriptor instanceof ConstructorDescriptor) {
            return generateNewCall(expression, resolvedCall);
        }

        if (funDescriptor.getOriginal() instanceof SamConstructorDescriptor) {
            //noinspection ConstantConditions
            SamType samType = SamType.create(funDescriptor.getReturnType());
            assert samType != null : "SamType is not created for SAM constructor: " + funDescriptor;
            return invokeSamConstructor(expression, resolvedCall, samType);
        }

        return invokeFunction(resolvedCall, receiver);
    }

    @NotNull
    private StackValue invokeSamConstructor(
            @NotNull JetCallExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull SamType samType
    ) {
        List<ResolvedValueArgument> arguments = resolvedCall.getValueArgumentsByIndex();
        if (arguments == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor());
        }
        ResolvedValueArgument argument = arguments.get(0);
        if (!(argument instanceof ExpressionValueArgument)) {
            throw new IllegalStateException(
                    "argument of SAM constructor is " + argument.getClass().getName() + " " + expression.getText());
        }
        ValueArgument valueArgument = ((ExpressionValueArgument) argument).getValueArgument();
        assert valueArgument != null : "getValueArgument() is null for " + expression.getText();
        JetExpression argumentExpression = valueArgument.getArgumentExpression();
        assert argumentExpression != null : "getArgumentExpression() is null for " + expression.getText();

        return genSamInterfaceValue(argumentExpression, samType, this);
    }

    @NotNull
    private StackValue genSamInterfaceValue(
            @NotNull JetExpression expression,
            @NotNull SamType samType,
            @NotNull JetVisitor<StackValue, StackValue> visitor
    ) {
        if (expression instanceof JetFunctionLiteralExpression) {
            return genClosure(((JetFunctionLiteralExpression) expression).getFunctionLiteral(), samType,
                              KotlinSyntheticClass.Kind.SAM_LAMBDA);
        }

        Type asmType = state.getSamWrapperClasses().getSamWrapperClass(samType, expression.getContainingJetFile(), getParentCodegen());

        v.anew(asmType);
        v.dup();

        Type functionType = typeMapper.mapType(samType.getKotlinFunctionType());
        expression.accept(visitor, StackValue.none()).put(functionType, v);

        Label ifNonNull = new Label();
        Label afterAll = new Label();

        v.dup();
        v.ifnonnull(ifNonNull);

        // if null: pop function value and wrapper objects, put null
        v.pop();
        v.pop2();
        v.aconst(null);
        v.goTo(afterAll);

        v.mark(ifNonNull);
        v.invokespecial(asmType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), false);

        v.mark(afterAll);
        return StackValue.onStack(asmType);
    }

    @NotNull
    private PropertyDescriptor accessiblePropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        return context.accessiblePropertyDescriptor(propertyDescriptor);
    }

    @NotNull
    protected FunctionDescriptor accessibleFunctionDescriptor(FunctionDescriptor fd) {
        return context.accessibleFunctionDescriptor(fd);
    }

    @NotNull
    public StackValue invokeFunction(@NotNull ResolvedCall<?> resolvedCall, @NotNull StackValue receiver) {
        return invokeFunction(resolvedCall.getCall(), resolvedCall, receiver);
    }

    @NotNull
    public StackValue invokeFunction(@NotNull Call call, @NotNull ResolvedCall<?> resolvedCall, @NotNull StackValue receiver) {
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            return invokeFunction(call, ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall(), receiver);
        }

        FunctionDescriptor fd = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        JetSuperExpression superCallExpression = getSuperCallExpression(call);
        boolean superCall = superCallExpression != null;

        if (superCall && !isInterface(fd.getContainingDeclaration())) {
            ClassDescriptor owner = getSuperCallLabelTarget(superCallExpression);
            CodegenContext c = context.findParentContextWithDescriptor(owner);
            assert c != null : "Couldn't find a context for a super-call: " + fd;
            if (c != context.getParentContext()) {
                fd = (FunctionDescriptor) c.getAccessor(fd);
            }
        }

        Callable callable = resolveToCallable(accessibleFunctionDescriptor(fd), superCall);

        if (callable instanceof CallableMethod) {
            CallableMethod callableMethod = (CallableMethod) callable;
            invokeMethodWithArguments(callableMethod, resolvedCall, receiver);
            //noinspection ConstantConditions
            Type returnType = typeMapper.mapReturnType(resolvedCall.getResultingDescriptor());
            StackValue.coerce(callableMethod.getReturnType(), returnType, v);
            return StackValue.onStack(returnType);
        }
        else {
            receiver = StackValue.receiver(resolvedCall, receiver, this, null);

            List<JetExpression> args = new ArrayList<JetExpression>();
            for (ValueArgument argument : call.getValueArguments()) {
                args.add(argument.getArgumentExpression());
            }

            Type returnType = typeMapper.mapType(resolvedCall.getResultingDescriptor());

            ((IntrinsicMethod) callable).generate(this, v, returnType, call.getCallElement(), args, receiver);
            return StackValue.onStack(returnType);
        }
    }

    @Nullable
    private static JetSuperExpression getSuperCallExpression(@NotNull Call call) {
        ReceiverValue explicitReceiver = call.getExplicitReceiver();
        if (explicitReceiver instanceof ExpressionReceiver) {
            JetExpression receiverExpression = ((ExpressionReceiver) explicitReceiver).getExpression();
            if (receiverExpression instanceof JetSuperExpression) {
                return (JetSuperExpression) receiverExpression;
            }
        }
        return null;
    }

    // Find the first parent of the current context which corresponds to a subclass of a given class
    @NotNull
    private static CodegenContext getParentContextSubclassOf(ClassDescriptor descriptor, CodegenContext context) {
        CodegenContext c = context;
        while (true) {
            if (c instanceof ClassContext && DescriptorUtils.isSubclass(c.getThisDescriptor(), descriptor)) {
                return c;
            }
            c = c.getParentContext();
            assert c != null;
        }
    }

    @NotNull
    Callable resolveToCallable(@NotNull FunctionDescriptor fd, boolean superCall) {
        IntrinsicMethod intrinsic = state.getIntrinsics().getIntrinsic(fd);
        if (intrinsic != null) {
            return intrinsic;
        }

        return resolveToCallableMethod(fd, superCall, context);
    }

    @NotNull
    private CallableMethod resolveToCallableMethod(@NotNull FunctionDescriptor fd, boolean superCall, @NotNull CodegenContext context) {
        SimpleFunctionDescriptor originalOfSamAdapter = (SimpleFunctionDescriptor) SamCodegenUtil.getOriginalIfSamAdapter(fd);
        return typeMapper.mapToCallableMethod(originalOfSamAdapter != null ? originalOfSamAdapter : fd, superCall, context);
    }

    public void invokeMethodWithArguments(
            @NotNull CallableMethod callableMethod,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull StackValue receiver
    ) {
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            resolvedCall = ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall();
        }

        CallGenerator callGenerator = getOrCreateCallGenerator(resolvedCall);
        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

        assert callGenerator == defaultCallGenerator || !tailRecursionCodegen.isTailRecursion(resolvedCall) :
                "Tail recursive method can't be inlined: " + descriptor;

        ArgumentGenerator argumentGenerator = new CallBasedArgumentGenerator(this, callGenerator, descriptor.getValueParameters(),
                                                                             callableMethod.getValueParameterTypes());

        invokeMethodWithArguments(callableMethod, resolvedCall, receiver, callGenerator, argumentGenerator);
    }

    public void invokeMethodWithArguments(
            @NotNull CallableMethod callableMethod,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull StackValue receiver,
            @NotNull CallGenerator callGenerator,
            @NotNull ArgumentGenerator argumentGenerator
    ) {
        if (!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor)) { // otherwise already
            receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
            receiver.put(receiver.type, v);
        }

        callGenerator.putHiddenParams();

        List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
        assert valueArguments != null : "Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor();

        List<Integer> masks = argumentGenerator.generate(valueArguments);

        if (tailRecursionCodegen.isTailRecursion(resolvedCall)) {
            tailRecursionCodegen.generateTailRecursion(resolvedCall);
            return;
        }

        for (int mask : masks) {
            callGenerator.putValueIfNeeded(null, Type.INT_TYPE, StackValue.constant(mask, Type.INT_TYPE));
        }

        callGenerator.genCall(callableMethod, resolvedCall, !masks.isEmpty(), this);
    }

    @NotNull
    protected CallGenerator getOrCreateCallGenerator(
            @NotNull CallableDescriptor descriptor,
            @Nullable JetElement callElement,
            @Nullable ReifiedTypeParameterMappings reifierTypeParameterMappings
    ) {
        if (callElement == null) return defaultCallGenerator;

        boolean isInline = state.isInlineEnabled() &&
                           descriptor instanceof SimpleFunctionDescriptor &&
                           ((SimpleFunctionDescriptor) descriptor).getInlineStrategy().isInline();

        if (!isInline) return defaultCallGenerator;

        SimpleFunctionDescriptor original = DescriptorUtils.unwrapFakeOverride((SimpleFunctionDescriptor) descriptor.getOriginal());
        return new InlineCodegen(this, state, original, callElement, reifierTypeParameterMappings);
    }

    @NotNull
    public CallGenerator getOrCreateCallGenerator(@NotNull FunctionDescriptor descriptor, @Nullable JetNamedFunction function) {
        return getOrCreateCallGenerator(descriptor, function, null);
    }

    @NotNull
    private CallGenerator getOrCreateCallGenerator(@NotNull ResolvedCall<?> resolvedCall) {
        Map<TypeParameterDescriptor, JetType> typeArguments = resolvedCall.getTypeArguments();
        ReifiedTypeParameterMappings mappings = new ReifiedTypeParameterMappings(typeArguments.size());
        for (Map.Entry<TypeParameterDescriptor, JetType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor key = entry.getKey();
            if (!key.isReified()) continue;

            TypeParameterDescriptor parameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(entry.getValue());
            if (parameterDescriptor == null) {
                // type is not generic
                // boxType call needed because inlined method is compiled for T as java/lang/Object
                mappings.addParameterMappingToType(
                        key.getIndex(),
                        key.getName().getIdentifier(),
                        boxType(asmType(entry.getValue()))
                );
            }
            else {
                mappings.addParameterMappingToNewParameter(
                        key.getIndex(),
                        key.getName().getIdentifier(),
                        parameterDescriptor.getIndex()
                );
            }
        }
        return getOrCreateCallGenerator(
                resolvedCall.getResultingDescriptor(), resolvedCall.getCall().getCallElement(), mappings
        );
    }

    public void generateReceiverValue(@NotNull ReceiverValue receiverValue, @NotNull Type type) {
        if (receiverValue instanceof ClassReceiver) {
            ClassDescriptor receiverDescriptor = ((ClassReceiver) receiverValue).getDeclarationDescriptor();
            if (DescriptorUtils.isClassObject(receiverDescriptor)) {
                CallableMemberDescriptor contextDescriptor = context.getContextDescriptor();
                if (contextDescriptor instanceof FunctionDescriptor && receiverDescriptor == contextDescriptor.getContainingDeclaration()) {
                    v.load(0, OBJECT_TYPE);
                }
                else {
                    FieldInfo info = FieldInfo.createForSingleton(receiverDescriptor, typeMapper);
                    v.getstatic(info.getOwnerInternalName(), info.getFieldName(), info.getFieldType().getDescriptor());
                }
                StackValue.onStack(asmType(receiverValue.getType())).put(type, v);
            }
            else {
                StackValue.thisOrOuter(this, receiverDescriptor, false, false).put(type, v);
            }
        }
        else if (receiverValue instanceof ScriptReceiver) {
            // SCRIPT: generate script
            generateScript((ScriptReceiver) receiverValue);
        }
        else if (receiverValue instanceof ExtensionReceiver) {
            generateReceiver(((ExtensionReceiver) receiverValue).getDeclarationDescriptor()).put(type, v);
        }
        else if (receiverValue instanceof ExpressionReceiver) {
            gen(((ExpressionReceiver) receiverValue).getExpression(), type);
        }
        else {
            throw new UnsupportedOperationException("Unsupported receiver value: " + receiverValue);
        }
    }

    @Nullable
    private static JetExpression getReceiverForSelector(PsiElement expression) {
        if (expression.getParent() instanceof JetDotQualifiedExpression && !isReceiver(expression)) {
            JetDotQualifiedExpression parent = (JetDotQualifiedExpression) expression.getParent();
            return parent.getReceiverExpression();
        }
        return null;
    }

    @NotNull
    private StackValue generateReceiver(@NotNull CallableDescriptor descriptor) {
        return context.generateReceiver(descriptor, state, false);
    }

    // SCRIPT: generate script, move to ScriptingUtil
    private void generateScript(@NotNull ScriptReceiver receiver) {
        CodegenContext cur = context;
        StackValue result = StackValue.local(0, OBJECT_TYPE);
        boolean inStartConstructorContext = cur instanceof ConstructorContext;
        while (cur != null) {
            if (!inStartConstructorContext) {
                cur = getNotNullParentContextForMethod(cur);
            }

            if (cur instanceof ScriptContext) {
                ScriptContext scriptContext = (ScriptContext) cur;

                Type currentScriptType = asmTypeForScriptDescriptor(bindingContext, scriptContext.getScriptDescriptor());
                if (scriptContext.getScriptDescriptor() == receiver.getDeclarationDescriptor()) {
                    result.put(currentScriptType, v);
                }
                else {
                    Type classType = asmTypeForScriptDescriptor(bindingContext, receiver.getDeclarationDescriptor());
                    String fieldName = scriptContext.getScriptFieldName(receiver.getDeclarationDescriptor());
                    result.put(currentScriptType, v);
                    StackValue.field(classType, currentScriptType, fieldName, false).put(classType, v);
                }
                return;
            }

            result = cur.getOuterExpression(result, false);

            if (inStartConstructorContext) {
                cur = getNotNullParentContextForMethod(cur);
                inStartConstructorContext = false;
            }

            cur = cur.getParentContext();
        }

        throw new UnsupportedOperationException();
    }

    @NotNull
    public StackValue generateThisOrOuter(@NotNull ClassDescriptor calleeContainingClass, boolean isSuper) {
        boolean isSingleton = calleeContainingClass.getKind().isSingleton();
        if (isSingleton) {
            if (context.hasThisDescriptor() &&
                context.getThisDescriptor().equals(calleeContainingClass) &&
                !AnnotationsPackage.isPlatformStaticInObject(context.getContextDescriptor())) {
                return StackValue.local(0, typeMapper.mapType(calleeContainingClass));
            }
            else {
                return StackValue.singleton(calleeContainingClass, typeMapper);
            }
        }

        CodegenContext cur = context;
        Type type = asmType(calleeContainingClass.getDefaultType());
        StackValue result = StackValue.local(0, type);
        boolean inStartConstructorContext = cur instanceof ConstructorContext;
        while (cur != null) {
            ClassDescriptor thisDescriptor = cur.getThisDescriptor();

            if (!isSuper && thisDescriptor == calleeContainingClass) {
                return result;
            }

            if (isSuper && DescriptorUtils.isSubclass(thisDescriptor, calleeContainingClass)) {
                return castToRequiredTypeOfInterfaceIfNeeded(result, thisDescriptor, calleeContainingClass);
            }

            //for constructor super call we should access to outer instance through parameter in locals, in other cases through field for captured outer
            if (inStartConstructorContext) {
                result = cur.getOuterExpression(result, false);
                cur = getNotNullParentContextForMethod(cur);
                inStartConstructorContext = false;
            }
            else {
                cur = getNotNullParentContextForMethod(cur);
                result = cur.getOuterExpression(result, false);
            }

            cur = cur.getParentContext();
        }

        throw new UnsupportedOperationException();
    }

    @NotNull
    private static CodegenContext getNotNullParentContextForMethod(CodegenContext cur) {
        if (cur instanceof MethodContext) {
            cur = cur.getParentContext();
        }
        assert cur != null;
        return cur;
    }


    private static boolean isReceiver(PsiElement expression) {
        PsiElement parent = expression.getParent();
        if (parent instanceof JetQualifiedExpression) {
            JetExpression receiverExpression = ((JetQualifiedExpression) parent).getReceiverExpression();
            return expression == receiverExpression;
        }
        return false;
    }

    public void genVarargs(@NotNull VarargValueArgument valueArgument, @NotNull JetType outType) {
        Type type = asmType(outType);
        assert type.getSort() == Type.ARRAY;
        Type elementType = correctElementType(type);
        List<ValueArgument> arguments = valueArgument.getArguments();
        int size = arguments.size();

        boolean hasSpread = false;
        for (int i = 0; i != size; ++i) {
            if (arguments.get(i).getSpreadElement() != null) {
                hasSpread = true;
                break;
            }
        }

        if (hasSpread) {
            if (size == 1) {
                gen(arguments.get(0).getArgumentExpression(), type);
            }
            else {
                String owner = "kotlin/jvm/internal/SpreadBuilder";
                v.anew(Type.getObjectType(owner));
                v.dup();
                v.invokespecial(owner, "<init>", "()V", false);
                for (int i = 0; i != size; ++i) {
                    v.dup();
                    ValueArgument argument = arguments.get(i);
                    if (argument.getSpreadElement() != null) {
                        gen(argument.getArgumentExpression(), OBJECT_TYPE);
                        v.invokevirtual(owner, "addSpread", "(Ljava/lang/Object;)V", false);
                    }
                    else {
                        gen(argument.getArgumentExpression(), elementType);
                        v.invokevirtual(owner, "add", "(Ljava/lang/Object;)Z", false);
                        v.pop();
                    }
                }
                v.dup();
                v.invokevirtual(owner, "size", "()I", false);
                v.newarray(elementType);
                v.invokevirtual(owner, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", false);
                v.checkcast(type);
            }
        }
        else {
            v.iconst(arguments.size());
            v.newarray(elementType);
            for (int i = 0; i != size; ++i) {
                v.dup();
                v.iconst(i);
                gen(arguments.get(i).getArgumentExpression(), elementType);
                StackValue.arrayElement(elementType).store(elementType, v);
            }
        }
    }

    public int indexOfLocal(JetReferenceExpression lhs) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, lhs);
        if (isVarCapturedInClosure(bindingContext, declarationDescriptor)) {
            return -1;
        }
        return lookupLocalIndex(declarationDescriptor);
    }

    @Override
    public StackValue visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, StackValue data) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression.getCallableReference(), bindingContext);
        FunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
        if (functionDescriptor != null) {
            CallableReferenceGenerationStrategy strategy = new CallableReferenceGenerationStrategy(state, functionDescriptor, resolvedCall);
            ClosureCodegen closureCodegen = new ClosureCodegen(state, expression, functionDescriptor, null, context,
                                                               KotlinSyntheticClass.Kind.CALLABLE_REFERENCE_WRAPPER,
                                                               this, strategy, getParentCodegen());
            closureCodegen.gen();
            return closureCodegen.putInstanceOnStack(v, this);
        }

        VariableDescriptor variableDescriptor = bindingContext.get(VARIABLE, expression);
        if (variableDescriptor != null) {
            VariableDescriptor descriptor = (VariableDescriptor) resolvedCall.getResultingDescriptor();

            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration instanceof PackageFragmentDescriptor) {
                return generateTopLevelPropertyReference(descriptor);
            }
            else if (containingDeclaration instanceof ClassDescriptor) {
                return generateMemberPropertyReference(descriptor, (ClassDescriptor) containingDeclaration);
            }
            else if (containingDeclaration instanceof ScriptDescriptor) {
                return generateMemberPropertyReference(descriptor, ((ScriptDescriptor) containingDeclaration).getClassDescriptor());
            }
            else {
                throw new UnsupportedOperationException("Unsupported callable reference container: " + containingDeclaration);
            }
        }

        throw new UnsupportedOperationException("Unsupported callable reference expression: " + expression.getText());
    }

    @NotNull
    private StackValue generateTopLevelPropertyReference(@NotNull VariableDescriptor descriptor) {
        PackageFragmentDescriptor containingPackage = (PackageFragmentDescriptor) descriptor.getContainingDeclaration();
        String packageClassInternalName = PackageClassUtils.getPackageClassInternalName(containingPackage.getFqName());

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        Method factoryMethod;
        if (receiverParameter != null) {
            Type[] parameterTypes = new Type[] {JAVA_STRING_TYPE, K_PACKAGE_IMPL_TYPE, getType(Class.class)};
            factoryMethod = descriptor.isVar()
                            ? method("mutableTopLevelExtensionProperty", K_MUTABLE_TOP_LEVEL_EXTENSION_PROPERTY_IMPL_TYPE, parameterTypes)
                            : method("topLevelExtensionProperty", K_TOP_LEVEL_EXTENSION_PROPERTY_IMPL_TYPE, parameterTypes);
        }
        else {
            Type[] parameterTypes = new Type[] {JAVA_STRING_TYPE, K_PACKAGE_IMPL_TYPE};
            factoryMethod = descriptor.isVar()
                            ? method("mutableTopLevelVariable", K_MUTABLE_TOP_LEVEL_VARIABLE_IMPL_TYPE, parameterTypes)
                            : method("topLevelVariable", K_TOP_LEVEL_VARIABLE_IMPL_TYPE, parameterTypes);
        }

        v.visitLdcInsn(descriptor.getName().asString());
        v.getstatic(packageClassInternalName, JvmAbi.KOTLIN_PACKAGE_FIELD_NAME, K_PACKAGE_IMPL_TYPE.getDescriptor());

        if (receiverParameter != null) {
            putJavaLangClassInstance(v, typeMapper.mapType(receiverParameter));
        }

        v.invokestatic(REFLECTION_INTERNAL_PACKAGE, factoryMethod.getName(), factoryMethod.getDescriptor(), false);

        return StackValue.onStack(factoryMethod.getReturnType());
    }

    @NotNull
    private StackValue generateMemberPropertyReference(@NotNull VariableDescriptor descriptor, @NotNull ClassDescriptor containingClass) {
        Type classAsmType = typeMapper.mapClass(containingClass);

        if (containingClass instanceof JavaClassDescriptor) {
            v.aconst(classAsmType);
            v.invokestatic(REFLECTION_INTERNAL_PACKAGE, "foreignKotlinClass",
                           Type.getMethodDescriptor(K_CLASS_IMPL_TYPE, getType(Class.class)), false);
        }
        else {
            v.getstatic(classAsmType.getInternalName(), JvmAbi.KOTLIN_CLASS_FIELD_NAME, K_CLASS_IMPL_TYPE.getDescriptor());
        }

        Method factoryMethod = descriptor.isVar()
                               ? method("mutableMemberProperty", K_MUTABLE_MEMBER_PROPERTY_TYPE, JAVA_STRING_TYPE)
                               : method("memberProperty", K_MEMBER_PROPERTY_TYPE, JAVA_STRING_TYPE);

        v.visitLdcInsn(descriptor.getName().asString());
        v.invokevirtual(K_CLASS_IMPL_TYPE.getInternalName(), factoryMethod.getName(), factoryMethod.getDescriptor(), false);

        return StackValue.onStack(factoryMethod.getReturnType());
    }

    private static class CallableReferenceGenerationStrategy extends FunctionGenerationStrategy.CodegenBased<FunctionDescriptor> {
        private final ResolvedCall<?> resolvedCall;
        private final FunctionDescriptor referencedFunction;

        public CallableReferenceGenerationStrategy(
                @NotNull GenerationState state,
                @NotNull FunctionDescriptor functionDescriptor,
                @NotNull ResolvedCall<?> resolvedCall
        ) {
            super(state, functionDescriptor);
            this.resolvedCall = resolvedCall;
            this.referencedFunction = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            /*
             Here we need to put the arguments from our locals to the stack and invoke the referenced method. Since invocation
             of methods is highly dependent on expressions, we create a fake call expression. Then we create a new instance of
             ExpressionCodegen and, in order for it to generate code correctly, we save to its 'tempVariables' field every
             argument of our fake expression, pointing it to the corresponding index in our locals. This way generation of
             every argument boils down to calling LOAD with the corresponding index
             */

            JetCallExpression fakeExpression = constructFakeFunctionCall();
            final List<? extends ValueArgument> fakeArguments = fakeExpression.getValueArguments();

            final ReceiverValue dispatchReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getDispatchReceiverParameter());
            final ReceiverValue extensionReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getExtensionReceiverParameter());
            computeAndSaveArguments(fakeArguments, codegen);

            ResolvedCall<CallableDescriptor> fakeResolvedCall = new DelegatingResolvedCall<CallableDescriptor>(resolvedCall) {
                @NotNull
                @Override
                public ReceiverValue getExtensionReceiver() {
                    return extensionReceiver;
                }

                @NotNull
                @Override
                public ReceiverValue getDispatchReceiver() {
                    return dispatchReceiver;
                }

                @NotNull
                @Override
                public List<ResolvedValueArgument> getValueArgumentsByIndex() {
                    List<ResolvedValueArgument> result = new ArrayList<ResolvedValueArgument>(fakeArguments.size());
                    for (ValueArgument argument : fakeArguments) {
                        result.add(new ExpressionValueArgument(argument));
                    }
                    return result;
                }
            };

            StackValue result;
            Type returnType = codegen.returnType;
            if (referencedFunction instanceof ConstructorDescriptor) {
                if (returnType.getSort() == Type.ARRAY) {
                    //noinspection ConstantConditions
                    codegen.generateNewArray(fakeExpression, referencedFunction.getReturnType());
                    result = StackValue.onStack(returnType);
                }
                else {
                    result = codegen.generateConstructorCall(fakeResolvedCall, returnType);
                }
            }
            else {
                Call call = CallMaker.makeCall(fakeExpression, NO_RECEIVER, null, fakeExpression, fakeArguments);
                result = codegen.invokeFunction(call, fakeResolvedCall, StackValue.none());
            }

            InstructionAdapter v = codegen.v;
            result.put(returnType, v);
            v.areturn(returnType);
        }

        @NotNull
        private JetCallExpression constructFakeFunctionCall() {
            StringBuilder fakeFunctionCall = new StringBuilder("callableReferenceFakeCall(");
            for (Iterator<ValueParameterDescriptor> iterator = referencedFunction.getValueParameters().iterator(); iterator.hasNext(); ) {
                ValueParameterDescriptor descriptor = iterator.next();
                fakeFunctionCall.append("p").append(descriptor.getIndex());
                if (iterator.hasNext()) {
                    fakeFunctionCall.append(", ");
                }
            }
            fakeFunctionCall.append(")");
            return (JetCallExpression) JetPsiFactory(state.getProject()).createExpression(fakeFunctionCall.toString());
        }

        private void computeAndSaveArguments(@NotNull List<? extends ValueArgument> fakeArguments, @NotNull ExpressionCodegen codegen) {
            for (ValueParameterDescriptor parameter : callableDescriptor.getValueParameters()) {
                ValueArgument fakeArgument = fakeArguments.get(parameter.getIndex());
                Type type = state.getTypeMapper().mapType(parameter);
                int localIndex = codegen.myFrameMap.getIndex(parameter);
                codegen.tempVariables.put(fakeArgument.getArgumentExpression(), StackValue.local(localIndex, type));
            }
        }

        @NotNull
        private ReceiverValue computeAndSaveReceiver(
                @NotNull JvmMethodSignature signature,
                @NotNull ExpressionCodegen codegen,
                @Nullable ReceiverParameterDescriptor receiver
        ) {
            if (receiver == null) return NO_RECEIVER;

            JetExpression receiverExpression = JetPsiFactory(state.getProject()).createExpression("callableReferenceFakeReceiver");
            codegen.tempVariables.put(receiverExpression, receiverParameterStackValue(signature));
            return new ExpressionReceiver(receiverExpression, receiver.getType());
        }

        @NotNull
        private static StackValue.Local receiverParameterStackValue(@NotNull JvmMethodSignature signature) {
            // 0 is this (the callable reference class), 1 is the invoke() method's first parameter
            return StackValue.local(1, signature.getAsmMethod().getArgumentTypes()[0]);
        }
    }

    @Override
    public StackValue visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression, StackValue receiver) {
        StackValue receiverValue = StackValue.none();
        return genQualified(receiverValue, expression.getSelectorExpression());
    }

    private void generateExpressionWithNullFallback(@NotNull JetExpression expression, @NotNull Label ifnull) {
        expression = JetPsiUtil.deparenthesize(expression);
        Type type = expressionType(expression);

        if (expression instanceof JetSafeQualifiedExpression && !isPrimitive(type)) {
            StackValue value = generateSafeQualifiedExpression((JetSafeQualifiedExpression) expression, ifnull);
            value.put(type, v);
        }
        else {
            gen(expression, type);
        }
    }

    private StackValue generateSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression, @NotNull Label ifnull) {
        JetExpression receiver = expression.getReceiverExpression();
        JetExpression selector = expression.getSelectorExpression();
        Type receiverType = expressionType(receiver);

        generateExpressionWithNullFallback(receiver, ifnull);

        if (isPrimitive(receiverType)) {
            return genQualified(StackValue.onStack(receiverType), selector);
        }

        v.dup();
        v.ifnull(ifnull);
        return genQualified(StackValue.onStack(receiverType), selector);
    }

    @Override
    public StackValue visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression, StackValue unused) {
        Label ifnull = new Label();
        Type type = boxType(expressionType(expression));

        StackValue value = generateSafeQualifiedExpression(expression, ifnull);
        value.put(type, v);

        if (!isPrimitive(expressionType(expression.getReceiverExpression()))) {
            Label end = new Label();

            v.goTo(end);
            v.mark(ifnull);
            v.pop();
            if (!type.equals(Type.VOID_TYPE)) {
                v.aconst(null);
            }
            v.mark(end);
        }

        return StackValue.onStack(type);
    }

    @Override
    public StackValue visitBinaryExpression(@NotNull JetBinaryExpression expression, StackValue receiver) {
        JetSimpleNameExpression reference = expression.getOperationReference();
        IElementType opToken = reference.getReferencedNameElementType();
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
            return generateComparison(expression, receiver);
        }
        else if (opToken == JetTokens.ELVIS) {
            return generateElvis(expression);
        }
        else if (opToken == JetTokens.IN_KEYWORD || opToken == JetTokens.NOT_IN) {
            return generateIn(StackValue.expression(Type.INT_TYPE, expression.getLeft(), this), expression.getRight(), reference);
        }
        else {
            ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
            FunctionDescriptor descriptor = (FunctionDescriptor) resolvedCall.getResultingDescriptor();

            Callable callable = resolveToCallable(descriptor, false);
            if (callable instanceof IntrinsicMethod) {
                Type returnType = typeMapper.mapType(descriptor);
                ((IntrinsicMethod) callable).generate(this, v, returnType, expression,
                                                      Arrays.asList(expression.getLeft(), expression.getRight()), receiver);
                return StackValue.onStack(returnType);
            }

            return invokeFunction(resolvedCall, receiver);
        }
    }

    private StackValue generateIn(StackValue leftValue, JetExpression rangeExpression, JetSimpleNameExpression operationReference) {
        JetExpression deparenthesized = JetPsiUtil.deparenthesize(rangeExpression);
        if (isIntRangeExpr(deparenthesized)) {
            genInIntRange(leftValue, (JetBinaryExpression) deparenthesized);
        }
        else {
            ResolvedCall<? extends CallableDescriptor> resolvedCall = getResolvedCallWithAssert(operationReference, bindingContext);
            invokeFunction(resolvedCall, StackValue.none());
        }
        if (operationReference.getReferencedNameElementType() == JetTokens.NOT_IN) {
            genInvertBoolean(v);
        }
        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private void genInIntRange(StackValue leftValue, JetBinaryExpression rangeExpression) {
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
        Type leftType = expressionType(left);
        Type rightType = expressionType(right);

        if (JetPsiUtil.isNullConstant(left)) {
            return genCmpWithNull(right, rightType, opToken);
        }

        if (JetPsiUtil.isNullConstant(right)) {
            return genCmpWithNull(left, leftType, opToken);
        }

        if (isIntZero(left, leftType) && isIntPrimitive(rightType)) {
            return genCmpWithZero(right, rightType, opToken);
        }

        if (isIntZero(right, rightType) && isIntPrimitive(leftType)) {
            return genCmpWithZero(left, leftType, opToken);
        }

        if (isPrimitive(leftType) != isPrimitive(rightType)) {
            leftType = boxType(leftType);
            gen(left, leftType);
            rightType = boxType(rightType);
            gen(right, rightType);
        }
        else {
            gen(left, leftType);
            gen(right, rightType);
        }

        return genEqualsForExpressionsOnStack(v, opToken, leftType, rightType);
    }

    private boolean isIntZero(JetExpression expr, Type exprType) {
        CompileTimeConstant<?> exprValue = getCompileTimeConstant(expr, bindingContext);
        return isIntPrimitive(exprType) && exprValue != null && Integer.valueOf(0).equals(exprValue.getValue());
    }

    private StackValue genCmpWithZero(JetExpression exp, Type expType, IElementType opToken) {
        gen(exp, expType);
        Label trueLabel = new Label();
        Label afterLabel = new Label();
        if (JetTokens.EQEQ == opToken || JetTokens.EQEQEQ == opToken) {
            v.ifeq(trueLabel);
        }
        else {
            v.ifne(trueLabel);
        }

        v.iconst(0);
        v.goTo(afterLabel);

        v.mark(trueLabel);
        v.iconst(1);

        v.mark(afterLabel);

        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue genCmpWithNull(JetExpression exp, Type expType, IElementType opToken) {
        gen(exp, boxType(expType));
        Label trueLabel = new Label();
        Label afterLabel = new Label();
        if (JetTokens.EQEQ == opToken || JetTokens.EQEQEQ == opToken) {
            v.ifnull(trueLabel);
        }
        else {
            v.ifnonnull(trueLabel);
        }

        v.iconst(0);
        v.goTo(afterLabel);

        v.mark(trueLabel);
        v.iconst(1);

        v.mark(afterLabel);

        return StackValue.onStack(Type.BOOLEAN_TYPE);
    }

    private StackValue generateElvis(JetBinaryExpression expression) {
        JetExpression left = expression.getLeft();

        Type exprType = expressionType(expression);
        Type leftType = expressionType(left);

        Label ifNull = new Label();


        assert left != null : "left expression in elvis should be not null: " + expression.getText();
        generateExpressionWithNullFallback(left, ifNull);

        if (isPrimitive(leftType)) {
            return StackValue.onStack(leftType);
        }

        v.dup();

        v.ifnull(ifNull);
        StackValue.onStack(leftType).put(exprType, v);

        Label end = new Label();
        v.goTo(end);

        v.mark(ifNull);
        v.pop();
        gen(expression.getRight(), exprType);
        v.mark(end);

        return StackValue.onStack(exprType);
    }

    private StackValue generateComparison(JetBinaryExpression expression, StackValue receiver) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
        FunctionDescriptor descriptor = (FunctionDescriptor) resolvedCall.getResultingDescriptor();

        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();
        Callable callable = resolveToCallable(descriptor, false);

        Type type;
        if (callable instanceof IntrinsicMethod) {
            // Compare two primitive values
            type = comparisonOperandType(expressionType(left), expressionType(right));
            StackValue recv = gen(left);
            recv.put(type, v);
            gen(right, type);
        }
        else {
            StackValue result = invokeFunction(resolvedCall, receiver);
            type = Type.INT_TYPE;
            result.put(type, v);
            v.iconst(0);
        }
        return StackValue.cmp(expression.getOperationToken(), type);
    }

    private StackValue generateAssignmentExpression(JetBinaryExpression expression) {
        StackValue stackValue = gen(expression.getLeft());
        JetExpression right = expression.getRight();
        assert right != null : expression.getText();
        gen(right, stackValue.type);
        stackValue.store(stackValue.type, v);
        return StackValue.none();
    }

    private StackValue generateAugmentedAssignment(JetBinaryExpression expression) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
        FunctionDescriptor descriptor = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        Callable callable = resolveToCallable(descriptor, false);
        JetExpression lhs = expression.getLeft();
        Type lhsType = expressionType(lhs);

        boolean keepReturnValue;
        if (Boolean.TRUE.equals(bindingContext.get(VARIABLE_REASSIGNMENT, expression))) {
            if (callable instanceof IntrinsicMethod) {
                StackValue value = gen(lhs);              // receiver
                value.dupReceiver(v);                     // receiver receiver
                value.put(lhsType, v);                    // receiver lhs
                Type returnType = typeMapper.mapType(descriptor);
                ((IntrinsicMethod) callable).generate(this, v, returnType, expression,
                                                      Collections.singletonList(expression.getRight()), StackValue.onStack(lhsType));
                value.store(returnType, v);
                return StackValue.none();
            }
            else {
                keepReturnValue = true;
            }
        }
        else {
            keepReturnValue = !KotlinBuiltIns.getInstance().getUnitType().equals(descriptor.getReturnType());
        }

        callAugAssignMethod(expression, resolvedCall, (CallableMethod) callable, lhsType, keepReturnValue);

        return StackValue.none();
    }

    private void callAugAssignMethod(
            @NotNull JetBinaryExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull CallableMethod callable,
            @NotNull Type lhsType,
            boolean keepReturnValue
    ) {
        StackValue value = gen(expression.getLeft());
        if (keepReturnValue) {
            value.dupReceiver(v);
        }
        value.put(lhsType, v);
        StackValue receiver = StackValue.onStack(lhsType);

        invokeMethodWithArguments(callable, resolvedCall, receiver);

        if (keepReturnValue) {
            value.store(callable.getReturnType(), v);
        }
    }

    public void invokeAppend(JetExpression expr) {
        if (expr instanceof JetBinaryExpression) {
            JetBinaryExpression binaryExpression = (JetBinaryExpression) expr;
            if (binaryExpression.getOperationToken() == JetTokens.PLUS) {
                JetExpression left = binaryExpression.getLeft();
                JetExpression right = binaryExpression.getRight();
                Type leftType = expressionType(left);

                if (leftType.equals(JAVA_STRING_TYPE)) {
                    invokeAppend(left);
                    invokeAppend(right);
                    return;
                }
            }
        }
        Type exprType = expressionType(expr);
        gen(expr, exprType);
        genInvokeAppendMethod(v, exprType.getSort() == Type.ARRAY ? OBJECT_TYPE : exprType);
    }

    @Nullable
    private static JetSimpleNameExpression targetLabel(JetExpression expression) {
        if (expression.getParent() instanceof JetLabeledExpression) {
            return ((JetLabeledExpression) expression.getParent()).getTargetLabel();
        }
        return null;
    }

    @Override
    public StackValue visitLabeledExpression(
            @NotNull JetLabeledExpression expression, StackValue receiver
    ) {
        return genQualified(receiver, expression.getBaseExpression());
    }

    @Override
    public StackValue visitPrefixExpression(@NotNull JetPrefixExpression expression, StackValue receiver) {
        DeclarationDescriptor originalOperation = bindingContext.get(REFERENCE_TARGET, expression.getOperationReference());
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
        CallableDescriptor op = resolvedCall.getResultingDescriptor();

        assert op instanceof FunctionDescriptor || originalOperation == null : String.valueOf(op);
        Callable callable = resolveToCallable((FunctionDescriptor) op, false);
        if (callable instanceof IntrinsicMethod) {
            Type returnType = typeMapper.mapType(op);
            ((IntrinsicMethod) callable).generate(this, v, returnType, expression,
                                                  Collections.singletonList(expression.getBaseExpression()), receiver);
            return StackValue.onStack(returnType);
        }

        DeclarationDescriptor cls = op.getContainingDeclaration();

        if (isPrimitiveNumberClassDescriptor(cls) || !(originalOperation.getName().asString().equals("inc") || originalOperation.getName().asString().equals("dec"))) {
            return invokeFunction(resolvedCall, receiver);
        }


        StackValue value = gen(expression.getBaseExpression());
        value.dupReceiver(v);
        value.dupReceiver(v);

        Type type = expressionType(expression.getBaseExpression());
        value.put(type, v);
        StackValue result = invokeFunction(resolvedCall, StackValue.onStack(type));
        value.store(result.type, v);
        value.put(type, v);
        return StackValue.onStack(type);
    }

    @Override
    public StackValue visitPostfixExpression(@NotNull JetPostfixExpression expression, StackValue receiver) {
        if (expression.getOperationReference().getReferencedNameElementType() == JetTokens.EXCLEXCL) {
            StackValue base = genQualified(receiver, expression.getBaseExpression());
            if (isPrimitive(base.type)) {
                return base;
            }
            base.put(base.type, v);
            v.dup();
            Label ok = new Label();
            v.ifnonnull(ok);
            v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "throwNpe", "()V", false);
            v.mark(ok);
            return StackValue.onStack(base.type);
        }

        DeclarationDescriptor originalOperation = bindingContext.get(REFERENCE_TARGET, expression.getOperationReference());
        String originalOperationName = originalOperation != null ? originalOperation.getName().asString() : null;
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(expression, bindingContext);
        DeclarationDescriptor op = resolvedCall.getResultingDescriptor();
        if (!(op instanceof FunctionDescriptor) || originalOperation == null) {
            throw new UnsupportedOperationException("Don't know how to generate this postfix expression: " + originalOperationName + " " + op);
        }

        Type asmResultType = expressionType(expression);
        Type asmBaseType = expressionType(expression.getBaseExpression());
        DeclarationDescriptor cls = op.getContainingDeclaration();

        int increment;
        if (originalOperationName.equals("inc")) {
            increment = 1;
        }
        else if (originalOperationName.equals("dec")) {
            increment = -1;
        }
        else {
            throw new UnsupportedOperationException("Unsupported postfix operation: " + originalOperationName + " " + op);
        }

        boolean isPrimitiveNumberClassDescriptor = isPrimitiveNumberClassDescriptor(cls);
        if (isPrimitiveNumberClassDescriptor && AsmUtil.isPrimitive(asmBaseType)) {
            JetExpression operand = expression.getBaseExpression();
            if (operand instanceof JetReferenceExpression && asmResultType == Type.INT_TYPE) {
                int index = indexOfLocal((JetReferenceExpression) operand);
                if (index >= 0) {
                    return StackValue.postIncrement(index, increment);
                }
            }
        }

        StackValue value = gen(expression.getBaseExpression());
        value.dupReceiver(v);

        Type type = expressionType(expression.getBaseExpression());
        value.put(type, v); // old value

        pushReceiverAndValueViaDup(value, type); // receiver and new value

        Type storeType;
        if (isPrimitiveNumberClassDescriptor && AsmUtil.isPrimitive(asmBaseType)) {
            genIncrement(asmResultType, increment, v);
            storeType = type;
        }
        else {
            StackValue result = invokeFunction(resolvedCall, StackValue.onStack(type));
            storeType = result.type;
        }

        value.store(storeType, v);
        return StackValue.onStack(asmResultType);  // old value
    }

    private void pushReceiverAndValueViaDup(StackValue value, Type type) {
        switch (value.receiverSize()) {
            case 0:
                dup(v, type);
                break;

            case 1:
                if (type.getSize() == 2) {
                    v.dup2X1();
                }
                else {
                    v.dupX1();
                }
                break;

            case 2:
                if (type.getSize() == 2) {
                    v.dup2X2();
                }
                else {
                    v.dupX2();
                }
                break;

            case -1:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public StackValue visitProperty(@NotNull JetProperty property, StackValue receiver) {
        final JetExpression initializer = property.getInitializer();
        if (initializer == null) {
            return StackValue.none();
        }
        initializeLocalVariable(property, new Function<VariableDescriptor, Void>() {
            @Override
            public Void fun(VariableDescriptor descriptor) {
                Type varType = asmType(descriptor.getType());
                gen(initializer, varType);
                return null;
            }
        });
        return StackValue.none();
    }

    @Override
    public StackValue visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, StackValue receiver) {
        JetExpression initializer = multiDeclaration.getInitializer();
        if (initializer == null) return StackValue.none();

        JetType initializerType = bindingContext.get(EXPRESSION_TYPE, initializer);
        assert initializerType != null;

        Type initializerAsmType = asmType(initializerType);

        final TransientReceiver initializerAsReceiver = new TransientReceiver(initializerType);

        int tempVarIndex = myFrameMap.enterTemp(initializerAsmType);

        gen(initializer, initializerAsmType);
        v.store(tempVarIndex, initializerAsmType);
        final StackValue.Local local = StackValue.local(tempVarIndex, initializerAsmType);

        for (final JetMultiDeclarationEntry variableDeclaration : multiDeclaration.getEntries()) {
            initializeLocalVariable(variableDeclaration, new Function<VariableDescriptor, Void>() {
                @Override
                public Void fun(VariableDescriptor descriptor) {
                    ResolvedCall<FunctionDescriptor> resolvedCall = bindingContext.get(COMPONENT_RESOLVED_CALL, variableDeclaration);
                    assert resolvedCall != null : "Resolved call is null for " + variableDeclaration.getText();
                    Call call = makeFakeCall(initializerAsReceiver);
                    invokeFunction(call, resolvedCall, local);
                    return null;
                }
            });
        }

        if (initializerAsmType.getSort() == Type.OBJECT || initializerAsmType.getSort() == Type.ARRAY) {
            v.aconst(null);
            v.store(tempVarIndex, initializerAsmType);
        }
        myFrameMap.leaveTemp(initializerAsmType);

        return StackValue.none();
    }

    private void initializeLocalVariable(
            @NotNull JetVariableDeclaration variableDeclaration,
            @NotNull Function<VariableDescriptor, Void> generateInitializer
    ) {
        VariableDescriptor variableDescriptor = bindingContext.get(VARIABLE, variableDeclaration);

        if (JetPsiUtil.isScriptDeclaration(variableDeclaration)) {
            return;
        }
        int index = lookupLocalIndex(variableDescriptor);

        if (index < 0) {
            throw new IllegalStateException("Local variable not found for " + variableDescriptor);
        }

        Type sharedVarType = typeMapper.getSharedVarType(variableDescriptor);
        assert variableDescriptor != null;

        Type varType = asmType(variableDescriptor.getType());

        // SCRIPT: Variable at the top of the script is generated as field
        if (JetPsiUtil.isScriptDeclaration(variableDeclaration)) {
            generateInitializer.fun(variableDescriptor);
            JetScript scriptPsi = JetPsiUtil.getScript(variableDeclaration);
            assert scriptPsi != null;
            Type scriptClassType = asmTypeForScriptPsi(bindingContext, scriptPsi);
            v.putfield(scriptClassType.getInternalName(), variableDeclaration.getName(), varType.getDescriptor());
        }
        else if (sharedVarType == null) {
            generateInitializer.fun(variableDescriptor);
            v.store(index, varType);
        }
        else {
            v.load(index, OBJECT_TYPE);
            generateInitializer.fun(variableDescriptor);
            v.putfield(sharedVarType.getInternalName(), "element",
                       sharedVarType.equals(OBJECT_REF_TYPE) ? "Ljava/lang/Object;" : varType.getDescriptor());
        }
    }

    @NotNull
    private StackValue generateNewCall(@NotNull JetCallExpression expression, @NotNull ResolvedCall<?> resolvedCall) {
        Type type = expressionType(expression);
        if (type.getSort() == Type.ARRAY) {
            generateNewArray(expression);
            return StackValue.onStack(type);
        }

        return generateConstructorCall(resolvedCall, type);
    }

    @NotNull
    private StackValue generateConstructorCall(@NotNull ResolvedCall<?> resolvedCall, @NotNull Type type) {
        v.anew(type);
        v.dup();

        ConstructorDescriptor constructor = (ConstructorDescriptor) resolvedCall.getResultingDescriptor();

        ReceiverParameterDescriptor dispatchReceiver = constructor.getDispatchReceiverParameter();
        if (dispatchReceiver != null) {
            Type receiverType = typeMapper.mapType(dispatchReceiver.getType());
            generateReceiverValue(resolvedCall.getDispatchReceiver(), receiverType);
        }

        // Resolved call to local class constructor doesn't have dispatchReceiver, so we need to generate closure on stack
        // See StackValue.receiver for more info
        pushClosureOnStack(constructor.getContainingDeclaration(), dispatchReceiver == null, defaultCallGenerator);

        ConstructorDescriptor originalOfSamAdapter = (ConstructorDescriptor) SamCodegenUtil.getOriginalIfSamAdapter(constructor);
        CallableMethod method = typeMapper.mapToCallableMethod(originalOfSamAdapter == null ? constructor : originalOfSamAdapter);
        invokeMethodWithArguments(method, resolvedCall, StackValue.none());

        return StackValue.onStack(type);
    }

    public void generateNewArray(@NotNull JetCallExpression expression) {
        JetType arrayType = bindingContext.get(EXPRESSION_TYPE, expression);
        assert arrayType != null : "Array instantiation isn't type checked: " + expression.getText();

        generateNewArray(expression, arrayType);
    }

    private void generateNewArray(@NotNull JetCallExpression expression, @NotNull JetType arrayType) {
        List<JetExpression> args = new ArrayList<JetExpression>();
        for (ValueArgument va : expression.getValueArguments()) {
            args.add(va.getArgumentExpression());
        }

        boolean isArray = KotlinBuiltIns.getInstance().isArray(arrayType);
        if (!isArray && args.size() != 1) {
            throw new CompilationException("primitive array constructor requires one argument", null, expression);
        }

        if (isArray) {
            gen(args.get(0), Type.INT_TYPE);
            TypeParameterDescriptor parameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(
                    arrayType.getArguments().get(0).getType()
            );
            if (parameterDescriptor != null && parameterDescriptor.isReified()) {
                v.iconst(parameterDescriptor.getIndex());
                v.invokestatic(
                        IntrinsicMethods.INTRINSICS_CLASS_NAME, ReifiedTypeInliner.NEW_ARRAY_MARKER_METHOD_NAME,
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false
                );
            }
            v.newarray(boxType(asmType(arrayType.getArguments().get(0).getType())));
        }
        else {
            Type type = typeMapper.mapType(arrayType);
            gen(args.get(0), Type.INT_TYPE);
            v.newarray(correctElementType(type));
        }

        if (args.size() == 2) {
            int sizeIndex = myFrameMap.enterTemp(Type.INT_TYPE);
            int indexIndex = myFrameMap.enterTemp(Type.INT_TYPE);

            v.dup();
            v.arraylength();
            v.store(sizeIndex, Type.INT_TYPE);

            v.iconst(0);
            v.store(indexIndex, Type.INT_TYPE);

            gen(args.get(1), FUNCTION1_TYPE);

            Label begin = new Label();
            Label end = new Label();
            v.visitLabel(begin);
            v.load(indexIndex, Type.INT_TYPE);
            v.load(sizeIndex, Type.INT_TYPE);
            v.ificmpge(end);

            v.dup2();
            v.load(indexIndex, Type.INT_TYPE);
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            v.invokeinterface(FUNCTION1_TYPE.getInternalName(), "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
            v.load(indexIndex, Type.INT_TYPE);
            v.iinc(indexIndex, 1);
            v.swap();
            v.astore(OBJECT_TYPE);

            v.goTo(begin);
            v.visitLabel(end);
            v.pop();

            myFrameMap.leaveTemp(Type.INT_TYPE);
            myFrameMap.leaveTemp(Type.INT_TYPE);
        }
    }

    @Override
    public StackValue visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, StackValue receiver) {
        JetExpression array = expression.getArrayExpression();
        JetType type = bindingContext.get(EXPRESSION_TYPE, array);
        Type arrayType = expressionType(array);
        List<JetExpression> indices = expression.getIndexExpressions();
        FunctionDescriptor operationDescriptor = (FunctionDescriptor) bindingContext.get(REFERENCE_TARGET, expression);
        assert operationDescriptor != null;
        if (arrayType.getSort() == Type.ARRAY &&
            indices.size() == 1 &&
            operationDescriptor.getValueParameters().get(0).getType().equals(KotlinBuiltIns.getInstance().getIntType())) {
            gen(array, arrayType);
            for (JetExpression index : indices) {
                gen(index, Type.INT_TYPE);
            }
            assert type != null;
            if (KotlinBuiltIns.getInstance().isArray(type)) {
                JetType elementType = type.getArguments().get(0).getType();
                return StackValue.arrayElement(boxType(asmType(elementType)));
            }
            else {
                return StackValue.arrayElement(correctElementType(arrayType));
            }
        }
        else {
            ResolvedCall<FunctionDescriptor> resolvedSetCall = bindingContext.get(INDEXED_LVALUE_SET, expression);
            ResolvedCall<FunctionDescriptor> resolvedGetCall = bindingContext.get(INDEXED_LVALUE_GET, expression);

            boolean isGetter = "get".equals(operationDescriptor.getName().asString());

            ResolvedCall<FunctionDescriptor> resolvedCall = isGetter ? resolvedGetCall : resolvedSetCall;
            assert resolvedCall != null : "couldn't find resolved call: " + expression.getText();

            Callable callable = resolveToCallable(operationDescriptor, false);
            Method asmMethod = resolveToCallableMethod(operationDescriptor, false, context).getAsmMethod();
            Type[] argumentTypes = asmMethod.getArgumentTypes();

            if (callable instanceof CallableMethod) {
                CallableMethod callableMethod = (CallableMethod) callable;
                ArgumentGenerator argumentGenerator =
                        new CallBasedArgumentGenerator(this, defaultCallGenerator,
                                                       resolvedCall.getResultingDescriptor().getValueParameters(),
                                                       callableMethod.getValueParameterTypes());

                receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
                receiver.put(receiver.type, v);

                List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
                assert valueArguments != null : "Failed to arrange value arguments by index: " + operationDescriptor;

                if (!isGetter) {
                    assert valueArguments.size() >= 2 : "Setter call should have at least 2 arguments: " + operationDescriptor;

                    // Skip generation of the right hand side of an indexed assignment, which is the last value argument
                    valueArguments.remove(valueArguments.size() - 1);
                }

                argumentGenerator.generate(valueArguments);
            }
            else {
                gen(array, arrayType); // intrinsic method

                int index = operationDescriptor.getExtensionReceiverParameter() != null ? 1 : 0;

                for (JetExpression jetExpression : expression.getIndexExpressions()) {
                    gen(jetExpression, argumentTypes[index]);
                    index++;
                }
            }

            Type elementType = isGetter ? asmMethod.getReturnType() : ArrayUtil.getLastElement(argumentTypes);
            return StackValue.collectionElement(elementType, resolvedGetCall, resolvedSetCall, this, state);
        }
    }

    @Override
    public StackValue visitThrowExpression(@NotNull JetThrowExpression expression, StackValue receiver) {
        gen(expression.getThrownExpression(), JAVA_THROWABLE_TYPE);
        v.athrow();
        return StackValue.none();
    }

    @Override
    public StackValue visitThisExpression(@NotNull JetThisExpression expression, StackValue receiver) {
        DeclarationDescriptor descriptor = bindingContext.get(REFERENCE_TARGET, expression.getInstanceReference());
        if (descriptor instanceof ClassDescriptor) {
            //TODO rewrite with context.lookupInContext()
            return StackValue.thisOrOuter(this, (ClassDescriptor) descriptor, false, true);
        }
        if (descriptor instanceof CallableDescriptor) {
            return generateReceiver((CallableDescriptor) descriptor);
        }
        throw new UnsupportedOperationException("Neither this nor receiver: " + descriptor);
    }

    @Override
    public StackValue visitTryExpression(@NotNull JetTryExpression expression, StackValue receiver) {
        return generateTryExpression(expression, false);
    }

    public StackValue generateTryExpression(JetTryExpression expression, boolean isStatement) {
        /*
The "returned" value of try expression with no finally is either the last expression in the try block or the last expression in the catch block
(or blocks).
         */
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        FinallyBlockStackElement finallyBlockStackElement = null;
        if (finallyBlock != null) {
            finallyBlockStackElement = new FinallyBlockStackElement(expression);
            blockStackElements.push(finallyBlockStackElement);
        }

        JetType jetType = bindingContext.get(EXPRESSION_TYPE, expression);
        assert jetType != null;
        Type expectedAsmType = isStatement ? Type.VOID_TYPE : asmType(jetType);

        Label tryStart = new Label();
        v.mark(tryStart);
        v.nop(); // prevent verify error on empty try

        gen(expression.getTryBlock(), expectedAsmType);

        int savedValue = -1;
        if (!isStatement) {
            savedValue = myFrameMap.enterTemp(expectedAsmType);
            v.store(savedValue, expectedAsmType);
        }

        Label tryEnd = new Label();
        v.mark(tryEnd);

        //do it before finally block generation
        List<Label> tryBlockRegions = getCurrentCatchIntervals(finallyBlockStackElement, tryStart, tryEnd);

        Label end = new Label();

        genFinallyBlockOrGoto(finallyBlockStackElement, end);

        List<JetCatchClause> clauses = expression.getCatchClauses();
        for (int i = 0, size = clauses.size(); i < size; i++) {
            JetCatchClause clause = clauses.get(i);

            Label clauseStart = new Label();
            v.mark(clauseStart);

            VariableDescriptor descriptor = bindingContext.get(VALUE_PARAMETER, clause.getCatchParameter());
            assert descriptor != null;
            Type descriptorType = asmType(descriptor.getType());
            myFrameMap.enter(descriptor, descriptorType);
            int index = lookupLocalIndex(descriptor);
            v.store(index, descriptorType);

            gen(clause.getCatchBody(), expectedAsmType);

            if (!isStatement) {
                v.store(savedValue, expectedAsmType);
            }

            myFrameMap.leave(descriptor);

            Label clauseEnd = new Label();
            v.mark(clauseEnd);

            v.visitLocalVariable(descriptor.getName().asString(), descriptorType.getDescriptor(), null, clauseStart, clauseEnd, index);

            genFinallyBlockOrGoto(finallyBlockStackElement, i != size - 1 || finallyBlock != null ? end : null);

            generateExceptionTable(clauseStart, tryBlockRegions, descriptorType.getInternalName());
        }


        //for default catch clause
        if (finallyBlock != null) {
            Label defaultCatchStart = new Label();
            v.mark(defaultCatchStart);
            int savedException = myFrameMap.enterTemp(JAVA_THROWABLE_TYPE);
            v.store(savedException, JAVA_THROWABLE_TYPE);
            Label defaultCatchEnd = new Label();
            v.mark(defaultCatchEnd);

            //do it before finally block generation
            //javac also generates entry in exception table for default catch clause too!!!! so defaultCatchEnd as end parameter
            List<Label> defaultCatchRegions = getCurrentCatchIntervals(finallyBlockStackElement, tryStart, defaultCatchEnd);


            genFinallyBlockOrGoto(finallyBlockStackElement, null);

            v.load(savedException, JAVA_THROWABLE_TYPE);
            myFrameMap.leaveTemp(JAVA_THROWABLE_TYPE);

            v.athrow();

            generateExceptionTable(defaultCatchStart, defaultCatchRegions, null);
        }

        markLineNumber(expression, isStatement);
        v.mark(end);

        if (!isStatement) {
            v.load(savedValue, expectedAsmType);
            myFrameMap.leaveTemp(expectedAsmType);
        }

        if (finallyBlock != null) {
            blockStackElements.pop();
        }

        return StackValue.onStack(expectedAsmType);
    }

    private void generateExceptionTable(@NotNull Label catchStart, @NotNull List<Label> catchedRegions, @Nullable String exception) {
        for (int i = 0; i < catchedRegions.size(); i += 2) {
            Label startRegion = catchedRegions.get(i);
            Label endRegion = catchedRegions.get(i+1);
            v.visitTryCatchBlock(startRegion, endRegion, catchStart, exception);
        }
    }

    @NotNull
    private static List<Label> getCurrentCatchIntervals(
            @Nullable FinallyBlockStackElement finallyBlockStackElement,
            @NotNull Label blockStart,
            @NotNull Label blockEnd
    ) {
        List<Label> gapsInBlock =
                finallyBlockStackElement != null ? new ArrayList<Label>(finallyBlockStackElement.gaps) : Collections.<Label>emptyList();
        assert gapsInBlock.size() % 2 == 0;
        List<Label> blockRegions = new ArrayList<Label>(gapsInBlock.size() + 2);
        blockRegions.add(blockStart);
        blockRegions.addAll(gapsInBlock);
        blockRegions.add(blockEnd);
        return blockRegions;
    }

    @Override
    public StackValue visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, StackValue receiver) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType opToken = operationSign.getReferencedNameElementType();
        if (opToken == JetTokens.COLON) {
            return gen(expression.getLeft());
        }
        else {
            JetTypeReference typeReference = expression.getRight();
            JetType rightType = bindingContext.get(TYPE, typeReference);
            assert rightType != null;
            Type rightTypeAsm = boxType(asmType(rightType));
            JetExpression left = expression.getLeft();
            DeclarationDescriptor descriptor = rightType.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor || descriptor instanceof TypeParameterDescriptor) {
                StackValue value = genQualified(receiver, left);
                value.put(boxType(value.type), v);

                if (opToken != JetTokens.AS_SAFE) {
                    if (!TypeUtils.isNullableType(rightType)) {
                        v.dup();
                        Label nonnull = new Label();
                        v.ifnonnull(nonnull);
                        JetType leftType = bindingContext.get(EXPRESSION_TYPE, left);
                        assert leftType != null;
                        genThrow(v, "kotlin/TypeCastException", DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(leftType) +
                                                                " cannot be cast to " +
                                                                DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(rightType));
                        v.mark(nonnull);
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
                throw new UnsupportedOperationException("Don't know how to handle non-class types in as/as? : " + descriptor);
            }
        }
    }

    @Override
    public StackValue visitIsExpression(@NotNull JetIsExpression expression, StackValue receiver) {
        StackValue match = StackValue.expression(OBJECT_TYPE, expression.getLeftHandSide(), this);
        return generateIsCheck(match, expression.getTypeReference(), expression.isNegated());
    }

    private StackValue generateExpressionMatch(StackValue expressionToMatch, JetExpression patternExpression) {
        if (expressionToMatch != null) {
            Type subjectType = expressionToMatch.type;
            markStartLineNumber(patternExpression);
            expressionToMatch.put(subjectType, v);
            JetType condJetType = bindingContext.get(EXPRESSION_TYPE, patternExpression);
            Type condType;
            if (isNumberPrimitive(subjectType) || subjectType.getSort() == Type.BOOLEAN) {
                assert condJetType != null;
                condType = asmType(condJetType);
                if (!(isNumberPrimitive(condType) || condType.getSort() == Type.BOOLEAN)) {
                    subjectType = boxType(subjectType);
                    expressionToMatch.coerceTo(subjectType, v);
                }
            }
            else {
                condType = OBJECT_TYPE;
            }
            gen(patternExpression, condType);
            return genEqualsForExpressionsOnStack(v, JetTokens.EQEQ, subjectType, condType);
        }
        else {
            return gen(patternExpression);
        }
    }

    private StackValue generateIsCheck(StackValue expressionToMatch, JetTypeReference typeReference, boolean negated) {
        JetType jetType = bindingContext.get(TYPE, typeReference);
        markStartLineNumber(typeReference);
        generateInstanceOf(expressionToMatch, jetType, false);
        StackValue value = StackValue.onStack(Type.BOOLEAN_TYPE);
        return negated ? StackValue.not(value) : value;
    }

    private void generateInstanceOf(StackValue expressionToGen, JetType jetType, boolean leaveExpressionOnStack) {
        expressionToGen.put(OBJECT_TYPE, v);
        if (leaveExpressionOnStack) {
            v.dup();
        }
        Type type = boxType(asmType(jetType));
        if (jetType.isNullable()) {
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
    public StackValue visitWhenExpression(@NotNull JetWhenExpression expression, StackValue receiver) {
        return generateWhenExpression(expression, false);
    }

    public StackValue generateWhenExpression(JetWhenExpression expression, boolean isStatement) {
        JetExpression expr = expression.getSubjectExpression();
        Type subjectType = expressionType(expr);

        Type resultType = isStatement ? Type.VOID_TYPE : expressionType(expression);

        SwitchCodegen switchCodegen = SwitchCodegenUtil.buildAppropriateSwitchCodegenIfPossible(expression, isStatement, this);
        if (switchCodegen != null) {
            switchCodegen.generate();
            return StackValue.onStack(resultType);
        }

        int subjectLocal = expr != null ? myFrameMap.enterTemp(subjectType) : -1;
        if (subjectLocal != -1) {
            gen(expr, subjectType);
            tempVariables.put(expr, StackValue.local(subjectLocal, subjectType));
            v.store(subjectLocal, subjectType);
        }

        Label end = new Label();
        boolean hasElse = JetPsiUtil.checkWhenExpressionHasSingleElse(expression);

        Label nextCondition = null;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            if (nextCondition != null) {
                v.mark(nextCondition);
            }
            nextCondition = new Label();
            FrameMap.Mark mark = myFrameMap.mark();
            Label thisEntry = new Label();
            if (!whenEntry.isElse()) {
                JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    StackValue conditionValue = generateWhenCondition(subjectType, subjectLocal, conditions[i]);
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
            if (!isStatement) {
                putUnitInstanceOntoStackForNonExhaustiveWhen(expression);
            }
        }

        markLineNumber(expression, isStatement);
        v.mark(end);

        myFrameMap.leaveTemp(subjectType);
        tempVariables.remove(expr);
        return StackValue.onStack(resultType);
    }

    public void putUnitInstanceOntoStackForNonExhaustiveWhen(
            @NotNull JetWhenExpression expression
    ) {
        if (Boolean.TRUE.equals(bindingContext.get(BindingContext.EXHAUSTIVE_WHEN, expression))) {
            // when() is supposed to be exhaustive
            genThrow(v, "kotlin/NoWhenBranchMatchedException", null);
        }
        else {
            // non-exhaustive when() with no else -> Unit must be expected
            StackValue.putUnitInstance(v);
        }
    }

    private StackValue generateWhenCondition(Type subjectType, int subjectLocal, JetWhenCondition condition) {
        if (condition instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange conditionInRange = (JetWhenConditionInRange) condition;
            return generateIn(StackValue.local(subjectLocal, subjectType),
                              conditionInRange.getRangeExpression(),
                              conditionInRange.getOperationReference());
        }
        StackValue.Local match = subjectLocal == -1 ? null : StackValue.local(subjectLocal, subjectType);
        if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
            return generateIsCheck(match, patternCondition.getTypeReference(), patternCondition.isNegated());
        }
        else if (condition instanceof JetWhenConditionWithExpression) {
            JetExpression patternExpression = ((JetWhenConditionWithExpression) condition).getExpression();
            return generateExpressionMatch(match, patternExpression);
        }
        else {
            throw new UnsupportedOperationException("unsupported kind of when condition");
        }
    }

    private boolean isIntRangeExpr(JetExpression rangeExpression) {
        if (rangeExpression instanceof JetBinaryExpression) {
            JetBinaryExpression binaryExpression = (JetBinaryExpression) rangeExpression;
            if (binaryExpression.getOperationReference().getReferencedNameElementType() == JetTokens.RANGE) {
                JetType jetType = bindingContext.get(EXPRESSION_TYPE, rangeExpression);
                assert jetType != null;
                DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
                return INTEGRAL_RANGES.contains(descriptor);
            }
        }
        return false;
    }

    private Call makeFakeCall(ReceiverValue initializerAsReceiver) {
        JetSimpleNameExpression fake = JetPsiFactory(state.getProject()).createSimpleName("fake");
        return CallMaker.makeCall(fake, initializerAsReceiver);
    }

    @Override
    public String toString() {
        return context.getContextDescriptor().toString();
    }

    @NotNull
    public FrameMap getFrameMap() {
        return myFrameMap;
    }

    @NotNull
    public MethodContext getContext() {
        return context;
    }

    @NotNull
    public NameGenerator getInlineNameGenerator() {
        NameGenerator nameGenerator = getParentCodegen().getInlineNameGenerator();
        Name name = context.getContextDescriptor().getName();
        return nameGenerator.subGenerator((name.isSpecial() ? "$special" : name.asString()) + "$$inlined" );
    }

    public Type getReturnType() {
        return returnType;
    }

    public Stack<BlockStackElement> getBlockStackElements() {
        return new Stack<BlockStackElement>(blockStackElements);
    }

    public void addBlockStackElementsForNonLocalReturns(@NotNull Stack<BlockStackElement> elements) {
        blockStackElements.addAll(elements);
    }

    private static class NonLocalReturnInfo {

        final Type returnType;

        final String labelName;

        private NonLocalReturnInfo(Type type, String name) {
            returnType = type;
            labelName = name;
        }
    }
}
