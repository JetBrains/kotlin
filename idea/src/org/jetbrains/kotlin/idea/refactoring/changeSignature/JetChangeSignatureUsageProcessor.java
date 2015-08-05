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

package org.jetbrains.kotlin.idea.refactoring.changeSignature;

import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.changeSignature.*;
import com.intellij.refactoring.rename.ResolveSnapshotProvider;
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.MultiMap;
import kotlin.KotlinPackage;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalyzerPackage;
import org.jetbrains.kotlin.asJava.AsJavaPackage;
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver;
import org.jetbrains.kotlin.idea.core.refactoring.RefactoringPackage;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.*;
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference;
import org.jetbrains.kotlin.idea.references.ReferencesPackage;
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchPackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.*;

public class JetChangeSignatureUsageProcessor implements ChangeSignatureUsageProcessor {
    // This is special 'PsiElement' whose purpose is to wrap JetMethodDescriptor so that it can be kept in the usage list
    private static class OriginalJavaMethodDescriptorWrapper extends UsageInfo {
        JetMethodDescriptor originalJavaMethodDescriptor;

        public OriginalJavaMethodDescriptorWrapper(@NotNull PsiElement element) {
            super(element);
        }
    }

    private static class DummyJetChangeInfo extends JetChangeInfo {
        public DummyJetChangeInfo(
                @NotNull PsiElement method,
                @NotNull JetMethodDescriptor methodDescriptor
        ) {
            super(methodDescriptor,
                  "",
                  null,
                  "",
                  Visibilities.INTERNAL,
                  Collections.<JetParameterInfo>emptyList(),
                  null,
                  method,
                  Collections.<PsiElement>emptyList());
        }
    }

    @Nullable
    private static OriginalJavaMethodDescriptorWrapper getOriginalJavaMethodDescriptorWrapper(@NotNull UsageInfo[] usages) {
        return KotlinPackage.firstOrNull(KotlinPackage.filterIsInstance(usages, OriginalJavaMethodDescriptorWrapper.class));
    }

    // It's here to prevent O(usage_count^2) performance
    private boolean initializedOriginalDescriptor;

    @Override
    public UsageInfo[] findUsages(ChangeInfo info) {
        initializedOriginalDescriptor = false;

        Set<UsageInfo> result = new HashSet<UsageInfo>();

        result.add(new OriginalJavaMethodDescriptorWrapper(info.getMethod()));

        if (info instanceof JetChangeInfo) {
            findAllMethodUsages((JetChangeInfo) info, result);
        }
        else {
            findSAMUsages(info, result);
            findConstructorDelegationUsages(info, result);
            findKotlinOverrides(info, result);
            if (info instanceof JavaChangeInfo) {
                findKotlinCallers((JavaChangeInfo) info, result);
            }
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    private static void findAllMethodUsages(JetChangeInfo changeInfo, Set<UsageInfo> result) {
        for (UsageInfo functionUsageInfo : ChangeSignaturePackage.getAffectedCallables(changeInfo)) {
            if (functionUsageInfo instanceof JetCallableDefinitionUsage) {
                findOneMethodUsages((JetCallableDefinitionUsage) functionUsageInfo, changeInfo, result);
            }
            else if (functionUsageInfo instanceof KotlinCallerUsage) {
                findCallerUsages((KotlinCallerUsage) functionUsageInfo, changeInfo, result);
            }
            else {
                result.add(functionUsageInfo);

                boolean propagationTarget = functionUsageInfo instanceof CallerUsageInfo
                                            || (functionUsageInfo instanceof OverriderUsageInfo
                                                && !((OverriderUsageInfo) functionUsageInfo).isOriginalOverrider());

                PsiElement callee = functionUsageInfo.getElement();
                if (callee == null) continue;

                SearchScope scope = callee.getUseScope();
                if (scope instanceof GlobalSearchScope) {
                    scope = GlobalSearchScope.getScopeRestrictedByFileTypes((GlobalSearchScope) scope, JetFileType.INSTANCE);
                }

                for (PsiReference reference : ReferencesSearch.search(callee, scope)) {
                    PsiElement element = reference.getElement();
                    JetCallElement callElement = PsiTreeUtil.getParentOfType(element, JetCallElement.class);
                    JetExpression calleeExpression = callElement != null ? callElement.getCalleeExpression() : null;
                    if (calleeExpression != null && PsiTreeUtil.isAncestor(calleeExpression, element, false)) {
                        result.add(propagationTarget
                                   ? new KotlinCallerCallUsage(callElement)
                                   : new JetFunctionCallUsage(callElement, changeInfo.getMethodDescriptor().getOriginalPrimaryCallable()));
                    }
                }
            }
        }
    }

    private static void findCallerUsages(KotlinCallerUsage callerUsage, JetChangeInfo changeInfo, final Set<UsageInfo> result) {
        result.add(callerUsage);

        JetNamedDeclaration element = callerUsage.getElement();
        if (element == null) return;

        for (PsiReference ref : ReferencesSearch.search(element, element.getUseScope())) {
            PsiElement refElement = ref.getElement();
            JetCallElement callElement = PsiTreeUtil.getParentOfType(refElement, JetCallElement.class);
            if (callElement != null && PsiTreeUtil.isAncestor(callElement.getCalleeExpression(), refElement, false)) {
                result.add(new KotlinCallerCallUsage(callElement));
            }
        }

        JetElement body = ChangeSignaturePackage.getDeclarationBody(element);
        final Set<String> newParameterNames = KotlinPackage.mapTo(
                changeInfo.getNonReceiverParameters(),
                new HashSet<String>(),
                new Function1<JetParameterInfo, String>() {
                    @Override
                    public String invoke(JetParameterInfo info) {
                        return info.getName();
                    }
                }
        );
        if (body != null) {
            final DeclarationDescriptor callerDescriptor = ResolvePackage.resolveToDescriptor(element);
            final BindingContext context = ResolvePackage.analyze(body);
            body.accept(
                    new JetTreeVisitorVoid() {
                        @Override
                        public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
                            final String currentName = expression.getReferencedName();
                            if (!newParameterNames.contains(currentName)) return;

                            ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilPackage.getResolvedCall(expression, context);
                            if (resolvedCall == null) return;

                            if (resolvedCall.getExplicitReceiverKind() != ExplicitReceiverKind.NO_EXPLICIT_RECEIVER) return;

                            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
                            if (!(resultingDescriptor instanceof VariableDescriptor)) return;

                            // Do not report usages of duplicated parameter
                            if (resultingDescriptor instanceof ValueParameterDescriptor
                                && resultingDescriptor.getContainingDeclaration() == callerDescriptor) return;

                            JetElement callElement = resolvedCall.getCall().getCallElement();

                            ReceiverValue receiver = resolvedCall.getExtensionReceiver();
                            if (!(receiver instanceof ThisReceiver)) {
                                receiver = resolvedCall.getDispatchReceiver();
                            }
                            if (receiver instanceof ThisReceiver) {
                                result.add(new JetImplicitThisUsage(callElement, ((ThisReceiver) receiver).getDeclarationDescriptor()));
                            }
                            else if (!receiver.exists()) {
                                result.add(
                                        new UnresolvableCollisionUsageInfo(callElement, null) {
                                            @Override
                                            public String getDescription() {
                                                return "There is already a variable '" + currentName + "' in " +
                                                       IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callerDescriptor) +
                                                       ". It will conflict with the new parameter.";
                                            }
                                        }
                                );
                            }
                        }
                    }
            );
        }
    }

    private static void findOneMethodUsages(
            @NotNull JetCallableDefinitionUsage<?> functionUsageInfo,
            final JetChangeInfo changeInfo,
            final Set<UsageInfo> result
    ) {
        boolean isInherited = functionUsageInfo.isInherited();

        if (isInherited) {
            result.add(functionUsageInfo);
        }

        PsiElement functionPsi = functionUsageInfo.getElement();
        if (functionPsi == null) return;

        for (PsiReference reference : ReferencesSearch.search(functionPsi, functionPsi.getUseScope())) {
            PsiElement element = reference.getElement();

            if (functionPsi instanceof JetClass && reference.resolve() != functionPsi) continue;

            if (element instanceof JetReferenceExpression) {
                PsiElement parent = element.getParent();

                if (parent instanceof JetCallExpression)
                    result.add(new JetFunctionCallUsage((JetCallExpression) parent, functionUsageInfo));
                else if (parent instanceof JetUserType && parent.getParent() instanceof JetTypeReference) {
                    parent = parent.getParent().getParent();

                    if (parent instanceof JetConstructorCalleeExpression && parent.getParent() instanceof JetDelegatorToSuperCall)
                        result.add(new JetFunctionCallUsage((JetDelegatorToSuperCall)parent.getParent(), functionUsageInfo));
                }
                else if (element instanceof JetSimpleNameExpression
                         && (functionPsi instanceof JetProperty || functionPsi instanceof JetParameter)) {
                    result.add(new JetPropertyCallUsage((JetSimpleNameExpression) element));
                }
            }
        }

        String oldName = ChangeSignaturePackage.getOldName(changeInfo);

        if (oldName != null)
            TextOccurrencesUtil.findNonCodeUsages(functionPsi, oldName, true, true, changeInfo.getNewName(), result);

        List<JetParameter> oldParameters = PsiUtilPackage.getValueParameters((JetNamedDeclaration) functionPsi);

        JetParameterInfo newReceiverInfo = changeInfo.getReceiverParameterInfo();

        for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
            if (parameterInfo.getOldIndex() >= 0 && parameterInfo.getOldIndex() < oldParameters.size()) {
                JetParameter oldParam = oldParameters.get(parameterInfo.getOldIndex());
                String oldParamName = oldParam.getName();

                if (parameterInfo == newReceiverInfo || (oldParamName != null && !oldParamName.equals(parameterInfo.getName()))) {
                    for (PsiReference reference : ReferencesSearch.search(oldParam, oldParam.getUseScope())) {
                        PsiElement element = reference.getElement();

                        if ((element instanceof JetSimpleNameExpression || element instanceof KDocName) &&
                            !(element.getParent() instanceof JetValueArgumentName)) // Usages in named arguments of the calls usage will be changed when the function call is changed
                        {
                            JetParameterUsage parameterUsage =
                                    new JetParameterUsage((JetElement) element, parameterInfo, functionUsageInfo);
                            result.add(parameterUsage);
                        }
                    }
                }
            }
        }

        if (functionPsi instanceof JetFunction && newReceiverInfo != changeInfo.getMethodDescriptor().getReceiver()) {
            findOriginalReceiversUsages(functionUsageInfo, result, changeInfo);
        }

        if (functionPsi instanceof JetClass && ((JetClass) functionPsi).isEnum()) {
            for (JetDeclaration declaration : ((JetClass) functionPsi).getDeclarations()) {
                if (declaration instanceof JetEnumEntry && ((JetEnumEntry) declaration).getDelegationSpecifiers().isEmpty()) {
                    result.add(new JetEnumEntryWithoutSuperCallUsage((JetEnumEntry) declaration));
                }
            }
        }

        UsagesSearchPackage.processDelegationCallConstructorUsages(
                functionPsi,
                functionPsi.getUseScope(),
                new Function1<JetConstructorDelegationCall, Unit>() {
                    @Override
                    public Unit invoke(JetConstructorDelegationCall element) {
                        result.add(new JetConstructorDelegationCallUsage(element, changeInfo));
                        return null;
                    }
                }
        );
    }

    private static void processInternalReferences(
            JetCallableDefinitionUsage functionUsageInfo,
            JetTreeVisitor<BindingContext> visitor
    ) {
        JetFunction jetFunction = (JetFunction) functionUsageInfo.getDeclaration();
        JetExpression body = jetFunction.getBodyExpression();
        if (body != null) {
            body.accept(visitor, ResolvePackage.analyze(body, BodyResolveMode.FULL));
        }
        for (JetParameter parameter : jetFunction.getValueParameters()) {
            JetExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                defaultValue.accept(visitor, ResolvePackage.analyze(defaultValue, BodyResolveMode.FULL));
            }
        }
    }

    private static void findOriginalReceiversUsages(
            @NotNull final JetCallableDefinitionUsage<?> functionUsageInfo,
            @NotNull final Set<UsageInfo> result,
            @NotNull final JetChangeInfo changeInfo
    ) {
        final JetParameterInfo originalReceiverInfo = changeInfo.getMethodDescriptor().getReceiver();
        final CallableDescriptor callableDescriptor = functionUsageInfo.getOriginalCallableDescriptor();
        processInternalReferences(
                functionUsageInfo,
                new JetTreeVisitor<BindingContext>() {
                    private void processExplicitThis(
                            @NotNull JetSimpleNameExpression expression,
                            @NotNull ReceiverParameterDescriptor receiverDescriptor
                    ) {
                        if (originalReceiverInfo != null && !changeInfo.hasParameter(originalReceiverInfo)) return;
                        if (!(expression.getParent() instanceof JetThisExpression)) return;

                        if (receiverDescriptor == callableDescriptor.getExtensionReceiverParameter()) {
                            assert originalReceiverInfo != null : "No original receiver info provided: " + functionUsageInfo.getDeclaration().getText();
                            result.add(new JetParameterUsage(expression, originalReceiverInfo, functionUsageInfo));
                        }
                        else {
                            ClassifierDescriptor targetDescriptor = receiverDescriptor.getType().getConstructor().getDeclarationDescriptor();
                            assert targetDescriptor != null : "Receiver type has no descriptor: " + functionUsageInfo.getDeclaration().getText();
                            result.add(new JetNonQualifiedOuterThisUsage((JetThisExpression) expression.getParent(), targetDescriptor));
                        }
                    }

                    private void processImplicitThis(
                            @NotNull JetElement callElement,
                            @NotNull ThisReceiver receiverValue
                    ) {
                        DeclarationDescriptor targetDescriptor = receiverValue.getDeclarationDescriptor();
                        if (targetDescriptor == callableDescriptor) {
                            assert originalReceiverInfo != null : "No original receiver info provided: " + functionUsageInfo.getDeclaration().getText();
                            result.add(new JetImplicitThisToParameterUsage(callElement, originalReceiverInfo, functionUsageInfo));
                        }
                        else {
                            result.add(new JetImplicitThisUsage(callElement, targetDescriptor));
                        }
                    }

                    @Override
                    public Void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, BindingContext context) {
                        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilPackage.getResolvedCall(expression, context);
                        if (resolvedCall == null) return null;

                        CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
                        if (resultingDescriptor instanceof ReceiverParameterDescriptor) {
                            processExplicitThis(expression, (ReceiverParameterDescriptor) resultingDescriptor);
                            return null;
                        }

                        ReceiverValue receiverValue = resolvedCall.getExtensionReceiver();
                        if (!receiverValue.exists()) {
                            receiverValue = resolvedCall.getDispatchReceiver();
                        }
                        if (receiverValue instanceof ThisReceiver) {
                            processImplicitThis(resolvedCall.getCall().getCallElement(), (ThisReceiver) receiverValue);
                        }

                        return null;
                    }
                }
        );
    }

    private static void findSAMUsages(ChangeInfo changeInfo, Set<UsageInfo> result) {
        PsiElement method = changeInfo.getMethod();
        if (!RefactoringPackage.isTrueJavaMethod(method)) return;

        FunctionDescriptor methodDescriptor = ResolvePackage.getJavaMethodDescriptor((PsiMethod) method);
        assert methodDescriptor != null;

        DeclarationDescriptor containingDescriptor = methodDescriptor.getContainingDeclaration();
        if (!(containingDescriptor instanceof JavaClassDescriptor)) return;

        if (((JavaClassDescriptor) containingDescriptor).getFunctionTypeForSamInterface() == null) return;

        PsiClass samClass = ((PsiMethod) method).getContainingClass();
        if (samClass == null) return;

        for (PsiReference ref : ReferencesSearch.search(samClass)) {
            if (!(ref instanceof JetSimpleNameReference)) continue;

            JetSimpleNameExpression callee = ((JetSimpleNameReference) ref).getExpression();
            JetCallExpression callExpression = PsiTreeUtil.getParentOfType(callee, JetCallExpression.class);
            if (callExpression == null || callExpression.getCalleeExpression() != callee) continue;

            List<? extends ValueArgument> arguments = callExpression.getValueArguments();
            if (arguments.size() != 1) continue;

            JetExpression argExpression = arguments.get(0).getArgumentExpression();
            if (!(argExpression instanceof JetFunctionLiteralExpression)) continue;

            BindingContext context = ResolvePackage.analyze(callExpression, BodyResolveMode.FULL);

            JetFunctionLiteral functionLiteral = ((JetFunctionLiteralExpression) argExpression).getFunctionLiteral();
            FunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionLiteral);
            assert functionDescriptor != null : "No descriptor for " + functionLiteral.getText();

            JetType samCallType = context.getType(callExpression);
            if (samCallType == null) continue;

            result.add(new DeferredJavaMethodOverrideOrSAMUsage(functionLiteral, functionDescriptor, samCallType));
        }
    }

    private static void findConstructorDelegationUsages(@NotNull ChangeInfo changeInfo, @NotNull final Set<UsageInfo> result) {
        PsiElement method = changeInfo.getMethod();
        if (!(RefactoringPackage.isTrueJavaMethod(method))) return;

        PsiMethod psiMethod = (PsiMethod) method;
        if (!psiMethod.isConstructor()) return;

        UsagesSearchPackage.processDelegationCallConstructorUsages(
                psiMethod,
                psiMethod.getUseScope(),
                new Function1<JetConstructorDelegationCall, Unit>() {
                    @Override
                    public Unit invoke(JetConstructorDelegationCall element) {
                        result.add(new JavaConstructorDeferredUsageInDelegationCall(element));
                        return null;
                    }
                }
        );
    }

    private static void findKotlinOverrides(ChangeInfo changeInfo, Set<UsageInfo> result) {
        PsiElement method = changeInfo.getMethod();
        if (!RefactoringPackage.isTrueJavaMethod(method)) return;

        for (PsiMethod overridingMethod : OverridingMethodsSearch.search((PsiMethod) method)) {
            PsiElement unwrappedElement = AsJavaPackage.getNamedUnwrappedElement(overridingMethod);
            if (!(unwrappedElement instanceof JetNamedFunction)) continue;

            JetNamedFunction function = (JetNamedFunction) unwrappedElement;
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) ResolvePackage.resolveToDescriptor(function);

            result.add(new DeferredJavaMethodOverrideOrSAMUsage(function, functionDescriptor, null));

            findDeferredUsagesOfParameters(changeInfo, result, function, functionDescriptor);
        }
    }

    private static void findKotlinCallers(JavaChangeInfo changeInfo, Set<UsageInfo> result) {
        PsiElement method = changeInfo.getMethod();
        if (!RefactoringPackage.isTrueJavaMethod(method)) return;

        for (PsiMethod primaryCaller : changeInfo.getMethodsToPropagateParameters()) {
            addDeferredCallerIfPossible(result, primaryCaller);
            for (PsiMethod overridingCaller : OverridingMethodsSearch.search(primaryCaller)) {
                addDeferredCallerIfPossible(result, overridingCaller);
            }
        }
    }

    private static void addDeferredCallerIfPossible(Set<UsageInfo> result, PsiMethod overridingCaller) {
        PsiElement unwrappedElement = AsJavaPackage.getNamedUnwrappedElement(overridingCaller);
        if (unwrappedElement instanceof JetFunction || unwrappedElement instanceof JetClass) {
            result.add(new DeferredJavaMethodKotlinCallerUsage((JetNamedDeclaration) unwrappedElement));
        }
    }

    private static void findDeferredUsagesOfParameters(
            ChangeInfo changeInfo,
            Set<UsageInfo> result,
            JetNamedFunction function,
            FunctionDescriptor functionDescriptor
    ) {
        final JetCallableDefinitionUsage<?> functionInfoForParameters =
                new JetCallableDefinitionUsage<PsiElement>(function, functionDescriptor, null, null);
        List<JetParameter> oldParameters = PsiUtilPackage.getValueParameters(function);
        ParameterInfo[] parameters = changeInfo.getNewParameters();
        for (int i = 0; i < parameters.length; i++) {
            final int paramIndex = i;
            ParameterInfo parameterInfo = parameters[paramIndex];
            if (parameterInfo.getOldIndex() >= 0 && parameterInfo.getOldIndex() < oldParameters.size()) {
                JetParameter oldParam = oldParameters.get(parameterInfo.getOldIndex());
                String oldParamName = oldParam.getName();

                if (oldParamName != null && !oldParamName.equals(parameterInfo.getName())) {
                    for (PsiReference reference : ReferencesSearch.search(oldParam, oldParam.getUseScope())) {
                        final PsiElement element = reference.getElement();

                        if ((element instanceof JetSimpleNameExpression || element instanceof KDocName) &&
                            !(element.getParent() instanceof JetValueArgumentName)) // Usages in named arguments of the calls usage will be changed when the function call is changed
                        {
                            result.add(
                                    new JavaMethodDeferredKotlinUsage<JetElement>((JetElement) element) {
                                        @NotNull
                                        @Override
                                        public JavaMethodKotlinUsageWithDelegate<JetElement> resolve(@NotNull JetChangeInfo javaMethodChangeInfo) {
                                            return new JavaMethodKotlinUsageWithDelegate<JetElement>((JetElement) element,
                                                                                                     javaMethodChangeInfo) {
                                                @NotNull
                                                @Override
                                                public JetUsageInfo<JetElement> getDelegateUsage() {
                                                    return new JetParameterUsage((JetElement) element,
                                                                                 getJavaMethodChangeInfo().getNewParameters()[paramIndex],
                                                                                 functionInfoForParameters);
                                                }
                                            };
                                        }
                                    }
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    public MultiMap<PsiElement, String> findConflicts(ChangeInfo info, Ref<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, String> result = new MultiMap<PsiElement, String>();

        // Delete OverriderUsageInfo and CallerUsageInfo for Kotlin declarations since they can't be processed correctly
        // TODO (OverriderUsageInfo only): Drop when OverriderUsageInfo.getElement() gets deleted
        UsageInfo[] usageInfos = refUsages.get();
        List<UsageInfo> adjustedUsages = KotlinPackage.filterNot(
                usageInfos,
                new Function1<UsageInfo, Boolean>() {
                    @Override
                    public Boolean invoke(UsageInfo info) {
                        return getOverriderOrCaller(info) instanceof KotlinLightMethod;
                    }
                }
        );
        if (adjustedUsages.size() < usageInfos.length) {
            refUsages.set(adjustedUsages.toArray(new UsageInfo[adjustedUsages.size()]));
        }

        if (!(info instanceof JetChangeInfo)) return result;

        Set<String> parameterNames = new HashSet<String>();
        JetChangeInfo changeInfo = (JetChangeInfo) info;
        PsiElement function = info.getMethod();
        PsiElement element = function != null ? function : changeInfo.getContext();
        BindingContext bindingContext = ResolvePackage.analyze((JetElement) element, BodyResolveMode.FULL);
        CallableDescriptor oldDescriptor = ChangeSignaturePackage.getOriginalBaseFunctionDescriptor(changeInfo);
        DeclarationDescriptor containingDeclaration = oldDescriptor.getContainingDeclaration();

        JetScope parametersScope = null;
        if (oldDescriptor instanceof ConstructorDescriptor && containingDeclaration instanceof ClassDescriptorWithResolutionScopes)
            parametersScope = ((ClassDescriptorWithResolutionScopes) containingDeclaration).getScopeForInitializerResolution();
        else if (function instanceof JetFunction)
            parametersScope = org.jetbrains.kotlin.idea.refactoring.RefactoringPackage.getBodyScope((JetFunction) function, bindingContext);

        JetScope callableScope = org.jetbrains.kotlin.idea.refactoring.RefactoringPackage.getContainingScope(oldDescriptor, bindingContext);

        JetMethodDescriptor.Kind kind = ChangeSignaturePackage.getKind(changeInfo);
        if (!kind.getIsConstructor() && callableScope != null && !info.getNewName().isEmpty()) {
            Name newName = Name.identifier(info.getNewName());
            Collection<? extends CallableDescriptor> conflicts = oldDescriptor instanceof FunctionDescriptor
                                                                 ? callableScope.getFunctions(newName, LookupLocation.NO_LOCATION)
                                                                 : callableScope.getProperties(newName, LookupLocation.NO_LOCATION);
            for (CallableDescriptor conflict : conflicts) {
                if (conflict == oldDescriptor) continue;

                PsiElement conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(conflict);
                if (conflictElement == changeInfo.getMethod()) continue;

                if (getCallableParameterTypes(conflict).equals(getCallableParameterTypes(oldDescriptor))) {
                    result.putValue(conflictElement, "Function already exists: '" + DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(conflict) + "'");
                    break;
                }
            }
        }

        for (JetParameterInfo parameter : changeInfo.getNonReceiverParameters()) {
            JetValVar valOrVar = parameter.getValOrVar();
            String parameterName = parameter.getName();

            if (!parameterNames.add(parameterName)) {
                result.putValue(element, "Duplicating parameter '" + parameterName + "'");
            }
            if (parametersScope != null) {
                if (kind == JetMethodDescriptor.Kind.PRIMARY_CONSTRUCTOR && valOrVar != JetValVar.None) {
                    for (VariableDescriptor property : parametersScope.getProperties(Name.identifier(parameterName), LookupLocation.NO_LOCATION)) {
                        PsiElement propertyDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(property);

                        if (propertyDeclaration != null && !(propertyDeclaration.getParent() instanceof JetParameterList)) {
                            result.putValue(propertyDeclaration, "Duplicating property '" + parameterName + "'");
                            break;
                        }
                    }
                }
                else if (function instanceof JetFunction) {
                    VariableDescriptor variable = parametersScope.getLocalVariable(Name.identifier(parameterName));

                    if (variable != null && !(variable instanceof ValueParameterDescriptor)) {
                        PsiElement conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(variable);
                        result.putValue(conflictElement, "Duplicating local variable '" + parameterName + "'");
                    }
                }
            }
        }

        JetParameterInfo newReceiverInfo = changeInfo.getReceiverParameterInfo();
        JetParameterInfo originalReceiverInfo = changeInfo.getMethodDescriptor().getReceiver();
        if (function instanceof JetCallableDeclaration && newReceiverInfo != originalReceiverInfo) {
            findReceiverIntroducingConflicts(result, function, newReceiverInfo);
            findInternalExplicitReceiverConflicts(refUsages.get(), result, originalReceiverInfo);
            findThisLabelConflicts((JetChangeInfo) info, refUsages, result, changeInfo, function);
        }

        for (UsageInfo usageInfo : usageInfos) {
            if (!(usageInfo instanceof KotlinCallerUsage)) continue;

            JetNamedDeclaration caller = (JetNamedDeclaration) usageInfo.getElement();
            DeclarationDescriptor callerDescriptor = ResolvePackage.resolveToDescriptor(caller);

            findParameterDuplicationInCaller(result, changeInfo, caller, callerDescriptor);
        }

        return result;
    }

    private static void findParameterDuplicationInCaller(
            MultiMap<PsiElement, String> result,
            JetChangeInfo changeInfo,
            JetNamedDeclaration caller,
            DeclarationDescriptor callerDescriptor
    ) {
        List<JetParameter> valueParameters = PsiUtilPackage.getValueParameters(caller);
        Map<String, JetParameter> existingParameters = KotlinPackage.toMap(
                valueParameters,
                new Function1<JetParameter, String>() {
                    @Override
                    public String invoke(JetParameter parameter) {
                        return parameter.getName();
                    }
                }
        );
        for (JetParameterInfo parameterInfo : changeInfo.getNonReceiverParameters()) {
            if (!(parameterInfo.getIsNewParameter())) continue;

            String name = parameterInfo.getName();
            JetParameter parameter = existingParameters.get(name);
            if (parameter != null) {
                result.putValue(parameter, "There is already a parameter '" + name + "' in " +
                                           IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callerDescriptor) +
                                           ". It will conflict with the new parameter.");
            }
        }
    }

    private static void findThisLabelConflicts(
            JetChangeInfo info,
            Ref<UsageInfo[]> refUsages,
            MultiMap<PsiElement, String> result,
            JetChangeInfo changeInfo,
            PsiElement function
    ) {
        JetPsiFactory psiFactory = new JetPsiFactory(function.getProject());
        for (UsageInfo usageInfo : refUsages.get()) {
            if (!(usageInfo instanceof JetParameterUsage)) continue;

            String newExprText = ((JetParameterUsage) usageInfo).getReplacementText(changeInfo);
            if (!newExprText.startsWith("this@")) continue;

            if (usageInfo.getElement() instanceof KDocName) continue; // TODO support converting parameter to receiver in KDoc

            JetExpression originalExpr = (JetExpression) usageInfo.getElement();
            JetScope scope = ResolvePackage.analyze(originalExpr, BodyResolveMode.FULL)
                    .get(BindingContext.RESOLUTION_SCOPE, originalExpr);
            if (scope == null) continue;

            JetThisExpression newExpr = (JetThisExpression) psiFactory.createExpression(newExprText);
            JetSimpleNameExpression labelExpr = newExpr.getTargetLabel();
            if (labelExpr == null) continue;

            BindingContext newContext =
                    AnalyzerPackage.analyzeInContext(newExpr,
                                                     scope,
                                                     new BindingTraceContext(),
                                                     DataFlowInfo.EMPTY,
                                                     TypeUtils.NO_EXPECTED_TYPE,
                                                     DescriptorUtils.getContainingModule(scope.getContainingDeclaration()));
            if (newContext.get(BindingContext.AMBIGUOUS_LABEL_TARGET, labelExpr) != null) {
                result.putValue(
                        originalExpr,
                        "Parameter reference can't be safely replaced with " +
                        newExprText +
                        " since " +
                        labelExpr.getText() +
                        " is ambiguous in this context"
                );
            }
        }
    }

    private static void findInternalExplicitReceiverConflicts(
            UsageInfo[] usages,
            MultiMap<PsiElement, String> result,
            JetParameterInfo originalReceiverInfo
    ) {
        if (originalReceiverInfo == null) {
            for (UsageInfo usageInfo : usages) {
                if (!(usageInfo instanceof JetFunctionCallUsage || usageInfo instanceof JetPropertyCallUsage)) continue;

                JetElement callElement = (JetElement) usageInfo.getElement();
                if (callElement == null) continue;

                PsiElement parent = callElement.getParent();
                if (parent instanceof JetQualifiedExpression && ((JetQualifiedExpression) parent).getSelectorExpression() == callElement) {
                    String message = "Explicit receiver is already present in call element: " +
                                     CommonRefactoringUtil.htmlEmphasize(parent.getText());
                    result.putValue(callElement, message);
                }
            }
        }
    }

    private static void findReceiverIntroducingConflicts(
            MultiMap<PsiElement, String> result,
            PsiElement callable,
            JetParameterInfo newReceiverInfo
    ) {
        if (newReceiverInfo != null && (callable instanceof JetNamedFunction) && ((JetNamedFunction) callable).getBodyExpression() != null) {
            Map<JetReferenceExpression, BindingContext> noReceiverRefToContext = KotlinPackage.filter(
                    JetFileReferencesResolver.INSTANCE$.resolve((JetNamedFunction) callable, true, true),
                    new Function1<Map.Entry<? extends JetReferenceExpression, ? extends BindingContext>, Boolean>() {
                        @Override
                        public Boolean invoke(Map.Entry<? extends JetReferenceExpression, ? extends BindingContext> entry) {
                            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(entry.getKey(), entry.getValue());
                            return resolvedCall != null
                                   && !resolvedCall.getDispatchReceiver().exists()
                                   && !resolvedCall.getExtensionReceiver().exists();
                        }
                    }
            );

            JetPsiFactory psiFactory = new JetPsiFactory(callable.getProject());
            JetFile tempFile = RefactoringPackage.createTempCopy(
                    (JetFile) callable.getContainingFile(),
                    new Function1<String, String>() {
                        @Override
                        public String invoke(String s) {
                            return s;
                        }
                    }
            );
            JetNamedFunction functionWithReceiver =
                    PsiTreeUtil.getParentOfType(tempFile.findElementAt(callable.getTextOffset()), JetNamedFunction.class);
            JetTypeReference receiverTypeRef = psiFactory.createType(newReceiverInfo.getCurrentTypeText());
            TypeRefHelpersPackage.setReceiverTypeReference(functionWithReceiver, receiverTypeRef);
            //noinspection ConstantConditions
            BindingContext newContext = ResolvePackage.analyze(functionWithReceiver.getBodyExpression(), BodyResolveMode.FULL);

            //noinspection ConstantConditions
            int originalOffset = ((JetNamedFunction) callable).getBodyExpression().getTextOffset();
            JetExpression newBody = functionWithReceiver.getBodyExpression();
            for (Map.Entry<JetReferenceExpression, BindingContext> entry : noReceiverRefToContext.entrySet()) {
                JetReferenceExpression originalRef = entry.getKey();
                BindingContext originalContext = entry.getValue();
                //noinspection ConstantConditions
                JetReferenceExpression newRef = PsiTreeUtil.getParentOfType(
                        newBody.findElementAt(originalRef.getTextOffset() - originalOffset),
                        JetReferenceExpression.class
                );
                ResolvedCall<?> newResolvedCall = CallUtilPackage.getResolvedCall(newRef, newContext);
                if (newResolvedCall == null
                        || newResolvedCall.getExtensionReceiver().exists()
                        || newResolvedCall.getDispatchReceiver().exists()) {
                    //noinspection ConstantConditions
                    CallableDescriptor descriptor =
                            CallUtilPackage.getResolvedCall(originalRef, originalContext).getCandidateDescriptor();
                    PsiElement declaration = DescriptorToSourceUtilsIde.INSTANCE$.getAnyDeclaration(callable.getProject(), descriptor);
                    String prefix = declaration != null ? RefactoringUIUtil.getDescription(declaration, true) : originalRef.getText();
                    result.putValue(
                            originalRef,
                            KotlinPackage.capitalize(prefix + " will no longer be accessible after signature change")
                    );
                }
            }
        }
    }

    private static List<JetType> getCallableParameterTypes(CallableDescriptor descriptor) {
        return ContainerUtil.map(descriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetType>() {
            @Override
            public JetType fun(ValueParameterDescriptor descriptor) {
                return descriptor.getType();
            }
        });
    }

    private static boolean isJavaMethodUsage(UsageInfo usageInfo) {
        // MoveRenameUsageInfo corresponds to non-Java usage of Java method
        return usageInfo instanceof JavaMethodDeferredKotlinUsage || usageInfo instanceof MoveRenameUsageInfo;
    }

    @Nullable
    private static UsageInfo createReplacementUsage(UsageInfo originalUsageInfo, JetChangeInfo javaMethodChangeInfo, UsageInfo[] allUsages) {
        if (originalUsageInfo instanceof JavaMethodDeferredKotlinUsage) {
            return ((JavaMethodDeferredKotlinUsage<?>) originalUsageInfo).resolve(javaMethodChangeInfo);
        }

        JetCallElement callElement = PsiTreeUtil.getParentOfType(originalUsageInfo.getElement(), JetCallElement.class);
        if (callElement == null) return null;

        PsiReference ref = originalUsageInfo.getReference();
        PsiElement refTarget = ref != null ? ref.resolve() : null;
        return new JavaMethodKotlinCallUsage(callElement,
                                             javaMethodChangeInfo,
                                             refTarget != null && ChangeSignaturePackage.isCaller(refTarget, allUsages));
    }

    private static boolean canCreateReplacementUsage(UsageInfo originalUsageInfo) {
        if (originalUsageInfo instanceof JavaMethodDeferredKotlinUsage) return true;
        return PsiTreeUtil.getParentOfType(originalUsageInfo.getElement(), JetCallElement.class) != null;
    }

    private static class NullabilityPropagator {
        private final NullableNotNullManager nullManager;
        private final JavaPsiFacade javaPsiFacade;
        private final JavaCodeStyleManager javaCodeStyleManager;
        private final PsiAnnotation methodAnnotation;
        private final PsiAnnotation[] parameterAnnotations;

        public NullabilityPropagator(@NotNull PsiMethod baseMethod) {
            Project project = baseMethod.getProject();
            this.nullManager = NullableNotNullManager.getInstance(project);
            this.javaPsiFacade = JavaPsiFacade.getInstance(project);
            this.javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);

            this.methodAnnotation = getNullabilityAnnotation(baseMethod);
            this.parameterAnnotations = ContainerUtil.map2Array(
                    baseMethod.getParameterList().getParameters(),
                    PsiAnnotation.class,
                    new Function<PsiParameter, PsiAnnotation>() {
                        @Override
                        public PsiAnnotation fun(PsiParameter parameter) {
                            return getNullabilityAnnotation(parameter);
                        }
                    }
            );
        }

        @Nullable
        private PsiAnnotation getNullabilityAnnotation(@NotNull PsiModifierListOwner element) {
            PsiAnnotation nullAnnotation = nullManager.getNullableAnnotation(element, false);
            PsiAnnotation notNullAnnotation = nullManager.getNotNullAnnotation(element, false);
            if ((nullAnnotation == null) == (notNullAnnotation == null)) return null;
            return nullAnnotation != null ? nullAnnotation : notNullAnnotation;
        }

        private void addNullabilityAnnotationIfApplicable(@NotNull PsiModifierListOwner element, @Nullable PsiAnnotation annotation) {
            PsiAnnotation nullableAnnotation = nullManager.getNullableAnnotation(element, false);
            PsiAnnotation notNullAnnotation = nullManager.getNotNullAnnotation(element, false);

            if (notNullAnnotation != null && nullableAnnotation == null && element instanceof PsiMethod) return;

            String annotationQualifiedName = annotation != null ? annotation.getQualifiedName() : null;
            if (annotationQualifiedName != null
                && javaPsiFacade.findClass(annotationQualifiedName, element.getResolveScope()) == null) return;

            if (notNullAnnotation != null) {
                notNullAnnotation.delete();
            }
            if (nullableAnnotation != null) {
                nullableAnnotation.delete();
            }

            if (annotationQualifiedName == null) return;

            PsiModifierList modifierList = element.getModifierList();
            if (modifierList != null) {
                modifierList.addAnnotation(annotationQualifiedName);
                javaCodeStyleManager.shortenClassReferences(element);
            }
        }

        public void processMethod(@NotNull PsiMethod currentMethod) {
            PsiParameter[] currentParameters = currentMethod.getParameterList().getParameters();
            addNullabilityAnnotationIfApplicable(currentMethod, methodAnnotation);
            for (int i = 0; i < parameterAnnotations.length; i++) {
                addNullabilityAnnotationIfApplicable(currentParameters[i], parameterAnnotations[i]);
            }
        }
    }

    private static boolean isOverriderOrCaller(UsageInfo usage) {
        return usage instanceof OverriderUsageInfo || usage instanceof CallerUsageInfo;
    }

    @Nullable
    private static PsiMethod getOverriderOrCaller(UsageInfo usage) {
        if (usage instanceof OverriderUsageInfo) return ((OverriderUsageInfo) usage).getOverridingMethod();
        if (usage instanceof CallerUsageInfo) {
            PsiElement element = usage.getElement();
            return element instanceof PsiMethod ? (PsiMethod) element : null;
        }
        return null;
    }

    @Override
    public boolean processUsage(ChangeInfo changeInfo, UsageInfo usageInfo, boolean beforeMethodChange, UsageInfo[] usages) {
        PsiElement method = changeInfo.getMethod();
        boolean isJavaMethodUsage = isJavaMethodUsage(usageInfo);

        if (usageInfo instanceof KotlinWrapperForJavaUsageInfos) {
            List<JavaChangeInfo> javaChangeInfos = ((JetChangeInfo) changeInfo).getOrCreateJavaChangeInfos();
            assert javaChangeInfos != null : "JavaChangeInfo not found: " + method.getText();

            KotlinWrapperForJavaUsageInfos wrapperForJavaUsageInfos = (KotlinWrapperForJavaUsageInfos) usageInfo;
            UsageInfo[] javaUsageInfos = wrapperForJavaUsageInfos.getJavaUsageInfos();
            ChangeSignatureUsageProcessor[] processors = ChangeSignatureUsageProcessor.EP_NAME.getExtensions();

            for (JavaChangeInfo javaChangeInfo : javaChangeInfos) {
                // Match names so that getter/setter usages are not confused with each other
                if (!javaChangeInfo.getOldName().equals(wrapperForJavaUsageInfos.getJavaChangeInfo().getOldName())) continue;

                NullabilityPropagator nullabilityPropagator = new NullabilityPropagator(javaChangeInfo.getMethod());

                for (UsageInfo usage : javaUsageInfos) {
                    if (isOverriderOrCaller(usage) && beforeMethodChange) continue;
                    for (ChangeSignatureUsageProcessor processor : processors) {
                        if (processor instanceof JetChangeSignatureUsageProcessor) continue;
                        if (isOverriderOrCaller(usage)) {
                            processor.processUsage(javaChangeInfo, usage, true, javaUsageInfos);
                        }
                        if (processor.processUsage(javaChangeInfo, usage, beforeMethodChange, javaUsageInfos)) break;
                    }
                    if (usage instanceof OverriderUsageInfo && ((OverriderUsageInfo) usage).isOriginalOverrider()) {
                        PsiMethod overridingMethod = ((OverriderUsageInfo) usage).getOverridingMethod();
                        if (overridingMethod != null && !(overridingMethod instanceof KotlinLightMethod)) {
                            nullabilityPropagator.processMethod(overridingMethod);
                        }
                    }
                }
            }
        }

        if (beforeMethodChange) {
            if (!(method instanceof PsiMethod) || initializedOriginalDescriptor) return true;

            OriginalJavaMethodDescriptorWrapper descriptorWrapper = getOriginalJavaMethodDescriptorWrapper(usages);
            if (descriptorWrapper == null || descriptorWrapper.originalJavaMethodDescriptor != null) return true;

            FunctionDescriptor methodDescriptor = ResolvePackage.getJavaMethodDescriptor((PsiMethod) method);
            assert methodDescriptor != null;
            descriptorWrapper.originalJavaMethodDescriptor =
                    new JetChangeSignatureData(methodDescriptor, method, Collections.singletonList(methodDescriptor));

            // This change info is used as a placeholder before primary method update
            // It gets replaced with real change info afterwards
            JetChangeInfo dummyChangeInfo = new DummyJetChangeInfo(changeInfo.getMethod(), descriptorWrapper.originalJavaMethodDescriptor);
            for (int i = 0; i < usages.length; i++) {
                UsageInfo oldUsageInfo = usages[i];
                if (!isJavaMethodUsage(oldUsageInfo)) continue;

                UsageInfo newUsageInfo = createReplacementUsage(oldUsageInfo, dummyChangeInfo, usages);
                if (newUsageInfo != null) {
                    usages[i] = newUsageInfo;
                }
            }

            initializedOriginalDescriptor = true;

            return true;
        }

        PsiElement element = usageInfo.getElement();
        if (element == null) return false;

        if (usageInfo instanceof JavaMethodKotlinUsageWithDelegate) {
            JavaMethodKotlinUsageWithDelegate usageWithDelegate = (JavaMethodKotlinUsageWithDelegate) usageInfo;
            // Do not call getOriginalJavaMethodDescriptorWrapper() on each usage to avoid O(usage_count^2) performance
            if (((JavaMethodKotlinUsageWithDelegate) usageInfo).getJavaMethodChangeInfo() instanceof DummyJetChangeInfo) {
                OriginalJavaMethodDescriptorWrapper descriptorWrapper = getOriginalJavaMethodDescriptorWrapper(usages);
                JetMethodDescriptor methodDescriptor = descriptorWrapper != null ? descriptorWrapper.originalJavaMethodDescriptor : null;
                if (methodDescriptor == null) return true;

                JetChangeInfo javaMethodChangeInfo = ChangeSignaturePackage.toJetChangeInfo(changeInfo, methodDescriptor);
                for (UsageInfo info : usages) {
                    if (info instanceof JavaMethodKotlinUsageWithDelegate) {
                        ((JavaMethodKotlinUsageWithDelegate) info).setJavaMethodChangeInfo(javaMethodChangeInfo);
                    }
                }
            }

            return usageWithDelegate.processUsage(usages);
        }

        if (usageInfo instanceof MoveRenameUsageInfo && isJavaMethodUsage) {
            JetSimpleNameExpression callee = PsiTreeUtil.getParentOfType(usageInfo.getElement(), JetSimpleNameExpression.class, false);
            PsiReference ref = callee != null ? ReferencesPackage.getMainReference(callee) : null;
            if (ref instanceof JetSimpleNameReference) {
                ((JetSimpleNameReference) ref).handleElementRename(((PsiMethod)method).getName());
                return true;
            }

            return false;
        }

        return usageInfo instanceof JetUsageInfo ? ((JetUsageInfo) usageInfo).processUsage((JetChangeInfo) changeInfo, element, usages) : true;
    }

    @Override
    public boolean processPrimaryMethod(ChangeInfo changeInfo) {
        if (!(changeInfo instanceof JetChangeInfo)) return false;

        JetChangeInfo jetChangeInfo = (JetChangeInfo) changeInfo;
        for (JetCallableDefinitionUsage primaryFunction : jetChangeInfo.getMethodDescriptor().getPrimaryCallables()) {
            primaryFunction.processUsage(jetChangeInfo, primaryFunction.getDeclaration(), UsageInfo.EMPTY_ARRAY);
        }
        jetChangeInfo.primaryMethodUpdated();
        return true;
    }

    @Override
    public boolean shouldPreviewUsages(ChangeInfo changeInfo, UsageInfo[] usages) {
        return false;
    }

    @Override
    public boolean setupDefaultValues(ChangeInfo changeInfo, Ref<UsageInfo[]> refUsages, Project project) {
        return true;
    }

    @Override
    public void registerConflictResolvers(List<ResolveSnapshotProvider.ResolveSnapshot> snapshots, @NotNull ResolveSnapshotProvider resolveSnapshotProvider, UsageInfo[] usages, ChangeInfo changeInfo) {
    }
}
