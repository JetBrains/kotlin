package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import jet.IntRange;
import jet.JetObject;
import jet.Range;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * @author max
 */
public class ExpressionCodegen extends JetVisitor {
    private static final String CLASS_OBJECT = "java/lang/Object";
    private static final String CLASS_STRING = "java/lang/String";
    private static final String CLASS_STRING_BUILDER = "java/lang/StringBuilder";
    private static final String CLASS_COMPARABLE = "java/lang/Comparable";
    private static final String CLASS_ITERABLE = "java/lang/Iterable";
    private static final String CLASS_ITERATOR = "java/util/Iterator";

    private static final String CLASS_RANGE = "jet/Range";
    private static final String CLASS_INT_RANGE = "jet/IntRange";
    private static final String CLASS_NO_PATTERN_MATCHED_EXCEPTION = "jet/NoPatternMatchedException";
    private static final String CLASS_TYPE_CAST_EXCEPTION = "jet/TypeCastException";

    private static final String ITERABLE_ITERATOR_DESCRIPTOR = "()Ljava/util/Iterator;";
    private static final String ITERATOR_HASNEXT_DESCRIPTOR = "()Z";
    private static final String ITERATOR_NEXT_DESCRIPTOR = "()Ljava/lang/Object;";
    private static final String INT_RANGE_CONSTRUCTOR_DESCRIPTOR = "(II)V";

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type INTEGER_TYPE = Type.getType(Integer.class);
    private static final Type ITERATOR_TYPE = Type.getType(Iterator.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

    private static final Type RANGE_TYPE = Type.getType(Range.class);
    private static final Type INT_RANGE_TYPE = Type.getType(IntRange.class);
    private static final Type JET_OBJECT_TYPE = Type.getType(JetObject.class);

    private final Stack<Label> myContinueTargets = new Stack<Label>();
    private final Stack<Label> myBreakTargets = new Stack<Label>();
    private final Stack<StackValue> myStack = new Stack<StackValue>();

    private final InstructionAdapter v;
    private final FrameMap myMap;
    private final JetTypeMapper typeMapper;
    private final Codegens factory;
    private final Type returnType;
    private final DeclarationDescriptor contextType;
    private final OwnerKind contextKind;
    private final BindingContext bindingContext;
    private final StackValue thisExpression;
    private final Map<ClassDescriptor, StackValue> outerThisExpressions = new HashMap<ClassDescriptor, StackValue>();

    public ExpressionCodegen(MethodVisitor v,
                             BindingContext bindingContext,
                             FrameMap myMap,
                             JetTypeMapper typeMapper,
                             Type returnType,
                             DeclarationDescriptor contextType,
                             OwnerKind contextKind,
                             @Nullable StackValue thisExpression,
                             Codegens factory) {
        this.myMap = myMap;
        this.typeMapper = typeMapper;
        this.returnType = returnType;
        this.contextType = contextType;
        this.contextKind = contextKind;
        this.factory = factory;
        this.v = new InstructionAdapter(v);
        this.bindingContext = bindingContext;
        this.thisExpression = thisExpression;
    }

    public void addOuterThis(ClassDescriptor outer, StackValue expression) {
        outerThisExpressions.put(outer, expression);
    }

    static void loadTypeInfo(ClassDescriptor descriptor, InstructionAdapter v) {
        String owner = JetTypeMapper.jvmNameForImplementation(descriptor);
        if (descriptor.getTypeConstructor().getParameters().size() > 0) {
            v.load(0, JetTypeMapper.TYPE_OBJECT);
            v.getfield(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
        }
        else {
            v.getstatic(owner, "$typeInfo", "Ljet/typeinfo/TypeInfo;");
        }
    }

    private void gen(JetElement expr) {
        if (expr == null) throw new CompilationException();
        expr.accept(this);
    }

    public void gen(JetElement expr, Type type) {
        int oldStackDepth = myStack.size();
        gen(expr);
        if (myStack.size() == oldStackDepth+1) {
            putTopOfStack(type);
        }
    }

    private void putTopOfStack(Type type) {
        StackValue value = myStack.pop();
        value.put(type, v);
    }

    public void genToJVMStack(JetExpression expr) {
        gen(expr, expressionType(expr));
    }

    private StackValue generateIntermediateValue(final JetExpression baseExpression) {
        int oldStackSize = myStack.size();
        gen(baseExpression);
        if (myStack.size() != oldStackSize+1) {
            throw new UnsupportedOperationException("intermediate value expected");
        }
        return myStack.pop();
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
        myContinueTargets.push(condition);
        v.mark(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        gen(expression.getCondition());
        myStack.pop().condJump(end, true, v);

        gen(expression.getBody(), Type.VOID_TYPE);
        v.goTo(condition);

        v.mark(end);
        myBreakTargets.pop();
        myContinueTargets.pop();
    }

    @Override
    public void visitDoWhileExpression(JetDoWhileExpression expression) {
        Label condition = new Label();
        v.mark(condition);
        myContinueTargets.push(condition);

        Label end = new Label();
        myBreakTargets.push(end);

        gen(expression.getBody(), Type.VOID_TYPE);

        gen(expression.getCondition());
        myStack.pop().condJump(condition, false, v);

        v.mark(end);

        myBreakTargets.pop();
        myContinueTargets.pop();
    }

    @Override
    public void visitForExpression(JetForExpression expression) {
        final JetExpression loopRange = expression.getLoopRange();
        final JetType expressionType = bindingContext.getExpressionType(loopRange);
        Type loopRangeType = typeMapper.mapType(expressionType);
        if (loopRangeType.getSort() == Type.ARRAY) {
            new ForInArrayLoopGenerator(expression, loopRangeType).invoke();
        }
        else {
            final DeclarationDescriptor descriptor = expressionType.getConstructor().getDeclarationDescriptor();
            final PsiElement declaration = bindingContext.getDeclarationPsiElement(descriptor);
            if (declaration instanceof PsiClass) {
                final Project project = declaration.getProject();
                final PsiClass iterable = JavaPsiFacade.getInstance(project).findClass("java.lang.Iterable", ProjectScope.getAllScope(project));
                if (((PsiClass) declaration).isInheritor(iterable, true)) {
                    generateForInIterable(expression, loopRangeType);
                    return;
                }
            }
            if (isClass(descriptor, "IntRange")) {       // TODO IntRange subclasses
                new ForInRangeLoopGenerator(expression, loopRangeType).invoke();
                return;
            }
            throw new UnsupportedOperationException("for/in loop currently only supported for arrays and Iterable instances");
        }
    }

    private void generateForInIterable(JetForExpression expression, Type loopRangeType) {
        final JetParameter loopParameter = expression.getLoopParameter();
        final VariableDescriptor parameterDescriptor = bindingContext.getVariableDescriptor(loopParameter);
        JetType paramType = parameterDescriptor.getOutType();
        Type asmParamType = typeMapper.mapType(paramType);

        int iteratorVar = myMap.enterTemp();
        gen(expression.getLoopRange(), loopRangeType);
        v.invokeinterface(CLASS_ITERABLE, "iterator", ITERABLE_ITERATOR_DESCRIPTOR);
        v.store(iteratorVar, ITERATOR_TYPE);

        Label begin = new Label();
        Label end = new Label();
        myContinueTargets.push(begin);
        myBreakTargets.push(end);

        v.mark(begin);
        v.load(iteratorVar, ITERATOR_TYPE);
        v.invokeinterface(CLASS_ITERATOR, "hasNext", ITERATOR_HASNEXT_DESCRIPTOR);
        v.ifeq(end);

        myMap.enter(parameterDescriptor, asmParamType.getSize());
        v.load(iteratorVar, ITERATOR_TYPE);
        v.invokeinterface(CLASS_ITERATOR, "next", ITERATOR_NEXT_DESCRIPTOR);
        // TODO checkcast should be generated via StackValue
        if (asmParamType.getSort() == Type.OBJECT && !"java.lang.Object".equals(asmParamType.getClassName())) {
            v.checkcast(asmParamType);
        }
        v.store(myMap.getIndex(parameterDescriptor), asmParamType);

        gen(expression.getBody(), Type.VOID_TYPE);

        v.goTo(begin);
        v.mark(end);

        int paramIndex = myMap.leave(parameterDescriptor);
        v.visitLocalVariable(loopParameter.getName(), asmParamType.getDescriptor(), null, begin, end, paramIndex);
        myMap.leaveTemp();
        myBreakTargets.pop();
        myContinueTargets.pop();
    }

    private abstract class ForLoopGenerator {
        protected JetForExpression expression;
        protected Type loopRangeType;
        protected VariableDescriptor parameterDescriptor;

        public ForLoopGenerator(JetForExpression expression, Type loopRangeType) {
            this.expression = expression;
            this.loopRangeType = loopRangeType;
            final JetParameter loopParameter = expression.getLoopParameter();
            this.parameterDescriptor = bindingContext.getVariableDescriptor(loopParameter);
        }

        public void invoke() {
            JetType paramType = parameterDescriptor.getOutType();
            Type asmParamType = typeMapper.mapType(paramType);

            myMap.enter(parameterDescriptor, asmParamType.getSize());
            generatePrologue();

            Label condition = new Label();
            Label increment = new Label();
            Label end = new Label();
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
            final int paramIndex = myMap.leave(parameterDescriptor);
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
        private int myLengthVar;
        private int myIndexVar;

        public ForInArrayLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myLengthVar = myMap.enterTemp();
            gen(expression.getLoopRange(), loopRangeType);
            v.arraylength();
            v.store(myLengthVar, Type.INT_TYPE);
            myIndexVar = myMap.enterTemp();
            v.aconst(0);
            v.store(myIndexVar, Type.INT_TYPE);
        }

        protected void generateCondition(Type asmParamType, Label end) {
            v.load(myIndexVar, Type.INT_TYPE);
            v.load(myLengthVar, Type.INT_TYPE);
            v.ificmpge(end);

            gen(expression.getLoopRange(), loopRangeType);  // array
            v.load(myIndexVar, Type.INT_TYPE);
            v.aload(loopRangeType.getElementType());
            StackValue.onStack(loopRangeType.getElementType()).put(asmParamType, v);
            v.store(myMap.getIndex(parameterDescriptor), asmParamType);
        }

        protected void generateIncrement() {
            v.iinc(myIndexVar, 1);
        }

        protected void cleanupTemp() {
            myMap.leaveTemp(2);
        }
    }

    private class ForInRangeLoopGenerator extends ForLoopGenerator {
        private int myRangeVar;
        private int myEndVar;

        public ForInRangeLoopGenerator(JetForExpression expression, Type loopRangeType) {
            super(expression, loopRangeType);
        }

        @Override
        protected void generatePrologue() {
            myRangeVar = myMap.enterTemp();
            myEndVar = myMap.enterTemp();
            gen(expression.getLoopRange(), loopRangeType);
            v.dup();
            v.dup();
            v.store(myRangeVar, loopRangeType);

            v.invokevirtual("jet/IntRange", "getStartValue", "()I");
            v.store(myMap.getIndex(parameterDescriptor), Type.INT_TYPE);
            v.invokevirtual("jet/IntRange", "getEndValue", "()I");
            v.store(myEndVar, Type.INT_TYPE);
        }

        @Override
        protected void generateCondition(Type asmParamType, Label end) {
            v.load(myMap.getIndex(parameterDescriptor), Type.INT_TYPE);
            v.load(myEndVar, Type.INT_TYPE);
            v.ificmpgt(end);
        }

        @Override
        protected void generateIncrement() {
            v.iinc(myMap.getIndex(parameterDescriptor), 1);  // TODO support decreasing order
        }

        @Override
        protected void cleanupTemp() {
            myMap.leaveTemp(2);
        }
    }

    @Override
    public void visitBreakExpression(JetBreakExpression expression) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();

        Label label = labelElement == null ? myBreakTargets.peek() : null; // TODO:

        v.goTo(label);
    }

    @Override
    public void visitContinueExpression(JetContinueExpression expression) {
        String labelName = expression.getLabelName();

        Label label = labelName == null ? myContinueTargets.peek() : null; // TODO:

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
            final GeneratedClosureDescriptor closure = new ClosureCodegen(bindingContext, typeMapper, factory).gen(expression);
            v.anew(Type.getObjectType(closure.getClassname()));
            v.dup();
            v.invokespecial(closure.getClassname(), "<init>", closure.getConstructor().getDescriptor());
        }
    }

    private void generateBlock(List<JetElement> statements) {
        Label blockStart = new Label();
        v.mark(blockStart);

        for (JetElement statement : statements) {
            if (statement instanceof JetProperty) {
                final VariableDescriptor variableDescriptor = bindingContext.getVariableDescriptor((JetProperty) statement);
                final Type type = typeMapper.mapType(variableDescriptor.getOutType());
                myMap.enter(variableDescriptor, type.getSize());
            }
        }

        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            JetElement statement = statements.get(i);
            if (i == statements.size() - 1) {
                gen(statement);
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
                VariableDescriptor variableDescriptor = bindingContext.getVariableDescriptor(var);
                Type outType = typeMapper.mapType(variableDescriptor.getOutType());

                int index = myMap.leave(variableDescriptor);
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
            putTopOfStack(returnType);
            v.areturn(returnType);
        }
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        DeclarationDescriptor descriptor = bindingContext.resolveReferenceExpression(expression);

        if (descriptor instanceof VariableAsFunctionDescriptor) {
            descriptor = ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        }

        final DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (descriptor instanceof VariableDescriptor) {
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
            final String owner = JetTypeMapper.jvmName(psiField.getContainingClass());
            final Type fieldType = psiTypeToAsm(psiField.getType());
            final boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
            if (!isStatic) {
                ensureReceiverOnStack(expression, null);
            }
            myStack.push(StackValue.field(fieldType, owner, psiField.getName(), isStatic));
        }
        else {
            int index = myMap.getIndex(descriptor);
            if (index >= 0) {
                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                myStack.push(StackValue.local(index, typeMapper.mapType(outType)));
            }
            else if (descriptor instanceof PropertyDescriptor) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

                //TODO: hack, will not need if resolve goes to right descriptor itself
                if (declaration instanceof JetParameter) {
                    if (PsiTreeUtil.getParentOfType(expression, JetDelegationSpecifier.class)  != null) {
                        JetClass aClass = PsiTreeUtil.getParentOfType(expression, JetClass.class);
                        ConstructorDescriptor constructorDescriptor = bindingContext.getConstructorDescriptor(aClass);
                        List<ValueParameterDescriptor> parameters = constructorDescriptor.getUnsubstitutedValueParameters();
                        for (ValueParameterDescriptor parameter : parameters) {
                            if (parameter.getName().equals(descriptor.getName())) {
                                final JetType outType = ((VariableDescriptor) descriptor).getOutType();
                                myStack.push(StackValue.local(myMap.getIndex(parameter), typeMapper.mapType(outType)));
                                return;
                            }
                        }
                    }
                }

                if (isClass(container, "Array") && propertyDescriptor.getName().equals("size")) {
                    ensureReceiverOnStack(expression, null);
                    v.arraylength();
                    myStack.push(StackValue.onStack(Type.INT_TYPE));
                }
                else if (declaration instanceof JetObjectDeclarationName) {
                    JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(declaration, JetObjectDeclaration.class);
                    ClassDescriptor classDescriptor = bindingContext.getClassDescriptor(objectDeclaration);
                    myStack.push(StackValue.field(typeMapper.jvmType(classDescriptor, OwnerKind.IMPLEMENTATION),
                            typeMapper.jvmName(classDescriptor, OwnerKind.IMPLEMENTATION),
                            "$instance",
                            true));
                }
                else {
                    boolean isStatic = container instanceof NamespaceDescriptorImpl;
                    final boolean directToField = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
                    JetExpression receiver = getReceiverForSelector(expression);
                    final boolean forceInterface = receiver != null && !(receiver instanceof JetThisExpression);
                    final StackValue iValue = intermediateValueForProperty(propertyDescriptor, directToField, forceInterface);
                    if (!isStatic) {
                        ensureReceiverOnStack(expression, container instanceof ClassDescriptor ? (ClassDescriptor) container : null);
                    }
                    myStack.push(iValue);
                }
            }
            else if (!(descriptor instanceof NamespaceDescriptor)) {
                throw new UnsupportedOperationException("don't know how to generate reference " + descriptor);
            }
        }
    }

    public StackValue intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, boolean forceInterface) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        boolean isStatic = containingDeclaration instanceof NamespaceDescriptorImpl;
        final JetType outType = propertyDescriptor.getOutType();
        boolean isInsideClass = !forceInterface && containingDeclaration == contextType;
        Method getter;
        Method setter;
        if (forceField) {
            getter = null;
            setter = null;
        }
        else {
            getter = isInsideClass && propertyDescriptor.getGetter() == null ? null : typeMapper.mapGetterSignature(propertyDescriptor);
            setter = isInsideClass && propertyDescriptor.getSetter() == null ? null : typeMapper.mapSetterSignature(propertyDescriptor);
        }

        String owner;
        boolean isInterface;
        if (isInsideClass || isStatic) {
            owner = typeMapper.getOwner(propertyDescriptor, contextKind);
            isInterface = false;
        }
        else {
            owner = typeMapper.getOwner(propertyDescriptor, OwnerKind.INTERFACE);
            isInterface = !(containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).isObject());
        }

        return StackValue.property(propertyDescriptor.getName(), owner, typeMapper.mapType(outType), isStatic, isInterface, getter, setter);
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
            if (funDescriptor instanceof ConstructorDescriptor) {
                generateConstructorCall(expression, (JetSimpleNameExpression) callee);
            }
            else if (funDescriptor instanceof FunctionDescriptor) {
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


                PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(funDescriptor);
                final FunctionDescriptor fd = (FunctionDescriptor) funDescriptor;
                if (declarationPsiElement == null && isClass(functionParent, "String")) {
                    final Project project = expression.getProject();
                    PsiClass jlString = JavaPsiFacade.getInstance(project).findClass("java.lang.String",
                            ProjectScope.getAllScope(project));
                    // TODO better overload mapping
                    final PsiMethod[] methods = jlString.findMethodsByName(funDescriptor.getName(), false);
                    final int arity = fd.getUnsubstitutedValueParameters().size();
                    for (PsiMethod method : methods) {
                        if (method.getParameterList().getParametersCount() == arity) {
                            declarationPsiElement = method;
                        }
                    }
                }
                if (declarationPsiElement == null) {
                    throw new UnsupportedOperationException("couldn't find declaration for " + funDescriptor);
                }
                Method methodDescriptor;
                if (declarationPsiElement instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) declarationPsiElement;
                    String owner = JetTypeMapper.jvmName(psiMethod.getContainingClass());
                    methodDescriptor = getMethodDescriptor(psiMethod);
                    final boolean isStatic = psiMethod.hasModifierProperty(PsiModifier.STATIC);

                    if (!isStatic) {
                        ensureReceiverOnStack(expression, null);
                        if (expression.getParent() instanceof JetQualifiedExpression) {
                            final JetExpression receiver = ((JetQualifiedExpression) expression.getParent()).getReceiverExpression();
                            JetType expressionType = bindingContext.getExpressionType(receiver);
                            DeclarationDescriptor declarationDescriptor = expressionType.getConstructor().getDeclarationDescriptor();
                            PsiElement ownerDeclaration = bindingContext.getDeclarationPsiElement(declarationDescriptor);
                            if (ownerDeclaration instanceof PsiClass) {
                                owner = typeMapper.mapType(expressionType).getInternalName();
                            }
                        }
                    }
                    pushMethodArguments(expression, methodDescriptor);

                    v.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                            owner,
                            methodDescriptor.getName(),
                            methodDescriptor.getDescriptor());
                }
                else if(declarationPsiElement instanceof JetFunction) {
                    final JetFunction jetFunction = (JetFunction) declarationPsiElement;
                    methodDescriptor = typeMapper.mapSignature(jetFunction);
                    if (functionParent instanceof NamespaceDescriptorImpl) {
                        if (jetFunction.getReceiverTypeRef() != null) {
                            ensureReceiverOnStack(expression, null);
                        }
                        pushMethodArguments(expression, methodDescriptor);
                        final String owner = NamespaceCodegen.getJVMClassName(DescriptorRenderer.getFQName(functionParent));
                        v.invokestatic(owner, methodDescriptor.getName(), methodDescriptor.getDescriptor());
                    }
                    else if (functionParent instanceof ClassDescriptor) {
                        ensureReceiverOnStack(expression, (ClassDescriptor) functionParent);
                        pushMethodArguments(expression, methodDescriptor);
                        final String owner = JetTypeMapper.jvmNameForInterface((ClassDescriptor) functionParent);
                        v.invokeinterface(owner, methodDescriptor.getName(), methodDescriptor.getDescriptor());
                    }
                    else {
                        throw new UnsupportedOperationException("don't know how to generate call to " + declarationPsiElement);
                    }
                }
                else {
                    gen(callee, Type.getObjectType(ClosureCodegen.getInternalClassName(fd)));

                    boolean isExtensionFunction = fd.getReceiverType() != null;
                    int paramCount = fd.getUnsubstitutedValueParameters().size();
                    if (isExtensionFunction) {
                        ensureReceiverOnStack(expression, null);
                        paramCount++;
                    }

                    methodDescriptor = ClosureCodegen.erasedInvokeSignature(fd);
                    pushMethodArguments(expression, methodDescriptor);
                    v.invokevirtual(ClosureCodegen.getInternalClassName(fd), "invoke", methodDescriptor.getDescriptor());
                }

                if (methodDescriptor.getReturnType() != Type.VOID_TYPE) {
                    final Type retType = typeMapper.mapType(fd.getUnsubstitutedReturnType());
                    StackValue.onStack(methodDescriptor.getReturnType()).put(retType, v);
                    myStack.push(StackValue.onStack(retType));
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

    private JetExpression getReceiverForSelector(JetElement expression) {
        if (expression.getParent() instanceof JetDotQualifiedExpression && !isReceiver(expression)) {
            final JetDotQualifiedExpression parent = (JetDotQualifiedExpression) expression.getParent();
            return parent.getReceiverExpression();
        }
        return null;
    }

    private void ensureReceiverOnStack(JetElement expression, @Nullable ClassDescriptor calleeContainingClass) {
        JetExpression receiver = getReceiverForSelector(expression);
        if (receiver != null) {
            if (!resolvesToClassOrPackage(receiver)) {
                // we have a receiver on stack
                if (myStack.isEmpty()) {
                    throw new IllegalStateException("expected receiver on stack but it's not there: " + receiver.getText());
                }
                myStack.pop().put(JetTypeMapper.TYPE_OBJECT, v);
            }
        }
        else if (!(expression.getParent() instanceof JetSafeQualifiedExpression)) {
            final StackValue value = generateThisOrOuter(calleeContainingClass);
            value.put(value.type, v);
        }
    }

    public StackValue generateThisOrOuter(ClassDescriptor calleeContainingClass) {
        final StackValue value = outerThisExpressions.get(calleeContainingClass);
        if (value != null) {
            return value;
        }
        else {
            // TODO hope it works; really need more checks here :)
            if (calleeContainingClass != null && contextType instanceof ClassDescriptor &&
                calleeContainingClass == contextType.getContainingDeclaration()) {
                v.load(0, JetTypeMapper.TYPE_OBJECT);
                return StackValue.field(typeMapper.jvmType(calleeContainingClass, OwnerKind.IMPLEMENTATION),
                        typeMapper.jvmName((ClassDescriptor) contextType, OwnerKind.IMPLEMENTATION),
                        "this$0",
                        false);
                // TODO handle more levels of class nestng
            }
            else {
                return StackValue.local(0, JetTypeMapper.TYPE_OBJECT);
            }
        }
    }

    private static boolean isReceiver(JetElement expression) {
        final PsiElement parent = expression.getParent();
        if (parent instanceof JetQualifiedExpression) {
            final JetExpression receiverExpression = ((JetQualifiedExpression) parent).getReceiverExpression();
            return expression == receiverExpression;
        }
        return false;
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
            if (psiClass instanceof PsiTypeParameter) {
                final PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
                if (extendsListTypes.length > 0) {
                    throw new UnsupportedOperationException("should return common supertype");
                }
                return OBJECT_TYPE;
            }
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
        genToJVMStack(expression.getReceiverExpression());
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
            generateEquals(expression.getLeft(), expression.getRight(), opToken);
        }
        else if (opToken == JetTokens.LT || opToken == JetTokens.LTEQ ||
                 opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
            generateCompareOp(expression.getLeft(), expression.getRight(), opToken, expressionType(expression.getLeft()));
        }
        else if (opToken == JetTokens.ELVIS) {
            generateElvis(expression);
        }
        else if (opToken == JetTokens.RANGE) {
            generateRange(expression);
        }
        else {
            DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationReference());
            if (op instanceof FunctionDescriptor) {
                DeclarationDescriptor cls = op.getContainingDeclaration();
                if (isNumberPrimitive(cls)) {
                    int opcode = opcodeForMethod(op.getName());
                    generateBinaryOp(expression, (FunctionDescriptor) op, opcode);
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

    private void generateEquals(JetExpression left, JetExpression right, IElementType opToken) {
        final Type leftType = expressionType(left);
        final Type rightType = expressionType(right);
        gen(left, leftType);
        gen(right, rightType);
        generateEqualsForExpressionsOnStack(opToken, leftType, rightType);
    }

    private void generateEqualsForExpressionsOnStack(IElementType opToken, Type leftType, Type rightType) {
        if (isNumberPrimitive(leftType) && leftType == rightType) {
            compareExpressionsOnStack(opToken, leftType);
        }
        else {
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

    private void generateElvis(JetBinaryExpression expression) {
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
        myStack.push(StackValue.onStack(exprType));
    }

    private void generateRange(JetBinaryExpression expression) {
        final Type leftType = expressionType(expression.getLeft());
        if (JetTypeMapper.isIntPrimitive(leftType)) {
            v.anew(INT_RANGE_TYPE);
            v.dup();
            gen(expression.getLeft(), Type.INT_TYPE);
            gen(expression.getRight(), Type.INT_TYPE);
            v.invokespecial(CLASS_INT_RANGE, "<init>", INT_RANGE_CONSTRUCTOR_DESCRIPTOR);
            myStack.push(StackValue.onStack(INT_RANGE_TYPE));
        }
        else {
            throw new UnsupportedOperationException("ranges are only supported for int objects");
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
        return JetTypeMapper.isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
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

    private void generateCompareOp(JetExpression left, JetExpression right, IElementType opToken, Type operandType) {
        gen(left, operandType);
        gen(right, operandType);
        compareExpressionsOnStack(opToken, operandType);
    }

    private void compareExpressionsOnStack(IElementType opToken, Type operandType) {
        if (operandType.getSort() == Type.OBJECT) {
            v.invokeinterface(CLASS_COMPARABLE, "compareTo", "(Ljava/lang/Object;)I");
            v.aconst(0);
            operandType = Type.INT_TYPE;
        }
        myStack.push(StackValue.cmp(opToken, operandType));
    }

    private void generateAssignmentExpression(JetBinaryExpression expression) {
        StackValue stackValue = generateIntermediateValue(expression.getLeft());
        genToJVMStack(expression.getRight());
        stackValue.store(v);
    }

    private void generateAugmentedAssignment(JetBinaryExpression expression) {
        DeclarationDescriptor op = bindingContext.resolveReferenceExpression(expression.getOperationReference());
        final JetExpression lhs = expression.getLeft();
        Type lhsType = expressionType(lhs);
        if (isNumberPrimitive(lhsType)) {
            StackValue value = generateIntermediateValue(lhs);              // receiver
            value.dupReceiver(v, 0);                                        // receiver receiver
            value.put(lhsType, v);                                          // receiver lhs
            genToJVMStack(expression.getRight());                           // receiver lhs rhs
            v.visitInsn(lhsType.getOpcode(opcodeForMethod(op.getName())));  // receiver result
            value.store(v);
        }
        else if ("java.lang.String".equals(lhsType.getClassName()) && op.getName().equals("plus")) {
            generateStringBuilderConstructor();                          // StringBuilder
            StackValue value = generateIntermediateValue(lhs);           // StringBuilder receiver
            value.dupReceiver(v, 1);                                     // receiver StringBuilder receiver
            value.put(lhsType, v);                                       // receiver StringBuilder value
            invokeAppendMethod(lhsType);                                 // receiver StringBuilder
            invokeAppend(expression.getRight());                         // receiver StringBuilder
            v.invokevirtual(CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
            value.store(v);
        }
        else {
            throw new UnsupportedOperationException("Augmented assignment for non-primitive types not yet implemented");
        }
    }

    private void generateConcatenation(JetBinaryExpression expression) {
        generateStringBuilderConstructor();
        invokeAppend(expression.getLeft());
        invokeAppend(expression.getRight());
        v.invokevirtual(CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
        myStack.push(StackValue.onStack(Type.getObjectType(CLASS_STRING)));
    }

    private void generateStringBuilderConstructor() {
        Type type = Type.getObjectType(CLASS_STRING_BUILDER);
        v.anew(type);
        v.dup();
        Method method = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        v.invokespecial(CLASS_STRING_BUILDER, method.getName(), method.getDescriptor());
    }

    private void invokeAppend(final JetExpression expr) {
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
        invokeAppendMethod(exprType);
    }

    private void invokeAppendMethod(Type exprType) {
        Method appendDescriptor = new Method("append", Type.getObjectType(CLASS_STRING_BUILDER),
                new Type[] { exprType.getSort() == Type.OBJECT ? JetTypeMapper.TYPE_OBJECT : exprType});
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
            myStack.push(generateIncrement(op, asmType, operand));
            return true;
        }
        return false;
    }

    private void generateNot(JetPrefixExpression expression) {
        final StackValue stackValue = generateIntermediateValue(expression.getBaseExpression());
        myStack.push(StackValue.not(stackValue));
    }

    private StackValue generateIncrement(DeclarationDescriptor op, Type asmType, JetExpression operand) {
        int increment = op.getName().equals("inc") ? 1 : -1;
        if (operand instanceof JetReferenceExpression) {
            final int index = indexOfLocal((JetReferenceExpression) operand);
            if (index >= 0 && JetTypeMapper.isIntPrimitive(asmType)) {
                v.iinc(index, increment);
                return StackValue.local(index, asmType);
            }
        }
        StackValue value = generateIntermediateValue(operand);
        value.dupReceiver(v, 0);
        value.put(asmType, v);
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
            v.aconst(increment);
        }
        v.add(asmType);
        value.store(v);
        return value;
    }

    private void generateInv(Type asmType) {
        v.aconst(-1);
        v.xor(asmType);
        myStack.push(StackValue.onStack(asmType));
    }

    @Override
    public void visitProperty(JetProperty property) {
        VariableDescriptor variableDescriptor = bindingContext.getVariableDescriptor(property);
        int index = myMap.getIndex(variableDescriptor);

        assert index >= 0;

        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            Type type = typeMapper.mapType(variableDescriptor.getOutType());
            gen(initializer, type);
            v.store(index, type);
        }
    }

    private void generateConstructorCall(JetCallExpression expression, JetSimpleNameExpression constructorReference) {
        DeclarationDescriptor constructorDescriptor = bindingContext.resolveReferenceExpression(constructorReference);
        final PsiElement declaration = bindingContext.getDeclarationPsiElement(constructorDescriptor);
        Type type;
        if (declaration instanceof PsiMethod) {
            type = generateJavaConstructorCall(expression, (PsiMethod) declaration);
        }
        else if (constructorDescriptor instanceof ConstructorDescriptor) {
            type = typeMapper.mapType(bindingContext.getExpressionType(expression), OwnerKind.IMPLEMENTATION);
            if (type.getSort() == Type.ARRAY) {
                generateNewArray(expression, type);
            }
            else {
                ClassDescriptor classDecl = (ClassDescriptor) constructorDescriptor.getContainingDeclaration();

                v.anew(type);
                v.dup();

                // TODO typechecker must verify that we're the outer class of the instance being created
                pushOuterClassArguments(classDecl);

                Method method = typeMapper.mapConstructorSignature((ConstructorDescriptor) constructorDescriptor, OwnerKind.IMPLEMENTATION);
                pushMethodArguments(expression, method);

                for (JetTypeProjection jetTypeArgument : expression.getTypeArguments()) {
                    JetType typeArgument = bindingContext.resolveTypeReference(jetTypeArgument.getTypeReference());
                    // TODO is the makeNullable() call correct here?
                    ClassCodegen.newTypeInfo(v, typeMapper.mapType(TypeUtils.makeNullable(typeArgument)));
                }

                v.invokespecial(JetTypeMapper.jvmNameForImplementation(classDecl), "<init>", method.getDescriptor());
            }
        }
        else {
            throw new UnsupportedOperationException("don't know how to generate this new expression");
        }
        myStack.push(StackValue.onStack(type));
    }

    private void pushOuterClassArguments(ClassDescriptor classDecl) {
        if (classDecl.getContainingDeclaration() instanceof ClassDescriptor) {
            v.load(0, JetTypeMapper.jetImplementationType(classDecl));
        }
    }

    private Type generateJavaConstructorCall(JetCallExpression expression, PsiMethod constructor) {
        PsiClass javaClass = constructor.getContainingClass();
        Type type = JetTypeMapper.psiClassType(javaClass);
        v.anew(type);
        v.dup();
        final Method jvmConstructor = getMethodDescriptor(constructor);
        pushMethodArguments(expression, jvmConstructor);
        v.invokespecial(JetTypeMapper.jvmName(javaClass), "<init>", jvmConstructor.getDescriptor());
        return type;
    }

    private void generateNewArray(JetCallExpression expression, Type type) {
        List<JetArgument> args = expression.getValueArguments();
        if (args.size() != 1) {
            throw new CompilationException("array constructor requires one value argument");
        }
        gen(args.get(0).getArgumentExpression(), Type.INT_TYPE);
        v.newarray(type.getElementType());
    }

    @Override
    public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
        final JetExpression array = expression.getArrayExpression();
        final Type arrayType = expressionType(array);
        if (arrayType.getSort() == Type.ARRAY) {
            gen(array, arrayType);
            generateArrayIndex(expression);
            final Type elementType = arrayType.getElementType();
            myStack.push(StackValue.arrayElement(elementType));
        }
        else {
            throw new UnsupportedOperationException("array access to non-Java arrays is not supported");
        }
    }

    private void generateArrayIndex(JetArrayAccessExpression expression) {
        final List<JetExpression> indices = expression.getIndexExpressions();
        if (indices.size() != 1) {
            throw new UnsupportedOperationException("array access with more than one index is not supported");
        }
        if (!expressionType(indices.get(0)).equals(Type.INT_TYPE)) {
            throw new UnsupportedOperationException("array access with non-integer is not supported");
        }
        gen(indices.get(0), Type.INT_TYPE);
    }

    @Override
    public void visitThrowExpression(JetThrowExpression expression) {
        gen(expression.getThrownExpression(), JetTypeMapper.TYPE_OBJECT);
        v.athrow();
    }

    @Override
    public void visitThisExpression(JetThisExpression expression) {
        final DeclarationDescriptor descriptor = bindingContext.resolveReferenceExpression(expression.getThisReference());
        if (descriptor instanceof ClassDescriptor) {
            myStack.push(generateThisOrOuter((ClassDescriptor) descriptor));
        }
        else {
            generateThis();
        }
    }

    public void thisToStack() {
        generateThis();
        putTopOfStack(JetTypeMapper.TYPE_OBJECT);
    }

    private void generateThis() {
        if (thisExpression != null) {
            myStack.push(thisExpression);
            return;
        }
        if (contextKind != OwnerKind.NAMESPACE) {
            ClassDescriptor contextClass = (ClassDescriptor) contextType;
            final Type thisType = typeMapper.jvmType(contextClass, contextKind);
            if (contextKind == OwnerKind.IMPLEMENTATION) {
                myStack.push(StackValue.local(0, thisType));
                return;
            }
            else if (contextKind == OwnerKind.DELEGATING_IMPLEMENTATION) {
                v.load(0, thisType);
                myStack.push(StackValue.field(JetTypeMapper.jetInterfaceType(contextClass),
                        thisType.getInternalName(),
                        "$this",
                        false));
                return;
            }
        }
        throw new UnsupportedOperationException("'this' expression is not defined in the context");
    }

    @Override
    public void visitTryExpression(JetTryExpression expression) {
        Label tryStart = new Label();
        v.mark(tryStart);
        gen(expression.getTryBlock(), Type.VOID_TYPE);
        Label tryEnd = new Label();
        v.mark(tryEnd);
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock != null) {
            gen(finallyBlock.getFinalExpression(), Type.VOID_TYPE);
        }
        Label end = new Label();
        v.goTo(end);         // TODO don't generate goto if there's no code following try/catch
        for (JetCatchClause clause : expression.getCatchClauses()) {
            Label clauseStart = new Label();
            v.mark(clauseStart);

            VariableDescriptor descriptor = bindingContext.getVariableDescriptor(clause.getCatchParameter());
            Type descriptorType = typeMapper.mapType(descriptor.getOutType());
            myMap.enter(descriptor, 1);
            int index = myMap.getIndex(descriptor);
            v.store(index, descriptorType);

            gen(clause.getCatchBody(), Type.VOID_TYPE);
            v.goTo(end);     // TODO don't generate goto if there's no code following try/catch

            myMap.leave(descriptor);
            v.visitTryCatchBlock(tryStart, tryEnd, clauseStart, descriptorType.getInternalName());
        }
        if (finallyBlock != null) {
            Label finallyStart = new Label();
            v.mark(finallyStart);

            int index = myMap.enterTemp();
            v.store(index, THROWABLE_TYPE);

            gen(finallyBlock.getFinalExpression(), Type.VOID_TYPE);

            v.load(index, THROWABLE_TYPE);
            v.athrow();

            myMap.leaveTemp();

            v.visitTryCatchBlock(tryStart, tryEnd, finallyStart, null);
        }
        v.mark(end);
    }

    @Override
    public void visitBinaryWithTypeRHSExpression(final JetBinaryExpressionWithTypeRHS expression) {
        JetSimpleNameExpression operationSign = expression.getOperationSign();
        IElementType opToken = operationSign.getReferencedNameElementType();
        if (opToken == JetTokens.COLON) {
            gen(expression.getLeft());
        }
        else {
            JetTypeReference typeReference = expression.getRight();
            JetType jetType = bindingContext.resolveTypeReference(typeReference);
            DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
            if (!(descriptor instanceof ClassDescriptor)) {
                throw new UnsupportedOperationException("don't know how to handle non-class types in as/as?");
            }
            Type type = typeMapper.mapType(jetType, OwnerKind.INTERFACE);
            generateInstanceOf(new Runnable() {
                        @Override
                        public void run() {
                            gen(expression.getLeft(), OBJECT_TYPE);
                        }
                    }, jetType, true);
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
            myStack.push(StackValue.onStack(type));
        }
    }

    @Override
    public void visitTypeofExpression(JetTypeofExpression expression) {
        gen(expression.getBaseExpression(), JET_OBJECT_TYPE);
        v.invokeinterface("jet/JetObject", "getTypeInfo", "()Ljet/typeinfo/TypeInfo;");
    }

    @Override
    public void visitIsExpression(final JetIsExpression expression) {
        generatePatternMatch(expression.getPattern(), expression.isNegated(), new Runnable() {
            @Override
            public void run() {
                gen(expression.getLeftHandSide(), OBJECT_TYPE);
            }
        });
    }

    private void generatePatternMatch(JetPattern pattern, boolean negated, Runnable expressionGen) {
        if (pattern instanceof JetTypePattern) {
            JetTypeReference typeReference = ((JetTypePattern) pattern).getTypeReference();
            JetType jetType = bindingContext.resolveTypeReference(typeReference);
            generateInstanceOf(expressionGen, jetType, false);
            StackValue value = StackValue.onStack(Type.BOOLEAN_TYPE);
            myStack.push(negated ? StackValue.not(value) : value);
        }
        else if (pattern instanceof JetWildcardPattern) {
            myStack.push(StackValue.constant(!negated, Type.BOOLEAN_TYPE));
        }
        else {
            throw new UnsupportedOperationException("Unsupported pattern type: " + pattern);
        }
    }

    private void generateInstanceOf(Runnable expressionGen, JetType jetType, boolean leaveExpressionOnStack) {
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (!(descriptor instanceof ClassDescriptor)) {
            throw new UnsupportedOperationException("don't know how to handle non-class types");
        }
        if (jetType.getArguments().size() > 0) {
            generateTypeInfo(jetType);
            expressionGen.run();
            if (leaveExpressionOnStack) {
                v.dupX1();
            }
            v.invokevirtual("jet/typeinfo/TypeInfo", "isInstance", "(Ljava/lang/Object;)Z");
        }
        else {
            expressionGen.run();
            if (leaveExpressionOnStack) {
                v.dup();
            }
            Type type = typeMapper.mapType(jetType, OwnerKind.INTERFACE);
            v.instanceOf(type);
        }
    }

    private void generateTypeInfo(JetType jetType) {
        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
            if (containingDeclaration == contextType && contextType instanceof ClassDescriptor) {
                loadTypeInfo((ClassDescriptor) contextType, v);
                v.iconst(((TypeParameterDescriptor) declarationDescriptor).getIndex());
                v.invokevirtual("jet/typeinfo/TypeInfo", "getTypeParameter", "(I)Ljet/typeinfo/TypeInfo;");
                return;
            }
            throw new UnsupportedOperationException("don't know what this type parameter resolves to");
        }

        v.anew(JetTypeMapper.TYPE_TYPEINFO);
        v.dup();
        v.aconst(typeMapper.jvmType((ClassDescriptor) declarationDescriptor, OwnerKind.INTERFACE));
        List<TypeProjection> arguments = jetType.getArguments();
        if (arguments.size() > 0) {
            v.iconst(arguments.size());
            v.newarray(JetTypeMapper.TYPE_TYPEINFO);

            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                TypeProjection argument = arguments.get(i);
                v.dup();
                v.iconst(i);
                generateTypeInfo(argument.getType());
                v.astore(JetTypeMapper.TYPE_OBJECT);
            }
            v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;[Ljet/typeinfo/TypeInfo;)V");
        }
        else {
            v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;)V");
        }
    }

    @Override
    public void visitWhenExpression(JetWhenExpression expression) {
        JetExpression expr = expression.getSubjectExpression();
        final Type subjectType = expressionType(expr);
        final int subjectLocal = myMap.enterTemp(subjectType.getSize());
        gen(expr, subjectType);
        v.store(subjectLocal, subjectType);

        Label end = new Label();
        Label nextEntry = null;
        boolean hasElse = false;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            if (nextEntry != null) {
                v.mark(nextEntry);
            }
            nextEntry = new Label();
            if (!whenEntry.isElse()) {
                JetWhenCondition condition = whenEntry.getCondition();
                if (condition instanceof JetWhenConditionWithExpression) {
                    v.load(subjectLocal, subjectType);
                    JetExpression condExpression = ((JetWhenConditionWithExpression) condition).getExpression();
                    Type condType = expressionType(condExpression);
                    gen(condExpression, condType);
                    generateEqualsForExpressionsOnStack(JetTokens.EQEQ, subjectType, condType);
                }
                else if (condition instanceof JetWhenConditionInRange) {
                    JetExpression range = ((JetWhenConditionInRange) condition).getRangeExpression();
                    gen(range, RANGE_TYPE);
                    new StackValue.Local(subjectLocal, subjectType).put(INTEGER_TYPE, v);
                    v.invokeinterface(CLASS_RANGE, "contains", "(Ljava/lang/Comparable;)Z");
                    myStack.push(new StackValue.OnStack(Type.BOOLEAN_TYPE));
                }
                else if (condition instanceof JetWhenConditionIsPattern) {
                    JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
                    JetPattern pattern = patternCondition.getPattern();
                    generatePatternMatch(pattern, patternCondition.isNegated(), new Runnable() {
                        @Override
                        public void run() {
                            new StackValue.Local(subjectLocal, subjectType).put(OBJECT_TYPE, v);
                        }
                    });
                }
                else {
                    throw new UnsupportedOperationException("unsupported kind of when condition");
                }
                myStack.pop().condJump(nextEntry, true, v);
            }
            else {
                hasElse = true;
            }
            genToJVMStack(whenEntry.getExpression());
            v.goTo(end);
        }
        if (!hasElse && nextEntry != null) {
            v.mark(nextEntry);
            throwNewException(CLASS_NO_PATTERN_MATCHED_EXCEPTION);
        }
        v.mark(end);
        final Type type = expressionType(expression);
        if (type.getSort() != Type.VOID) {
            myStack.push(StackValue.onStack(type));
        }

        myMap.leaveTemp(subjectType.getSize());
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
}
