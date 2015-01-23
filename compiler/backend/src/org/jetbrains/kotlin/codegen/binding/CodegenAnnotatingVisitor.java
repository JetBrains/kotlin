/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.binding;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.WhenChecker;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.JvmRuntimeTypes;
import org.jetbrains.kotlin.codegen.SamCodegenUtil;
import org.jetbrains.kotlin.codegen.SamType;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.when.SwitchCodegenUtil;
import org.jetbrains.kotlin.codegen.when.WhenByEnumsMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.EnumValue;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.source.SourcePackage;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.lexer.JetTokens.*;
import static org.jetbrains.kotlin.name.SpecialNames.safeIdentifier;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

class CodegenAnnotatingVisitor extends JetVisitorVoid {
    private static final TokenSet BINARY_OPERATIONS = TokenSet.orSet(
            AUGMENTED_ASSIGNMENTS,
            TokenSet.create(PLUS, MINUS, MUL, DIV, PERC, RANGE, LT, GT, LTEQ, GTEQ, IDENTIFIER)
    );

    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

    private final Stack<ClassDescriptor> classStack = new Stack<ClassDescriptor>();
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
    private ClassDescriptor recordClassForFunction(
            @NotNull JetElement element,
            @NotNull FunctionDescriptor funDescriptor,
            @NotNull Collection<JetType> supertypes,
            @NotNull String name
    ) {
        String simpleName = name.substring(name.lastIndexOf('/') + 1);
        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                correctContainerForLambda(funDescriptor, element),
                Name.special("<closure-" + simpleName + ">"),
                Modality.FINAL,
                supertypes,
                SourcePackage.toSourceElement(element)
        );
        classDescriptor.initialize(JetScope.Empty.INSTANCE$, Collections.<ConstructorDescriptor>emptySet(), null);

        bindingTrace.record(CLASS_FOR_FUNCTION, funDescriptor, classDescriptor);
        return classDescriptor;
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    private DeclarationDescriptor correctContainerForLambda(@NotNull FunctionDescriptor descriptor, @NotNull JetElement function) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();

        // In almost all cases the function's direct container is the correct container to consider in JVM back-end
        // (and subsequently to write to EnclosingMethod and InnerClasses attributes).
        // The only exceptional case is when a lambda is declared in the super call of an anonymous object:
        // in this case it's constructed in the outer code, despite being located under the object PSI- and descriptor-wise
        // TODO: consider the possibility of fixing this in the compiler front-end

        if (container instanceof ConstructorDescriptor && DescriptorUtils.isAnonymousObject(container.getContainingDeclaration())) {
            PsiElement element = function;
            while (element != null) {
                PsiElement child = element;
                element = element.getParent();

                if (bindingContext.get(DECLARATION_TO_DESCRIPTOR, element) == container) return container;

                if (element instanceof JetObjectDeclaration &&
                    element.getParent() instanceof JetObjectLiteralExpression &&
                    child instanceof JetDelegationSpecifierList) {
                    // If we're passing an anonymous object's super call, it means "container" is ConstructorDescriptor of that object.
                    // To reach outer context, we should call getContainingDeclaration() twice
                    // TODO: this is probably not entirely correct, mostly because DECLARATION_TO_DESCRIPTOR can return null
                    container = container.getContainingDeclaration().getContainingDeclaration();
                }
            }
        }

        return container;
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
            classStack.push(classDescriptor);
            //noinspection ConstantConditions
            nameStack.push(asmTypeForScriptPsi(bindingContext, file.getScript()).getInternalName());
        }
        else {
            nameStack.push(AsmUtil.internalNameByFqNameWithoutInnerClasses(file.getPackageFqName()));
        }
        file.acceptChildren(this);
        nameStack.pop();
        if (file.isScript()) {
            classStack.pop();
        }
    }

    @Override
    public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
        ClassDescriptor descriptor = bindingContext.get(CLASS, enumEntry);
        assert descriptor != null :
                String.format("No descriptor for enum entry \n---\n%s\n---\n", JetPsiUtil.getElementTextWithContext(enumEntry));

        if (enumEntry.getDeclarations().isEmpty()) {
            for (JetDelegationSpecifier specifier : enumEntry.getDelegationSpecifiers()) {
                specifier.accept(this);
            }
        }
        else {
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
        recordClosure(classDescriptor, name);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitClassObject(classObject);
        nameStack.pop();
        classStack.pop();
    }

    @Override
    public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
        if (declaration.getParent() instanceof JetClassObject) {
            super.visitObjectDeclaration(declaration);
        }
        else {
            if (!filter.shouldProcessClass(declaration)) return;

            ClassDescriptor classDescriptor = bindingContext.get(CLASS, declaration);
            // working around a problem with shallow analysis
            if (classDescriptor == null) return;

            String name = getName(classDescriptor);
            recordClosure(classDescriptor, name);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitObjectDeclaration(declaration);
            nameStack.pop();
            classStack.pop();
        }
    }

    @Override
    public void visitClass(@NotNull JetClass klass) {
        if (!filter.shouldProcessClass(klass)) return;

        ClassDescriptor classDescriptor = bindingContext.get(CLASS, klass);
        // working around a problem with shallow analysis
        if (classDescriptor == null) return;

        String name = getName(classDescriptor);
        recordClosure(classDescriptor, name);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitClass(klass);
        nameStack.pop();
        classStack.pop();
    }

    private String getName(ClassDescriptor classDescriptor) {
        String base = peekFromStack(nameStack);
        Name descriptorName = safeIdentifier(classDescriptor.getName());
        return DescriptorUtils.isTopLevelDeclaration(classDescriptor) ? base.isEmpty() ? descriptorName
                        .asString() : base + '/' + descriptorName : base + '$' + descriptorName;
    }

    @Override
    public void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression) {
        JetObjectDeclaration object = expression.getObjectDeclaration();
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, object);
        if (classDescriptor == null) {
            // working around a problem with shallow analysis
            super.visitObjectLiteralExpression(expression);
            return;
        }

        String name = inventAnonymousClassName(object);
        recordClosure(classDescriptor, name);

        JetDelegationSpecifierList delegationSpecifierList = object.getDelegationSpecifierList();
        if (delegationSpecifierList != null) {
            delegationSpecifierList.accept(this);
        }

        classStack.push(classDescriptor);
        nameStack.push(CodegenBinding.getAsmType(bindingContext, classDescriptor).getInternalName());
        JetClassBody body = object.getBody();
        if (body != null) {
            super.visitClassBody(body);
        }
        nameStack.pop();
        classStack.pop();
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
        ClassDescriptor classDescriptor = recordClassForFunction(functionLiteral, functionDescriptor, supertypes, name);
        recordClosure(classDescriptor, name);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitFunctionLiteralExpression(expression);
        nameStack.pop();
        classStack.pop();
    }

    @Override
    public void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression) {
        FunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        ResolvedCall<?> referencedFunction = CallUtilPackage.getResolvedCall(expression.getCallableReference(), bindingContext);
        if (referencedFunction == null) return;
        Collection<JetType> supertypes =
                runtimeTypes.getSupertypesForFunctionReference((FunctionDescriptor) referencedFunction.getResultingDescriptor());

        String name = inventAnonymousClassName(expression);
        ClassDescriptor classDescriptor = recordClassForFunction(expression, functionDescriptor, supertypes, name);
        recordClosure(classDescriptor, name);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitCallableReferenceExpression(expression);
        nameStack.pop();
        classStack.pop();
    }

    private void recordClosure(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        CodegenBinding.recordClosure(bindingTrace, classDescriptor, peekFromStack(classStack), Type.getObjectType(name));
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
            ClassDescriptor classDescriptor = recordClassForFunction(function, functionDescriptor, supertypes, name);
            recordClosure(classDescriptor, name);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitNamedFunction(function);
            nameStack.pop();
            classStack.pop();
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
            JetFile containingFile = DescriptorToSourceUtils.getContainingFile(descriptor);
            assert containingFile != null : "File not found for " + descriptor;
            return PackagePartClassUtils.getPackagePartInternalName(containingFile) + '$' + name;
        }

        return null;
    }

    @Override
    public void visitCallExpression(@NotNull JetCallExpression expression) {
        super.visitCallExpression(expression);
        ResolvedCall<?> call = CallUtilPackage.getResolvedCall(expression, bindingContext);
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

    @Override
    public void visitWhenExpression(@NotNull JetWhenExpression expression) {
        super.visitWhenExpression(expression);
        if (!isWhenWithEnums(expression)) return;

        String currentClassName = getCurrentTopLevelClassOrPackagePartInternalName(expression.getContainingJetFile());

        if (bindingContext.get(MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE, currentClassName) == null) {
            bindingTrace.record(MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE, currentClassName, new ArrayList<WhenByEnumsMapping>(1));
        }

        List<WhenByEnumsMapping> mappings = bindingContext.get(MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE, currentClassName);
        assert mappings != null : "guaranteed by contract";

        int fieldNumber = mappings.size();

        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getSubjectExpression());
        assert type != null : "should not be null in a valid when by enums";
        ClassDescriptor classDescriptor = (ClassDescriptor) type.getConstructor().getDeclarationDescriptor();
        assert classDescriptor != null : "because it's enum";

        WhenByEnumsMapping mapping = new WhenByEnumsMapping(classDescriptor, currentClassName, fieldNumber);

        for (CompileTimeConstant constant : SwitchCodegenUtil.getAllConstants(expression, bindingContext)) {
            if (constant instanceof NullValue) continue;

            assert constant instanceof EnumValue : "expression in when should be EnumValue";
            mapping.putFirstTime((EnumValue) constant, mapping.size() + 1);
        }

        mappings.add(mapping);

        bindingTrace.record(MAPPING_FOR_WHEN_BY_ENUM, expression, mapping);
    }

    private boolean isWhenWithEnums(@NotNull JetWhenExpression expression) {
        return WhenChecker.isWhenByEnum(expression, bindingContext) &&
               SwitchCodegenUtil.checkAllItemsAreConstantsSatisfying(
                       expression,
                       bindingContext,
                       new Function1<CompileTimeConstant, Boolean>() {
                           @Override
                           public Boolean invoke(@NotNull CompileTimeConstant constant) {
                               return constant instanceof EnumValue || constant instanceof NullValue;
                           }
                       }
               );
    }

    @NotNull
    private String getCurrentTopLevelClassOrPackagePartInternalName(@NotNull JetFile file) {
        ListIterator<ClassDescriptor> iterator = classStack.listIterator(classStack.size());
        while (iterator.hasPrevious()) {
            ClassDescriptor previous = iterator.previous();
            if (DescriptorUtils.isTopLevelOrInnerClass(previous)) {
                return CodegenBinding.getAsmType(bindingContext, previous).getInternalName();
            }
        }

        return PackagePartClassUtils.getPackagePartInternalName(file);
    }

    private static <T> T peekFromStack(@NotNull Stack<T> stack) {
        return stack.empty() ? null : stack.peek();
    }
}
