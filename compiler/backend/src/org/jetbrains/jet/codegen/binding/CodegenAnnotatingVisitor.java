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

package org.jetbrains.jet.codegen.binding;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.SamCodegenUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.*;

import static org.jetbrains.jet.codegen.CodegenUtil.peekFromStack;
import static org.jetbrains.jet.codegen.FunctionTypesUtil.getSuperTypeForClosure;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

class CodegenAnnotatingVisitor extends JetVisitorVoid {
    private static final TokenSet BINARY_OPERATIONS = TokenSet.orSet(
            AUGMENTED_ASSIGNMENTS, TokenSet.create(PLUS, MINUS, MUL, DIV, PERC, RANGE, LT, GT, LTEQ, GTEQ, IDENTIFIER));

    private static class ClassDescriptorWithState {

        private boolean isDelegationToSuperCall;

        private final ClassDescriptor descriptor;

        public ClassDescriptorWithState(ClassDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public boolean isDelegationToSuperCall() {
            return isDelegationToSuperCall;
        }

        public void setDelegationToSuperCall(boolean isDelegationToSuperCall) {
            this.isDelegationToSuperCall = isDelegationToSuperCall;
        }

        public ClassDescriptor getDescriptor() {
            return descriptor;
        }
    }

    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

    private final Stack<ClassDescriptorWithState> classStack = new Stack<ClassDescriptorWithState>();
    private final Stack<String> nameStack = new Stack<String>();
    private final BindingTrace bindingTrace;
    private final BindingContext bindingContext;

    public CodegenAnnotatingVisitor(BindingTrace bindingTrace) {
        this.bindingTrace = bindingTrace;
        this.bindingContext = bindingTrace.getBindingContext();
    }

    @NotNull
    private ClassDescriptor recordClassForFunction(@NotNull FunctionDescriptor funDescriptor, @NotNull JetType superType) {
        ClassDescriptorImpl classDescriptor =
                new ClassDescriptorImpl(funDescriptor.getContainingDeclaration(), Name.special("<closure>"), Modality.FINAL,
                                        Collections.singleton(superType));
        classDescriptor.initialize(JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet(), null);

        bindingTrace.record(CLASS_FOR_FUNCTION, funDescriptor, classDescriptor);
        return classDescriptor;
    }

    private String inventAnonymousClassName(JetElement declaration) {
        String top = peekFromStack(nameStack);
        Integer cnt = anonymousSubclassesCount.get(top);
        if (cnt == null) {
            cnt = 0;
        }
        String name = top + "$" + (cnt + 1);
        ClassDescriptor descriptor = bindingContext.get(CLASS, declaration);
        if (descriptor == null) {
            if (declaration instanceof JetFunctionLiteralExpression ||
                declaration instanceof JetNamedFunction ||
                declaration instanceof JetObjectLiteralExpression ||
                declaration instanceof JetCallableReferenceExpression) {
            }
            else {
                throw new IllegalStateException(
                        "Class-less declaration which is not JetFunctionLiteralExpression|JetNamedFunction|JetObjectLiteralExpression|JetCallableReferenceExpression : " +
                        declaration.getClass().getName());
            }
        }
        anonymousSubclassesCount.put(top, cnt + 1);

        return name;
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        super.visitJetElement(element);
        element.acceptChildren(this);
    }

    @Override
    public void visitJetFile(@NotNull JetFile file) {
        if (file.isScript()) {
            //noinspection ConstantConditions
            ClassDescriptor classDescriptor = bindingContext.get(CLASS_FOR_SCRIPT, bindingContext.get(SCRIPT, file.getScript()));
            pushClassDescriptor(classDescriptor);
            //noinspection ConstantConditions
            nameStack.push(asmTypeForScriptPsi(bindingContext, file.getScript()).getInternalName());
        }
        else {
            nameStack.push(JvmClassName.byFqNameWithoutInnerClasses(JetPsiUtil.getFQName(file)).getInternalName());
        }
        file.acceptChildren(this);
        nameStack.pop();
        if (file.isScript()) {
            popClassDescriptor();
        }
    }

    @Override
    public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
        ClassDescriptor descriptor = bindingContext.get(CLASS, enumEntry);
        assert descriptor != null;

        boolean trivial = enumEntry.getDeclarations().isEmpty();
        if (!trivial) {
            bindingTrace.record(ENUM_ENTRY_CLASS_NEED_SUBCLASS, descriptor);
            super.visitEnumEntry(enumEntry);
        }
        else {
            Type asmType = bindingTrace.get(ASM_TYPE, getOuterClassDescriptor());
            assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, descriptor, asmType);
            bindingTrace.record(ASM_TYPE, descriptor, asmType);
        }
    }

    @Override
    public void visitClassObject(@NotNull JetClassObject classObject) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, classObject.getObjectDeclaration());
        assert classDescriptor != null;

        String name = peekFromStack(nameStack) + JvmAbi.CLASS_OBJECT_SUFFIX;
        recordClosure(classObject, classDescriptor, name);

        pushClassDescriptor(classDescriptor);
        nameStack.push(name);
        super.visitClassObject(classObject);
        nameStack.pop();
        popClassDescriptor();
    }

    @Override
    public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
        if (declaration.getParent() instanceof JetObjectLiteralExpression || declaration.getParent() instanceof JetClassObject) {
            super.visitObjectDeclaration(declaration);
        }
        else {
            ClassDescriptor classDescriptor = bindingContext.get(CLASS, declaration);
            // working around a problem with shallow analysis
            if (classDescriptor == null) return;

            String name = getName(classDescriptor);
            recordClosure(declaration, classDescriptor, name);

            pushClassDescriptor(classDescriptor);
            nameStack.push(name);
            super.visitObjectDeclaration(declaration);
            nameStack.pop();
            popClassDescriptor();
        }
    }

    @Override
    public void visitClass(@NotNull JetClass klass) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, klass);
        // working around a problem with shallow analysis
        if (classDescriptor == null) return;

        String name = getName(classDescriptor);
        recordClosure(klass, classDescriptor, name);

        pushClassDescriptor(classDescriptor);
        nameStack.push(name);
        super.visitClass(klass);
        nameStack.pop();
        popClassDescriptor();
    }

    private String getName(ClassDescriptor classDescriptor) {
        String base = peekFromStack(nameStack);
        return DescriptorUtils.isTopLevelDeclaration(classDescriptor) ? base.isEmpty() ? classDescriptor.getName()
                        .asString() : base + '/' + classDescriptor.getName() : base + '$' + classDescriptor.getName();
    }

    @Override
    public void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, expression.getObjectDeclaration());
        if (classDescriptor == null) {
            // working around a problem with shallow analysis
            super.visitObjectLiteralExpression(expression);
            return;
        }

        String name = inventAnonymousClassName(expression.getObjectDeclaration());
        recordClosure(expression.getObjectDeclaration(), classDescriptor, name);

        pushClassDescriptor(classDescriptor);
        //noinspection ConstantConditions
        nameStack.push(bindingContext.get(ASM_TYPE, classDescriptor).getInternalName());
        super.visitObjectLiteralExpression(expression);
        nameStack.pop();
        popClassDescriptor();
    }

    @Override
    public void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        FunctionDescriptor functionDescriptor =
                (FunctionDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, functionLiteral);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        String name = inventAnonymousClassName(expression);
        JetType superType = getSuperTypeForClosure(functionDescriptor, false);
        ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, superType);
        recordClosure(functionLiteral, classDescriptor, name);

        pushClassDescriptor(classDescriptor);
        nameStack.push(name);
        super.visitFunctionLiteralExpression(expression);
        nameStack.pop();
        popClassDescriptor();
    }

    private void pushClassDescriptor(ClassDescriptor classDescriptor) {
        classStack.push(new ClassDescriptorWithState(classDescriptor));
    }

    private void popClassDescriptor() {
        classStack.pop();
    }

    private ClassDescriptor getOuterClassDescriptor() {
        ListIterator<ClassDescriptorWithState> iterator = classStack.listIterator(classStack.size());
        while(iterator.hasPrevious()) {
            ClassDescriptorWithState previous = iterator.previous();
            if (!previous.isDelegationToSuperCall()) {//find last not delegated to super call
                return previous.getDescriptor();
            }
        }
        return null;
    }

    @Override
    public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
        ClassDescriptorWithState state = peekFromStack(classStack);
        // working around a problem with shallow analysis
        if (state == null) return;
        state.setDelegationToSuperCall(true);
        super.visitDelegationToSuperCallSpecifier(call);
        state.setDelegationToSuperCall(false);
    }

    @Override
    public void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression) {
        FunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        ResolvedCall<? extends CallableDescriptor> referencedFunction =
                bindingContext.get(RESOLVED_CALL, expression.getCallableReference());
        if (referencedFunction == null) return;
        JetType superType = getSuperTypeForClosure((FunctionDescriptor) referencedFunction.getResultingDescriptor(), true);

        String name = inventAnonymousClassName(expression);
        ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, superType);
        recordClosure(expression, classDescriptor, name);

        pushClassDescriptor(classDescriptor);
        nameStack.push(name);
        super.visitCallableReferenceExpression(expression);
        nameStack.pop();
        popClassDescriptor();
    }


    private void recordClosure(
            @NotNull JetElement element,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull String name
    ) {
        CodegenBinding.recordClosure(bindingTrace, element, classDescriptor, getOuterClassDescriptor(),
                                     Type.getObjectType(name));
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        DeclarationDescriptor propertyDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, property);
        // working around a problem with shallow analysis
        if (propertyDescriptor == null) return;

        String nameForClassOrNamespaceMember = getNameForClassOrNamespaceMember(propertyDescriptor);
        if (nameForClassOrNamespaceMember != null) {
            nameStack.push(nameForClassOrNamespaceMember);
        }
        else {
            nameStack.push(peekFromStack(nameStack) + '$' + property.getName());
        }
        super.visitProperty(property);
        nameStack.pop();
    }

    @Override
    public void visitNamedFunction(@NotNull JetNamedFunction function) {
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, function);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        String nameForClassOrNamespaceMember = getNameForClassOrNamespaceMember(functionDescriptor);
        if (nameForClassOrNamespaceMember != null) {
            nameStack.push(nameForClassOrNamespaceMember);
            super.visitNamedFunction(function);
            nameStack.pop();
        }
        else {
            String name = inventAnonymousClassName(function);
            JetType superType = getSuperTypeForClosure(functionDescriptor, false);
            ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, superType);
            recordClosure(function, classDescriptor, name);

            pushClassDescriptor(classDescriptor);
            nameStack.push(name);
            super.visitNamedFunction(function);
            nameStack.pop();
            popClassDescriptor();
        }
    }

    @Nullable
    private String getNameForClassOrNamespaceMember(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        String peek = peekFromStack(nameStack);
        String name = descriptor.getName().asString();
        if (containingDeclaration instanceof ClassDescriptor) {
            return peek + '$' + name;
        }
        else if (containingDeclaration instanceof PackageFragmentDescriptor) {
            FqName qualifiedName = ((PackageFragmentDescriptor) containingDeclaration).getFqName();
            String packageClassShortName = PackageClassUtils.getPackageClassName(qualifiedName);
            String packageClassName = peek.isEmpty() ? packageClassShortName : peek + "/" + packageClassShortName;
            return packageClassName + '$' + name;
        }
        else {
            return null;
        }
    }

    @Override
    public void visitCallExpression(@NotNull JetCallExpression expression) {
        super.visitCallExpression(expression);
        ResolvedCall<? extends CallableDescriptor> call = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
        if (call == null) {
            return;
        }

        CallableDescriptor descriptor = call.getResultingDescriptor();
        if (!(descriptor instanceof FunctionDescriptor)) {
            return;
        }
        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter((FunctionDescriptor) descriptor);

        if (original == null) {
            return;
        }
        List<ResolvedValueArgument> valueArguments = call.getValueArgumentsByIndex();
        for (ValueParameterDescriptor valueParameter : original.getValueParameters()) {
            JavaClassDescriptor samInterface = getInterfaceIfSamType(valueParameter.getType());
            if (samInterface == null) {
                continue;
            }

            ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameter.getIndex());
            assert resolvedValueArgument instanceof ExpressionValueArgument : resolvedValueArgument;
            ValueArgument valueArgument = ((ExpressionValueArgument) resolvedValueArgument).getValueArgument();
            assert valueArgument != null;
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            assert argumentExpression != null : valueArgument.asElement().getText();

            bindingTrace.record(CodegenBinding.SAM_VALUE, argumentExpression, samInterface);
        }
    }

    @Override
    public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
        super.visitBinaryExpression(expression);

        FunctionDescriptor operationDescriptor =
                (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        if (operationDescriptor == null) return;

        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter(operationDescriptor);
        if (original == null) return;

        JavaClassDescriptor samInterfaceOfParameter = getInterfaceIfSamType(original.getValueParameters().get(0).getType());
        if (samInterfaceOfParameter == null) return;

        IElementType token = expression.getOperationToken();
        if (BINARY_OPERATIONS.contains(token)) {
            bindingTrace.record(CodegenBinding.SAM_VALUE, expression.getRight(), samInterfaceOfParameter);
        }
        else if (token == IN_KEYWORD || token == NOT_IN) {
            bindingTrace.record(CodegenBinding.SAM_VALUE, expression.getLeft(), samInterfaceOfParameter);
        }
    }

    @Override
    public void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression) {
        super.visitArrayAccessExpression(expression);

        FunctionDescriptor operationDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (operationDescriptor == null) {
            return;
        }

        boolean isSetter = operationDescriptor.getName().asString().equals("set");
        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter(operationDescriptor);
        if (original == null) {
            return;
        }

        List<JetExpression> indexExpressions = expression.getIndexExpressions();
        List<ValueParameterDescriptor> parameters = original.getValueParameters();
        for (ValueParameterDescriptor valueParameter : parameters) {
            JavaClassDescriptor samInterface = getInterfaceIfSamType(valueParameter.getType());
            if (samInterface == null) {
                continue;
            }

            if (isSetter && valueParameter.getIndex() == parameters.size() - 1) {
                PsiElement parent = expression.getParent();
                if (parent instanceof JetBinaryExpression && ((JetBinaryExpression) parent).getOperationToken() == EQ) {
                    JetExpression right = ((JetBinaryExpression) parent).getRight();
                    bindingTrace.record(CodegenBinding.SAM_VALUE, right, samInterface);
                }
            }
            else {
                JetExpression indexExpression = indexExpressions.get(valueParameter.getIndex());
                bindingTrace.record(CodegenBinding.SAM_VALUE, indexExpression, samInterface);
            }
        }
    }

    @Nullable
    private static JavaClassDescriptor getInterfaceIfSamType(@NotNull JetType originalType) {
        if (!SingleAbstractMethodUtils.isSamType(originalType)) {
            return null;
        }
        JavaClassDescriptor samInterface =
                (JavaClassDescriptor) originalType.getConstructor().getDeclarationDescriptor();
        assert samInterface != null;
        return samInterface;
    }
}
