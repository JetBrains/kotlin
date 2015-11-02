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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;
import kotlin.ArraysKt;
import kotlin.CollectionsKt;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.shorten.ShortenWaitingSetKt;
import org.jetbrains.kotlin.idea.core.PsiModificationUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignatureUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractorUtilKt;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.*;

public class JetFunctionCallUsage extends JetUsageInfo<KtCallElement> {
    private static final Comparator<Pair<KtElement, KtElement>>
            REVERSED_TEXT_OFFSET_COMPARATOR = new Comparator<Pair<KtElement, KtElement>>() {
        @Override
        public int compare(
                @NotNull Pair<KtElement, KtElement> p1,
                @NotNull Pair<KtElement, KtElement> p2
        ) {
            int offset1 = p1.getFirst().getTextRange().getStartOffset();
            int offset2 = p2.getFirst().getTextRange().getStartOffset();
            return offset1 < offset2 ? 1
                                     : offset1 > offset2 ? -1
                                                         : 0;
        }
    };

    private static final ShortenReferences.Options SHORTEN_ARGUMENTS_OPTIONS = new ShortenReferences.Options(true, true);

    private final JetCallableDefinitionUsage<?> callee;
    private final BindingContext context;
    private final ResolvedCall<? extends CallableDescriptor> resolvedCall;

    public JetFunctionCallUsage(@NotNull KtCallElement element, JetCallableDefinitionUsage callee) {
        super(element);
        this.callee = callee;
        this.context = ResolutionUtils.analyze(element, BodyResolveMode.FULL);
        this.resolvedCall = CallUtilKt.getResolvedCall(element, context);
    }

    @Override
    public boolean processUsage(@NotNull JetChangeInfo changeInfo, @NotNull KtCallElement element, @NotNull UsageInfo[] allUsages) {
        if (shouldSkipUsage(element)) return true;

        changeNameIfNeeded(changeInfo, element);

        if (element.getValueArgumentList() != null) {
            if (changeInfo.isParameterSetOrOrderChanged()) {
                updateArgumentsAndReceiver(changeInfo, element, allUsages);
            }
            else {
                changeArgumentNames(changeInfo, element);
            }
        }

        if (changeInfo.getNewParametersCount() == 0
            && element instanceof KtDelegatorToSuperCall) {
            KtEnumEntry enumEntry = PsiTreeUtil.getParentOfType(element, KtEnumEntry.class, true);
            if (enumEntry != null && enumEntry.getInitializerList() == element.getParent()) {
                PsiElement colon = enumEntry.getColon();
                KtInitializerList initializerList = enumEntry.getInitializerList();
                enumEntry.deleteChildRange(colon != null ? colon : initializerList, initializerList);
            }
        }

        return true;
    }

    private boolean shouldSkipUsage(KtCallElement element) {
        // TODO: We probable need more clever processing of invalid calls, but for now default to Java-like behaviour
        if (resolvedCall == null && !(element instanceof KtDelegatorToSuperCall)) return true;
        if (resolvedCall != null && !ArgumentMappingKt.isReallySuccess(resolvedCall)) {
            // TODO: investigate why arguments are not recorded for enum constructor call
            if (element instanceof KtDelegatorToSuperCall && element.getParent().getParent() instanceof KtEnumEntry) return false;
            for (ValueArgument valueArgument : resolvedCall.getCall().getValueArguments()) {
                if (!(resolvedCall.getArgumentMapping(valueArgument) instanceof ArgumentMatch)) return true;
            }
            Map<ValueParameterDescriptor, ResolvedValueArgument> arguments = resolvedCall.getValueArguments();
            for (ValueParameterDescriptor valueParameter : resolvedCall.getResultingDescriptor().getValueParameters()) {
                if (!arguments.containsKey(valueParameter)) return true;
            }
        }
        return false;
    }

    private boolean isPropertyJavaUsage() {
        PsiElement calleeElement = this.callee.getElement();
        return (calleeElement instanceof KtProperty || calleeElement instanceof KtParameter)
               && resolvedCall != null && resolvedCall.getResultingDescriptor() instanceof JavaMethodDescriptor;
    }

    protected void changeNameIfNeeded(JetChangeInfo changeInfo, KtCallElement element) {
        if (!changeInfo.isNameChanged()) return;

        KtExpression callee = element.getCalleeExpression();
        if (!(callee instanceof KtSimpleNameExpression)) return;

        String newName = changeInfo.getNewName();
        if (isPropertyJavaUsage()) {
            String currentName = ((KtSimpleNameExpression) callee).getReferencedName();
            if (JvmAbi.isGetterName(currentName)) newName = JvmAbi.getterName(newName);
            else if (JvmAbi.isSetterName(currentName)) newName = JvmAbi.setterName(newName);
        }

        callee.replace(KtPsiFactoryKt.KtPsiFactory(getProject()).createSimpleName(newName));
    }

    @Nullable
    private KtExpression getReceiverExpressionIfMatched(
            @NotNull ReceiverValue receiverValue,
            @NotNull DeclarationDescriptor originalDescriptor,
            @NotNull KtPsiFactory psiFactory
    ) {
        if (!receiverValue.exists()) return null;

        // Replace descriptor of extension function/property with descriptor of its receiver
        // to simplify checking against receiver value in the corresponding resolved call
        if (originalDescriptor instanceof CallableDescriptor && !(originalDescriptor instanceof ReceiverParameterDescriptor)) {
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) originalDescriptor).getExtensionReceiverParameter();
            if (receiverParameter == null) return null;
            originalDescriptor = receiverParameter;
        }

        boolean currentIsExtension = resolvedCall.getExtensionReceiver() == receiverValue;
        boolean originalIsExtension =
                originalDescriptor instanceof ReceiverParameterDescriptor &&
                ((ReceiverParameterDescriptor) originalDescriptor).getValue() instanceof ExtensionReceiver;
        if (currentIsExtension != originalIsExtension) return null;

        KotlinType originalType = originalDescriptor instanceof ReceiverParameterDescriptor
                               ? ((ReceiverParameterDescriptor) originalDescriptor).getType()
                               : originalDescriptor instanceof ClassDescriptor
                                 ? ((ClassDescriptor) originalDescriptor).getDefaultType()
                                 : null;
        if (originalType == null || !KotlinTypeChecker.DEFAULT.isSubtypeOf(receiverValue.getType(), originalType)) return null;

        return getReceiverExpression(receiverValue, psiFactory);
    }

    private boolean needSeparateVariable(@NotNull PsiElement element) {
        if (element instanceof KtConstantExpression) return false;
        if (element instanceof KtThisExpression) return false;
        if (element instanceof KtSimpleNameExpression) return false;

        //noinspection SuspiciousMethodCalls
        if (element instanceof KtBinaryExpression
            && OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(((KtBinaryExpression) element).getOperationToken())) return true;

        //noinspection SuspiciousMethodCalls
        if (element instanceof KtUnaryExpression
            && OperatorConventions.INCREMENT_OPERATIONS.contains(((KtUnaryExpression) element).getOperationToken())) return true;

        if (element instanceof KtCallExpression) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall =
                    CallUtilKt.getResolvedCall((KtCallExpression) element, context);
            return resolvedCall != null && resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor;
        }

        for (PsiElement child : element.getChildren()) {
            if (needSeparateVariable(child)) return true;
        }
        return false;
    }

    @NotNull
    private KtExpression substituteReferences(
            @NotNull KtExpression expression,
            @NotNull Map<PsiReference, DeclarationDescriptor> referenceMap,
            @NotNull final KtPsiFactory psiFactory
    ) {
        if (referenceMap.isEmpty() || resolvedCall == null) return expression;

        KtExpression newExpression = (KtExpression) expression.copy();

        Map<KtSimpleNameExpression, KtSimpleNameExpression> nameCounterpartMap =
                ExtractorUtilKt.createNameCounterpartMap(expression, newExpression);

        Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = resolvedCall.getValueArguments();

        List<Pair<KtElement, KtElement>> replacements = new ArrayList<Pair<KtElement, KtElement>>();
        for (Map.Entry<PsiReference, DeclarationDescriptor> e : referenceMap.entrySet()) {
            DeclarationDescriptor descriptor = e.getValue();

            KtExpression argumentExpression;
            boolean addReceiver = false;
            if (descriptor instanceof ValueParameterDescriptor) { // Ordinary parameter
                // Find corresponding parameter in the current function (may differ from 'descriptor' if original function is part of override hierarchy)
                ValueParameterDescriptor parameterDescriptor =
                        resolvedCall.getResultingDescriptor().getValueParameters().get(((ValueParameterDescriptor) descriptor).getIndex());

                ResolvedValueArgument resolvedValueArgument = valueArguments.get(parameterDescriptor);
                if (!(resolvedValueArgument instanceof ExpressionValueArgument)) continue;

                ValueArgument argument = ((ExpressionValueArgument) resolvedValueArgument).getValueArgument();
                if (argument == null) continue;

                argumentExpression = argument.getArgumentExpression();
            }
            else {
                addReceiver = !(descriptor instanceof ReceiverParameterDescriptor);
                argumentExpression = getReceiverExpressionIfMatched(resolvedCall.getExtensionReceiver(), descriptor, psiFactory);
                if (argumentExpression == null) {
                    argumentExpression = getReceiverExpressionIfMatched(resolvedCall.getDispatchReceiver(), descriptor, psiFactory);
                }
            }
            if (argumentExpression == null) continue;

            //noinspection unchecked
            if (needSeparateVariable(argumentExpression) &&
                PsiTreeUtil.getNonStrictParentOfType(getElement(),
                                                     KtConstructorDelegationCall.class,
                                                     KtDelegationSpecifier.class,
                                                     KtParameter.class) == null) {

                final Ref<KtExpression> newExpressionRef = new Ref<KtExpression>();
                KotlinIntroduceVariableHandler.doRefactoring(
                        getProject(),
                        null,
                        argumentExpression,
                        Collections.singletonList(argumentExpression),
                        new Function1<KtProperty, Unit>() {
                            @Override
                            public Unit invoke(KtProperty property) {
                                //noinspection ConstantConditions
                                newExpressionRef.set(psiFactory.createExpression(property.getName()));
                                return null;
                            }
                        }
                );
                argumentExpression = newExpressionRef.get();
            }

            //noinspection SuspiciousMethodCalls
            KtExpression expressionToReplace = nameCounterpartMap.get(e.getKey().getElement());
            if (expressionToReplace == null) continue;
            PsiElement parent = expressionToReplace.getParent();
            if (parent instanceof KtThisExpression) {
                expressionToReplace = (KtThisExpression) parent;
            }

            if (addReceiver) {
                KtCallExpression callExpression = PsiTreeUtil.getParentOfType(expressionToReplace, KtCallExpression.class, true);
                if (callExpression != null && PsiTreeUtil.isAncestor(callExpression.getCalleeExpression(), expressionToReplace, false)) {
                    expressionToReplace = callExpression;
                } else {
                    // Do not substitute operation references in infix/prefix calls
                    if (parent instanceof KtOperationExpression
                        && ((KtOperationExpression) parent).getOperationReference() == expressionToReplace) {
                        continue;
                    }
                }
                replacements.add(
                        new Pair<KtElement, KtElement>(
                                expressionToReplace,
                                psiFactory.createExpression(argumentExpression.getText() + "." + expressionToReplace.getText())
                        )
                );
            }
            else {
                replacements.add(new Pair<KtElement, KtElement>(expressionToReplace, argumentExpression));
            }
        }

        // Sort by descending offset so that call arguments are replaced before call itself
        ContainerUtil.sort(replacements, REVERSED_TEXT_OFFSET_COMPARATOR);
        for (Pair<KtElement, KtElement> replacement : replacements) {
            replacement.getFirst().replace(replacement.getSecond());
        }

        return newExpression;
    }

    private void updateArgumentsAndReceiver(JetChangeInfo changeInfo, KtCallElement element, @NotNull UsageInfo[] allUsages) {
        KtValueArgumentList arguments = element.getValueArgumentList();
        assert arguments != null : "Argument list is expected: " + element.getText();
        List<? extends ValueArgument> oldArguments = element.getValueArguments();

        if (isPropertyJavaUsage()) {
            updateJavaPropertyCall(changeInfo, element);
            return;
        }

        boolean isNamedCall = oldArguments.size() > 1 && oldArguments.get(0).isNamed();
        StringBuilder parametersBuilder = new StringBuilder("(");
        boolean isFirst = true;

        TIntArrayList indicesOfArgumentsWithDefaultValues = new TIntArrayList();

        KtPsiFactory psiFactory = new KtPsiFactory(element.getProject());

        List<JetParameterInfo> newSignatureParameters = changeInfo.getNonReceiverParameters();
        for (JetParameterInfo parameterInfo : newSignatureParameters) {
            if (isFirst) {
                isFirst = false;
            }
            else {
                parametersBuilder.append(',');
            }

            KtExpression defaultValueForCall = parameterInfo.getDefaultValueForCall();

            String defaultValueText;
            if (ChangeSignatureUtilsKt.isInsideOfCallerBody(element, allUsages)) {
                defaultValueText = parameterInfo.getName();
            }
            else {
                defaultValueText = defaultValueForCall != null
                                   ? substituteReferences(defaultValueForCall,
                                                          parameterInfo.getDefaultValueParameterReferences(),
                                                          psiFactory).getText()
                                   : "";
            }

            if (isNamedCall) {
                String newName = parameterInfo.getInheritedName(callee);
                parametersBuilder.append(newName).append('=');
            }

            parametersBuilder.append(defaultValueText.isEmpty() ? '0' : defaultValueText);
        }

        parametersBuilder.append(')');
        KtValueArgumentList newArgumentList = KtPsiFactoryKt.KtPsiFactory(getProject()).createCallArguments(parametersBuilder.toString());

        Map<Integer, ValueArgument> argumentMap = getParamIndexToArgumentMap(changeInfo, oldArguments);

        JetParameterInfo newReceiverInfo = changeInfo.getReceiverParameterInfo();
        JetParameterInfo originalReceiverInfo = changeInfo.getMethodDescriptor().getReceiver();

        ReceiverValue extensionReceiver = resolvedCall != null ? resolvedCall.getExtensionReceiver() : ReceiverValue.NO_RECEIVER;
        ReceiverValue dispatchReceiver = resolvedCall != null ? resolvedCall.getDispatchReceiver() : ReceiverValue.NO_RECEIVER;

        PsiElement elementToReplace = element;
        PsiElement parent = element.getParent();
        if (parent instanceof KtQualifiedExpression && ((KtQualifiedExpression) parent).getSelectorExpression() == element) {
            elementToReplace = parent;
        }

        // Do not add extension receiver to calls with explicit dispatch receiver
        if (newReceiverInfo != null
            && elementToReplace instanceof KtQualifiedExpression
            && dispatchReceiver instanceof ExpressionReceiver) return;

        List<KtValueArgument> newArguments = newArgumentList.getArguments();
        int actualIndex = 0;
        for (int i = 0; i < newArguments.size(); i++) {
            KtValueArgument newArgument = newArguments.get(i);
            JetParameterInfo parameterInfo = newSignatureParameters.get(i);
            if (parameterInfo == originalReceiverInfo) {
                KtExpression receiverExpression = getReceiverExpression(extensionReceiver, psiFactory);
                if (receiverExpression != null) {
                    newArgument.replace(receiverExpression);
                }
                actualIndex++;
                continue;
            }

            ValueArgument oldArgument = argumentMap.get(parameterInfo.getOldIndex());

            if (oldArgument != null) {
                ValueArgumentName argumentName = oldArgument.getArgumentName();
                KtSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;
                changeArgumentName(argumentNameExpression, parameterInfo);
                //noinspection ConstantConditions
                ValueArgument argumentReplacement = (ValueArgument) newArgument.replace(
                        oldArgument instanceof KtFunctionLiteralArgument
                        ? psiFactory.createArgument(oldArgument.getArgumentExpression(), null, false)
                        : oldArgument.asElement()
                );
                argumentMap.put(parameterInfo.getOldIndex(), argumentReplacement);
            }
            // TODO: process default arguments in the middle
            else if (parameterInfo.getDefaultValueForCall() == null) {
                if (parameterInfo.getDefaultValueForParameter() != null) {
                    newArgumentList.removeArgument(newArgument);
                }
                else {
                    newArgument.delete(); // keep space between commas
                }
            }
            else {
                indicesOfArgumentsWithDefaultValues.add(actualIndex++);
            }
        }

        List<KtFunctionLiteralArgument> lambdaArguments = element.getFunctionLiteralArguments();
        boolean hasLambdaArgumentsBefore = !lambdaArguments.isEmpty();
        if (hasLambdaArgumentsBefore) {
            element.deleteChildRange(CollectionsKt.first(lambdaArguments), CollectionsKt.last(lambdaArguments));
        }

        KtValueArgument lastArgument = CollectionsKt.lastOrNull(newArgumentList.getArguments());
        KtFunctionLiteralExpression lastLambdaExpr =
                lastArgument != null ? KtFunctionLiteralArgumentKt.unpackFunctionLiteral(lastArgument.getArgumentExpression()) : null;
        JetParameterInfo lastNewParam = ArraysKt.lastOrNull(changeInfo.getNewParameters());
        boolean hasTrailingLambdaInArgumentListAfter =
                lastLambdaExpr != null && lastNewParam != null && argumentMap.get(lastNewParam.getOldIndex()) == lastArgument;
        boolean newLambdaWithDefaultValueWasAdded =
                lastNewParam != null
                && lastNewParam.isNewParameter()
                && lastNewParam.getDefaultValueForCall() instanceof KtFunctionLiteralExpression
                && lastArgument != null
                && !lastArgument.isNamed();
        boolean shouldMoveLambdaOut =
                hasTrailingLambdaInArgumentListAfter && hasLambdaArgumentsBefore || newLambdaWithDefaultValueWasAdded;

        arguments = (KtValueArgumentList) arguments.replace(newArgumentList);

        final List<KtElement> argumentsToShorten = new ArrayList<KtElement>(indicesOfArgumentsWithDefaultValues.size());
        final List<KtValueArgument> argumentList = arguments.getArguments();
        indicesOfArgumentsWithDefaultValues.forEach(
                new TIntProcedure() {
                    @Override
                    public boolean execute(int i) {
                        argumentsToShorten.add(argumentList.get(i));
                        return true;
                    }
                }
        );

        for (KtElement argument : argumentsToShorten) {
            ShortenWaitingSetKt.addToShorteningWaitSet(argument, SHORTEN_ARGUMENTS_OPTIONS);
        }

        KtElement newElement = element;
        if (newReceiverInfo != originalReceiverInfo) {
            PsiElement replacingElement;
            if (newReceiverInfo != null) {
                ValueArgument receiverArgument = argumentMap.get(newReceiverInfo.getOldIndex());
                KtExpression extensionReceiverExpression = receiverArgument != null ? receiverArgument.getArgumentExpression() : null;
                KtExpression defaultValueForCall = newReceiverInfo.getDefaultValueForCall();
                KtExpression receiver =
                        extensionReceiverExpression != null ? psiFactory.createExpression(extensionReceiverExpression.getText())
                        : defaultValueForCall != null ? defaultValueForCall
                        : psiFactory.createExpression("_");

                replacingElement = CreateByPatternKt.createExpressionByPattern(psiFactory, "$0.$1", receiver, element);
            }
            else {
                replacingElement = psiFactory.createExpression(element.getText());
            }

            newElement = (KtElement) elementToReplace.replace(replacingElement);
        }

        if (shouldMoveLambdaOut) {
            KtCallExpression newCallExpression =
                    (KtCallExpression) (newElement instanceof KtQualifiedExpression
                                      ? ((KtQualifiedExpression) newElement).getSelectorExpression()
                                      : newElement);
            PsiModificationUtilsKt.moveFunctionLiteralOutsideParentheses(newCallExpression);
        }
    }

    private static void updateJavaPropertyCall(JetChangeInfo changeInfo, KtCallElement element) {
        JetParameterInfo newReceiverInfo = changeInfo.getReceiverParameterInfo();
        JetParameterInfo originalReceiverInfo = changeInfo.getMethodDescriptor().getReceiver();
        if (newReceiverInfo == originalReceiverInfo) return;

        KtValueArgumentList arguments = element.getValueArgumentList();
        assert arguments != null : "Argument list is expected: " + element.getText();
        List<? extends ValueArgument> oldArguments = element.getValueArguments();

        KtPsiFactory psiFactory = new KtPsiFactory(element.getProject());

        KtValueArgument firstArgument = oldArguments.isEmpty() ? null : (KtValueArgument) oldArguments.get(0);

        if (newReceiverInfo == null) {
            if (firstArgument != null) arguments.removeArgument(firstArgument);
        }
        else {
            KtExpression defaultValueForCall = newReceiverInfo.getDefaultValueForCall();
            if (defaultValueForCall == null) {
                defaultValueForCall = psiFactory.createExpression("_");
            }
            KtValueArgument newReceiverArgument = psiFactory.createArgument(defaultValueForCall, null, false);

            if (originalReceiverInfo != null) {
                if (firstArgument != null) firstArgument.replace(newReceiverArgument);
            }
            else {
                arguments.addArgumentAfter(newReceiverArgument, null);
            }
        }
    }

    @Nullable
    private static KtExpression getReceiverExpression(@NotNull ReceiverValue receiver, @NotNull KtPsiFactory psiFactory) {
        if (receiver instanceof ExpressionReceiver) {
            return ((ExpressionReceiver) receiver).getExpression();
        }
        else if (receiver instanceof ThisReceiver) {
            DeclarationDescriptor descriptor = ((ThisReceiver) receiver).getDeclarationDescriptor();
            String thisText = descriptor instanceof ClassDescriptor ? "this@" + descriptor.getName().asString() : "this";
            return psiFactory.createExpression(thisText);
        }
        return null;
    }

    private static Map<Integer, ValueArgument> getParamIndexToArgumentMap(JetChangeInfo changeInfo, List<? extends ValueArgument> oldArguments) {
        Map<Integer, ValueArgument> argumentMap = new HashMap<Integer, ValueArgument>();

        for (int i = 0; i < oldArguments.size(); i++) {
            ValueArgument argument = oldArguments.get(i);
            ValueArgumentName argumentName = argument.getArgumentName();
            String oldParameterName = argumentName != null ? argumentName.getAsName().asString() : null;

            if (oldParameterName != null) {
                Integer oldParameterIndex = changeInfo.getOldParameterIndex(oldParameterName);

                if (oldParameterIndex != null)
                    argumentMap.put(oldParameterIndex, argument);
            }
            else
                argumentMap.put(i, argument);
        }

        return argumentMap;
    }

    private void changeArgumentNames(JetChangeInfo changeInfo, KtCallElement element) {
        for (ValueArgument argument : element.getValueArguments()) {
            ValueArgumentName argumentName = argument.getArgumentName();
            KtSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;

            if (argumentNameExpression != null) {
                Integer oldParameterIndex = changeInfo.getOldParameterIndex(argumentNameExpression.getReferencedName());
                if (oldParameterIndex == null) continue;

                JetParameterInfo parameterInfo = changeInfo.getNewParameters()[
                        changeInfo.getReceiverParameterInfo() != null ? oldParameterIndex + 1 : oldParameterIndex
                ];
                changeArgumentName(argumentNameExpression, parameterInfo);
            }
        }
    }

    private void changeArgumentName(KtSimpleNameExpression argumentNameExpression, JetParameterInfo parameterInfo) {
        PsiElement identifier = argumentNameExpression != null ? argumentNameExpression.getIdentifier() : null;

        if (identifier != null) {
            String newName = parameterInfo.getInheritedName(callee);
            identifier.replace(KtPsiFactoryKt.KtPsiFactory(getProject()).createIdentifier(newName));
        }
    }
}
