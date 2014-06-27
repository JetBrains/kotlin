/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.JvmRuntimeTypes;
import org.jetbrains.jet.codegen.SamCodegenUtil;
import org.jetbrains.jet.codegen.SamType;
import org.jetbrains.jet.codegen.state.GenerationState;
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
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.jet.codegen.JvmCodegenUtil.peekFromStack;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.safeIdentifier;
import static org.jetbrains.jet.lexer.JetTokens.*;

class CodegenAnnotatingVisitor extends JetVisitorVoid {
    private static final TokenSet BINARY_OPERATIONS = TokenSet.orSet(
            AUGMENTED_ASSIGNMENTS,
            TokenSet.create(PLUS, MINUS, MUL, DIV, PERC, RANGE, LT, GT, LTEQ, GTEQ, IDENTIFIER));

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
    private final GenerationState.GenerateClassFilter filter;
    private final JvmRuntimeTypes runtimeTypes;

    public CodegenAnnotatingVisitor(@NotNull GenerationState state) {
        this.bindingTrace = state.getBindingTrace();
        this.bindingContext = state.getBindingContext();
        this.filter = state.getGenerateDeclaredClassFilter();
        this.runtimeTypes = state.getJvmRuntimeTypes();
    }

    @NotNull
    private ClassDescriptor recordClassForFunction(@NotNull FunctionDescriptor funDescriptor, @NotNull Collection<JetType> supertypes) {
        ClassDescriptorImpl classDescriptor =
                new ClassDescriptorImpl(funDescriptor.getContainingDeclaration(), Name.special("<closure>"), Modality.FINAL, supertypes);
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
            // SCRIPT: should be replaced with VisitScript override
            //noinspection ConstantConditions
            ClassDescriptor classDescriptor = bindingContext.get(CLASS_FOR_SCRIPT, bindingContext.get(SCRIPT, file.getScript()));
            pushClassDescriptor(classDescriptor);
            //noinspection ConstantConditions
            nameStack.push(asmTypeForScriptPsi(bindingContext, file.getScript()).getInternalName());
        }
        else {
            nameStack.push(AsmUtil.internalNameByFqNameWithoutInnerClasses(file.getPackageFqName()));
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
        assert descriptor != null :
                String.format("No descriptor for enum entry \n---\n%s\n---\n", JetPsiUtil.getElementTextWithContext(enumEntry));

        if (!enumEntry.getDeclarations().isEmpty()) {
            bindingTrace.record(ENUM_ENTRY_CLASS_NEED_SUBCLASS, descriptor);
            super.visitEnumEntry(enumEntry);
        }
    }

    @Override
    public void visitClassObject(@NotNull JetClassObject classObject) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, classObject.getObjectDeclaration());

        assert classDescriptor != null : String.format("No class found in binding context for: \n---\n%s\n---\n",
                                                       JetPsiUtil.getElementTextWithContext(classObject));

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
            if (!filter.shouldProcess(declaration)) return;

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
        if (!filter.shouldProcess(klass)) return;

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
        Name descriptorName = safeIdentifier(classDescriptor.getName());
        return DescriptorUtils.isTopLevelDeclaration(classDescriptor) ? base.isEmpty() ? descriptorName
                        .asString() : base + '/' + descriptorName : base + '$' + descriptorName;
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
        Collection<JetType> supertypes = runtimeTypes.getSupertypesForClosure(functionDescriptor);
        ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, supertypes);
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

        ResolvedCall<?> referencedFunction = bindingContext.get(RESOLVED_CALL, expression.getCallableReference());
        if (referencedFunction == null) return;
        Collection<JetType> supertypes =
                runtimeTypes.getSupertypesForFunctionReference((FunctionDescriptor) referencedFunction.getResultingDescriptor());

        String name = inventAnonymousClassName(expression);
        ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, supertypes);
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
        CodegenBinding.recordClosure(bindingTrace, element, classDescriptor, getOuterClassDescriptor(), Type.getObjectType(name));
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        DeclarationDescriptor propertyDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, property);
        // working around a problem with shallow analysis
        if (propertyDescriptor == null) return;

        String nameForClassOrPackageMember = getNameForClassOrPackageMember(propertyDescriptor);
        if (nameForClassOrPackageMember != null) {
            nameStack.push(nameForClassOrPackageMember);
        }
        else {
            nameStack.push(peekFromStack(nameStack) + '$' + safeIdentifier(property.getNameAsSafeName()).asString());
        }
        super.visitProperty(property);
        nameStack.pop();
    }

    @Override
    public void visitNamedFunction(@NotNull JetNamedFunction function) {
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, function);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        String nameForClassOrPackageMember = getNameForClassOrPackageMember(functionDescriptor);
        if (nameForClassOrPackageMember != null) {
            nameStack.push(nameForClassOrPackageMember);
            super.visitNamedFunction(function);
            nameStack.pop();
        }
        else {
            String name = inventAnonymousClassName(function);
            Collection<JetType> supertypes = runtimeTypes.getSupertypesForClosure(functionDescriptor);
            ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor, supertypes);
            recordClosure(function, classDescriptor, name);

            pushClassDescriptor(classDescriptor);
            nameStack.push(name);
            super.visitNamedFunction(function);
            nameStack.pop();
            popClassDescriptor();
        }
    }

    @Nullable
    private String getNameForClassOrPackageMember(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        String peek = peekFromStack(nameStack);
        String name = safeIdentifier(descriptor.getName()).asString();
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
        ResolvedCall<?> call = bindingContext.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
        if (call == null) return;

        CallableDescriptor descriptor = call.getResultingDescriptor();
        if (!(descriptor instanceof FunctionDescriptor)) return;

        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter((FunctionDescriptor) descriptor);
        if (original == null) return;

        List<ResolvedValueArgument> valueArguments = call.getValueArgumentsByIndex();
        if (valueArguments == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + descriptor);
        }
        for (ValueParameterDescriptor valueParameter : original.getValueParameters()) {
            SamType samType = SamType.create(valueParameter.getType());
            if (samType == null) continue;

            ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameter.getIndex());
            assert resolvedValueArgument instanceof ExpressionValueArgument : resolvedValueArgument;
            ValueArgument valueArgument = ((ExpressionValueArgument) resolvedValueArgument).getValueArgument();
            assert valueArgument != null;
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            assert argumentExpression != null : valueArgument.asElement().getText();

            bindingTrace.record(CodegenBinding.SAM_VALUE, argumentExpression, samType);
        }
    }

    @Override
    public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
        super.visitBinaryExpression(expression);

        DeclarationDescriptor operationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
        if (!(operationDescriptor instanceof FunctionDescriptor)) return;

        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter((FunctionDescriptor) operationDescriptor);
        if (original == null) return;

        SamType samType = SamType.create(original.getValueParameters().get(0).getType());
        if (samType == null) return;

        IElementType token = expression.getOperationToken();
        if (BINARY_OPERATIONS.contains(token)) {
            bindingTrace.record(CodegenBinding.SAM_VALUE, expression.getRight(), samType);
        }
        else if (token == IN_KEYWORD || token == NOT_IN) {
            bindingTrace.record(CodegenBinding.SAM_VALUE, expression.getLeft(), samType);
        }
    }

    @Override
    public void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression) {
        super.visitArrayAccessExpression(expression);

        DeclarationDescriptor operationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (!(operationDescriptor instanceof FunctionDescriptor)) return;

        boolean isSetter = operationDescriptor.getName().asString().equals("set");
        FunctionDescriptor original = SamCodegenUtil.getOriginalIfSamAdapter((FunctionDescriptor) operationDescriptor);
        if (original == null) return;

        List<JetExpression> indexExpressions = expression.getIndexExpressions();
        List<ValueParameterDescriptor> parameters = original.getValueParameters();
        for (ValueParameterDescriptor valueParameter : parameters) {
            SamType samType = SamType.create(valueParameter.getType());
            if (samType == null) continue;

            if (isSetter && valueParameter.getIndex() == parameters.size() - 1) {
                PsiElement parent = expression.getParent();
                if (parent instanceof JetBinaryExpression && ((JetBinaryExpression) parent).getOperationToken() == EQ) {
                    JetExpression right = ((JetBinaryExpression) parent).getRight();
                    bindingTrace.record(CodegenBinding.SAM_VALUE, right, samType);
                }
            }
            else {
                JetExpression indexExpression = indexExpressions.get(valueParameter.getIndex());
                bindingTrace.record(CodegenBinding.SAM_VALUE, indexExpression, samType);
            }
        }
    }
}
