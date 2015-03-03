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
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.changeSignature.*;
import com.intellij.refactoring.rename.ResolveSnapshotProvider;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.MultiMap;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver;
import org.jetbrains.kotlin.idea.refactoring.RefactoringPackage;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.*;
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JetChangeSignatureUsageProcessor implements ChangeSignatureUsageProcessor {
    @Override
    public UsageInfo[] findUsages(ChangeInfo info) {
        Set<UsageInfo> result = new HashSet<UsageInfo>();

        if (info instanceof JetChangeInfo) {
            findAllMethodUsages((JetChangeInfo) info, result);
        }
        else {
            findSAMUsages(info, result);
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    private static void findAllMethodUsages(JetChangeInfo changeInfo, Set<UsageInfo> result) {
        for (UsageInfo functionUsageInfo : ChangeSignaturePackage.getAffectedFunctions(changeInfo)) {
            if (functionUsageInfo instanceof JetFunctionDefinitionUsage) {
                findOneMethodUsages((JetFunctionDefinitionUsage) functionUsageInfo, changeInfo, result);
            }
            else {
                result.add(functionUsageInfo);
            }
        }
    }

    private static void findOneMethodUsages(
            @NotNull JetFunctionDefinitionUsage<?> functionUsageInfo,
            JetChangeInfo changeInfo,
            Set<UsageInfo> result
    ) {
        boolean isInherited = functionUsageInfo.isInherited();

        if (isInherited) {
            result.add(functionUsageInfo);
        }

        PsiElement functionPsi = functionUsageInfo.getElement();
        if (functionPsi == null) return;

        for (PsiReference reference : ReferencesSearch.search(functionPsi, functionPsi.getUseScope())) {
            PsiElement element = reference.getElement();

            if (element instanceof JetReferenceExpression) {
                PsiElement parent = element.getParent();

                if (parent instanceof JetCallExpression)
                    result.add(new JetFunctionCallUsage((JetCallExpression) parent, functionUsageInfo));
                else if (parent instanceof JetUserType && parent.getParent() instanceof JetTypeReference) {
                    parent = parent.getParent().getParent();

                    if (parent instanceof JetConstructorCalleeExpression && parent.getParent() instanceof JetDelegatorToSuperCall)
                        result.add(new JetFunctionCallUsage((JetDelegatorToSuperCall)parent.getParent(), functionUsageInfo));
                }
            }
        }

        String oldName = ChangeSignaturePackage.getOldName(changeInfo);

        if (oldName != null)
            TextOccurrencesUtil.findNonCodeUsages(functionPsi, oldName, true, true, changeInfo.getNewName(), result);

        List<JetParameter> oldParameters = functionPsi instanceof JetFunction
                                        ? ((JetFunction) functionPsi).getValueParameters()
                                        : ((JetClass) functionPsi).getPrimaryConstructorParameters();

        JetParameterInfo newReceiverInfo = changeInfo.getReceiverParameterInfo();

        for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
            if (parameterInfo.getOldIndex() >= 0 && parameterInfo.getOldIndex() < oldParameters.size()) {
                JetParameter oldParam = oldParameters.get(parameterInfo.getOldIndex());
                String oldParamName = oldParam.getName();

                if (parameterInfo == newReceiverInfo || (oldParamName != null && !oldParamName.equals(parameterInfo.getName()))) {
                    for (PsiReference reference : ReferencesSearch.search(oldParam, oldParam.getUseScope())) {
                        PsiElement element = reference.getElement();

                        if (element instanceof JetSimpleNameExpression &&
                            !(element.getParent() instanceof JetValueArgumentName)) // Usages in named arguments of the calls usage will be changed when the function call is changed
                        {
                            JetParameterUsage parameterUsage =
                                    new JetParameterUsage((JetSimpleNameExpression) element, parameterInfo, functionUsageInfo);
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
                if (declaration instanceof JetEnumEntry && ((JetEnumEntry) declaration).getDelegationSpecifierList() == null) {
                    result.add(new JetEnumEntryWithoutSuperCallUsage((JetEnumEntry) declaration));
                }
            }
        }
    }

    private static void processInternalReferences(
            JetFunctionDefinitionUsage functionUsageInfo,
            JetTreeVisitor<BindingContext> visitor
    ) {
        JetFunction jetFunction = (JetFunction) functionUsageInfo.getDeclaration();
        JetExpression body = jetFunction.getBodyExpression();
        if (body != null) {
            body.accept(visitor, ResolvePackage.analyze(body));
        }
        for (JetParameter parameter : jetFunction.getValueParameters()) {
            JetExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                defaultValue.accept(visitor, ResolvePackage.analyze(defaultValue));
            }
        }
    }

    private static void findOriginalReceiversUsages(
            @NotNull final JetFunctionDefinitionUsage<?> functionUsageInfo,
            @NotNull final Set<UsageInfo> result,
            @NotNull final JetChangeInfo changeInfo
    ) {
        final JetParameterInfo originalReceiverInfo = changeInfo.getMethodDescriptor().getReceiver();
        final FunctionDescriptor functionDescriptor = functionUsageInfo.getOriginalFunctionDescriptor();
        processInternalReferences(
                functionUsageInfo,
                new JetTreeVisitor<BindingContext>() {
                    private void processExplicitThis(
                            @NotNull JetSimpleNameExpression expression,
                            @NotNull ReceiverParameterDescriptor receiverDescriptor
                    ) {
                        if (originalReceiverInfo != null && !changeInfo.hasParameter(originalReceiverInfo)) return;
                        if (!(expression.getParent() instanceof JetThisExpression)) return;

                        if (receiverDescriptor == functionDescriptor.getExtensionReceiverParameter()) {
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
                        if (targetDescriptor == functionDescriptor) {
                            assert originalReceiverInfo != null : "No original receiver info provided: " + functionUsageInfo.getDeclaration().getText();
                            result.add(new JetImplicitThisToParameterUsage(callElement, originalReceiverInfo, functionUsageInfo));
                        }
                        else {
                            result.add(new JetImplicitOuterThisToQualifiedThisUsage(callElement, targetDescriptor));
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
                        if (!(receiverValue instanceof ThisReceiver)) {
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

            BindingContext context = ResolvePackage.analyze(callExpression);

            JetFunctionLiteral functionLiteral = ((JetFunctionLiteralExpression) argExpression).getFunctionLiteral();
            FunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionLiteral);
            assert functionDescriptor != null : "No descriptor for " + functionLiteral.getText();

            JetType samCallType = context.get(BindingContext.EXPRESSION_TYPE, callExpression);
            if (samCallType == null) continue;

            result.add(new KotlinSAMUsage(functionLiteral, functionDescriptor, samCallType));
        }
    }

    @Override
    public MultiMap<PsiElement, String> findConflicts(ChangeInfo info, Ref<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, String> result = new MultiMap<PsiElement, String>();

        if (!(info instanceof JetChangeInfo)) {
            return result;
        }

        Set<String> parameterNames = new HashSet<String>();
        JetChangeInfo changeInfo = (JetChangeInfo) info;
        PsiElement function = info.getMethod();
        PsiElement element = function != null ? function : changeInfo.getContext();
        BindingContext bindingContext = ResolvePackage.analyze((JetElement) element);
        FunctionDescriptor oldDescriptor = ChangeSignaturePackage.getOriginalBaseFunctionDescriptor(changeInfo);
        DeclarationDescriptor containingDeclaration = oldDescriptor.getContainingDeclaration();

        JetScope parametersScope = null;
        if (oldDescriptor instanceof ConstructorDescriptor && containingDeclaration instanceof ClassDescriptorWithResolutionScopes)
            parametersScope = ((ClassDescriptorWithResolutionScopes) containingDeclaration).getScopeForInitializerResolution();
        else if (function instanceof JetFunction)
            parametersScope = RefactoringPackage.getBodyScope((JetFunction) function, bindingContext);

        JetScope functionScope = RefactoringPackage.getContainingScope(oldDescriptor, bindingContext);

        if (!ChangeSignaturePackage.getIsConstructor(changeInfo) && functionScope != null && !info.getNewName().isEmpty()) {
            for (FunctionDescriptor conflict : functionScope.getFunctions(Name.identifier(info.getNewName()))) {
                if (conflict == oldDescriptor) continue;

                PsiElement conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(conflict);
                if (conflictElement == changeInfo.getMethod()) continue;

                if (getFunctionParameterTypes(conflict).equals(getFunctionParameterTypes(oldDescriptor))) {
                    result.putValue(conflictElement, "Function already exists: '" + DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(conflict) + "'");
                    break;
                }
            }
        }

        for (ParameterInfo parameter : info.getNewParameters()) {
            JetValVar valOrVar = ((JetParameterInfo) parameter).getValOrVar();
            String parameterName = parameter.getName();

            if (!parameterNames.add(parameterName)) {
                result.putValue(element, "Duplicating parameter '" + parameterName + "'");
            }
            if (parametersScope != null) {
                if (ChangeSignaturePackage.getIsConstructor(changeInfo) && valOrVar != JetValVar.None) {
                    for (VariableDescriptor property : parametersScope.getProperties(Name.identifier(parameterName))) {
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
        if (function instanceof JetNamedFunction && newReceiverInfo != originalReceiverInfo) {
            findReceiverIntroducingConflicts(result, function, newReceiverInfo);
            findInternalExplicitReceiverConflicts(refUsages.get(), result, originalReceiverInfo);
        }

        return result;
    }

    private static void findInternalExplicitReceiverConflicts(
            UsageInfo[] usages,
            MultiMap<PsiElement, String> result,
            JetParameterInfo originalReceiverInfo
    ) {
        if (originalReceiverInfo == null) {
            for (UsageInfo usageInfo : usages) {
                if (!(usageInfo instanceof JetFunctionCallUsage)) continue;

                JetFunctionCallUsage callUsage = (JetFunctionCallUsage) usageInfo;
                JetElement callElement = callUsage.getElement();
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
        if (newReceiverInfo != null && ((JetNamedFunction) callable).getBodyExpression() != null) {
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
            BindingContext newContext = ResolvePackage.analyze(functionWithReceiver.getBodyExpression());

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

    private static List<JetType> getFunctionParameterTypes(FunctionDescriptor descriptor) {
        return ContainerUtil.map(descriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetType>() {
            @Override
            public JetType fun(ValueParameterDescriptor descriptor) {
                return descriptor.getType();
            }
        });
    }

    private JetMethodDescriptor originalJavaMethodDescriptor;

    private static boolean isJavaMethodUsage(UsageInfo usageInfo) {
        if (usageInfo instanceof KotlinSAMUsage) return true;

        // MoveRenameUsageInfo corresponds to non-Java usage of Java method
        return usageInfo instanceof MoveRenameUsageInfo
               && RefactoringPackage.isTrueJavaMethod(((MoveRenameUsageInfo) usageInfo).getReferencedElement());
    }

    @Nullable
    private static UsageInfo createReplacementUsage(
            UsageInfo originalUsageInfo,
            final JetChangeInfo javaMethodChangeInfo
    ) {
        if (originalUsageInfo instanceof KotlinSAMUsage) {
            final KotlinSAMUsage samUsage = (KotlinSAMUsage) originalUsageInfo;
            return new JavaMethodKotlinUsageWithDelegate<JetFunction>(samUsage.getFunctionLiteral(), javaMethodChangeInfo) {
                private final JetFunctionDefinitionUsage<JetFunction> delegateUsage = new JetFunctionDefinitionUsage<JetFunction>(
                        samUsage.getFunctionLiteral(),
                        samUsage.getFunctionDescriptor(),
                        javaMethodChangeInfo.getMethodDescriptor().getOriginalPrimaryFunction(),
                        samUsage.getSamCallType()
                );

                @NotNull
                @Override
                protected JetUsageInfo<JetFunction> getDelegateUsage() {
                    return delegateUsage;
                }
            };
        }

        JetCallElement callElement = PsiTreeUtil.getParentOfType(originalUsageInfo.getElement(), JetCallElement.class);
        return callElement != null ? new JavaMethodKotlinCallUsage(callElement, javaMethodChangeInfo) : null;
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

    @Override
    public boolean processUsage(ChangeInfo changeInfo, UsageInfo usageInfo, boolean beforeMethodChange, UsageInfo[] usages) {
        PsiElement method = changeInfo.getMethod();
        boolean isJavaMethodUsage = isJavaMethodUsage(usageInfo);

        if (usageInfo instanceof KotlinWrapperForJavaUsageInfos) {
            JavaChangeInfo javaChangeInfo = ((JetChangeInfo) changeInfo).getOrCreateJavaChangeInfo();
            assert javaChangeInfo != null : "JavaChangeInfo not found: " + method.getText();
            UsageInfo[] javaUsageInfos = ((KotlinWrapperForJavaUsageInfos) usageInfo).getJavaUsageInfos();
            ChangeSignatureUsageProcessor[] processors = ChangeSignatureUsageProcessor.EP_NAME.getExtensions();

            NullabilityPropagator nullabilityPropagator = new NullabilityPropagator(javaChangeInfo.getMethod());

            for (UsageInfo usage : javaUsageInfos) {
                if (usage instanceof OverriderUsageInfo && beforeMethodChange) continue;
                for (ChangeSignatureUsageProcessor processor : processors) {
                    if (processor instanceof JetChangeSignatureUsageProcessor) continue;
                    if (usage instanceof OverriderUsageInfo) {
                        processor.processUsage(javaChangeInfo, usage, true, javaUsageInfos);
                    }
                    if (processor.processUsage(javaChangeInfo, usage, beforeMethodChange, javaUsageInfos)) break;
                }
                if (usage instanceof OverriderUsageInfo) {
                    PsiMethod overridingMethod = ((OverriderUsageInfo)usage).getOverridingMethod();
                    if (overridingMethod != null) {
                        nullabilityPropagator.processMethod(overridingMethod);
                    }
                }
            }
        }

        if (beforeMethodChange) {
            if (isJavaMethodUsage) {
                FunctionDescriptor methodDescriptor = ResolvePackage.getJavaMethodDescriptor((PsiMethod) method);
                JetChangeSignatureData changeSignatureData =
                        new JetChangeSignatureData(methodDescriptor, method, Collections.singletonList(methodDescriptor));
                if (changeSignatureData != originalJavaMethodDescriptor) {
                    originalJavaMethodDescriptor = changeSignatureData;
                }
            }

            return true;
        }

        PsiElement element = usageInfo.getElement();
        if (element == null) return false;

        if (isJavaMethodUsage && originalJavaMethodDescriptor != null) {
            JetChangeInfo javaMethodChangeInfo = ChangeSignaturePackage.toJetChangeInfo(changeInfo, originalJavaMethodDescriptor);
            originalJavaMethodDescriptor = null;

            for (int i = 0; i < usages.length; i++) {
                UsageInfo oldUsageInfo = usages[i];
                if (!isJavaMethodUsage(oldUsageInfo)) continue;

                UsageInfo newUsageInfo = createReplacementUsage(oldUsageInfo, javaMethodChangeInfo);
                if (newUsageInfo != null) {
                    usages[i] = newUsageInfo;
                    if (oldUsageInfo == usageInfo) {
                        usageInfo = newUsageInfo;
                    }
                }
            }
        }

        if (usageInfo instanceof JavaMethodKotlinUsageWithDelegate) {
            return ((JavaMethodKotlinUsageWithDelegate) usageInfo).processUsage();
        }

        if (usageInfo instanceof MoveRenameUsageInfo && isJavaMethodUsage) {
            JetSimpleNameExpression callee = PsiTreeUtil.getParentOfType(usageInfo.getElement(), JetSimpleNameExpression.class, false);
            PsiReference ref = callee != null ? callee.getReference() : null;
            if (ref instanceof JetSimpleNameReference) {
                ((JetSimpleNameReference) ref).handleElementRename(((PsiMethod)method).getName());
                return true;
            }

            return false;
        }

        return usageInfo instanceof JetUsageInfo ? ((JetUsageInfo) usageInfo).processUsage((JetChangeInfo) changeInfo, element) : true;
    }

    @Override
    public boolean processPrimaryMethod(ChangeInfo changeInfo) {
        if (!(changeInfo instanceof JetChangeInfo)) return false;

        JetChangeInfo jetChangeInfo = (JetChangeInfo) changeInfo;
        for (JetFunctionDefinitionUsage primaryFunction : jetChangeInfo.getMethodDescriptor().getPrimaryFunctions()) {
            primaryFunction.processUsage(jetChangeInfo, primaryFunction.getDeclaration());
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
