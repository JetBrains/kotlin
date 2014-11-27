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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.changeSignature.*;
import com.intellij.refactoring.rename.ResolveSnapshotProvider;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightMethod;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.refactoring.changeSignature.usages.*;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JetChangeSignatureUsageProcessor implements ChangeSignatureUsageProcessor {
    @Override
    public UsageInfo[] findUsages(ChangeInfo info) {
        Set<UsageInfo> result = new HashSet<UsageInfo>();

        if (info instanceof JetChangeInfo) {
            findAllMethodUsages((JetChangeInfo)info, result);
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    private static void findAllMethodUsages(JetChangeInfo changeInfo, Set<UsageInfo> result) {
        for (UsageInfo functionUsageInfo : changeInfo.getAffectedFunctions()) {
            if (functionUsageInfo instanceof JetFunctionDefinitionUsage) {
                findOneMethodUsages((JetFunctionDefinitionUsage) functionUsageInfo, changeInfo, result);
            }
            else {
                result.add(functionUsageInfo);
            }
        }
    }

    private static void findOneMethodUsages(
            @NotNull JetFunctionDefinitionUsage functionUsageInfo,
            JetChangeInfo changeInfo,
            Set<UsageInfo> result
    ) {
        result.add(functionUsageInfo);

        PsiElement functionPsi = functionUsageInfo.getElement();
        if (functionPsi == null) return;

        boolean isInherited = functionUsageInfo.isInherited();

        for (PsiReference reference : ReferencesSearch.search(functionPsi, functionPsi.getUseScope())) {
            PsiElement element = reference.getElement();

            if (element instanceof JetReferenceExpression) {
                PsiElement parent = element.getParent();

                if (parent instanceof JetCallExpression)
                    result.add(new JetFunctionCallUsage((JetCallExpression) parent, functionPsi, isInherited));
                else if (parent instanceof JetUserType && parent.getParent() instanceof JetTypeReference) {
                    parent = parent.getParent().getParent();

                    if (parent instanceof JetConstructorCalleeExpression && parent.getParent() instanceof JetDelegatorToSuperCall)
                        result.add(new JetFunctionCallUsage((JetDelegatorToSuperCall)parent.getParent(), functionPsi, isInherited));
                }
            }
        }

        String oldName = changeInfo.getOldName();

        if (oldName != null)
            TextOccurrencesUtil.findNonCodeUsages(functionPsi, oldName, true, true, changeInfo.getNewName(), result);

        List<JetParameter> oldParameters = functionPsi instanceof JetFunction
                                        ? ((JetFunction) functionPsi).getValueParameters()
                                        : ((JetClass) functionPsi).getPrimaryConstructorParameters();

        for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
            if (parameterInfo.getOldIndex() >= 0 && parameterInfo.getOldIndex() < oldParameters.size()) {
                JetParameter oldParam = oldParameters.get(parameterInfo.getOldIndex());
                String oldParamName = oldParam.getName();

                if (oldParamName != null && !oldParamName.equals(parameterInfo.getName())) {
                    for (PsiReference reference : ReferencesSearch.search(oldParam, oldParam.getUseScope())) {
                        PsiElement element = reference.getElement();

                        if (element instanceof JetSimpleNameExpression &&
                            !(element.getParent() instanceof JetValueArgumentName)) // Usages in named arguments of the calls usage will be changed when the function call is changed
                            result.add(new JetParameterUsage((JetSimpleNameExpression) element, parameterInfo, functionPsi, isInherited));
                    }
                }
            }
        }

        if (functionPsi instanceof JetClass && ((JetClass) functionPsi).isEnum()) {
            for (JetDeclaration declaration : ((JetClass) functionPsi).getDeclarations()) {
                if (declaration instanceof JetEnumEntry && ((JetEnumEntry) declaration).getDelegationSpecifierList() == null) {
                    result.add(new JetEnumEntryWithoutSuperCallUsage((JetEnumEntry) declaration));
                }
            }
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
        FunctionDescriptor oldDescriptor = changeInfo.getOldDescriptor();
        JetScope parametersScope = null;
        DeclarationDescriptor containingDeclaration = oldDescriptor != null ? oldDescriptor.getContainingDeclaration() : null;

        if (oldDescriptor instanceof ConstructorDescriptor && containingDeclaration instanceof ClassDescriptorWithResolutionScopes)
            parametersScope = ((ClassDescriptorWithResolutionScopes) containingDeclaration).getScopeForInitializerResolution();
        else if (function instanceof JetFunction)
            parametersScope = getFunctionBodyScope((JetFunction) function, bindingContext);

        JetScope functionScope = getFunctionScope(bindingContext, containingDeclaration);

        if (!changeInfo.isConstructor() && functionScope != null && !info.getNewName().isEmpty()) {
            for (FunctionDescriptor conflict : functionScope.getFunctions(Name.identifier(info.getNewName()))) {
                if (conflict != oldDescriptor && getFunctionParameterTypes(conflict).equals(getFunctionParameterTypes(oldDescriptor))) {
                    PsiElement conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(conflict);
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
                if (changeInfo.isConstructor() && valOrVar != JetValVar.None) {
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

        return result;
    }

    @Nullable
    private static JetScope getFunctionScope(BindingContext bindingContext, DeclarationDescriptor containingDeclaration) {
        if (containingDeclaration instanceof ClassDescriptorWithResolutionScopes)
            return ((ClassDescriptorWithResolutionScopes) containingDeclaration).getScopeForInitializerResolution();
        else if (containingDeclaration instanceof FunctionDescriptorImpl) {
            PsiElement container = DescriptorToSourceUtils.descriptorToDeclaration(containingDeclaration);

            if (container instanceof JetFunction)
                return getFunctionBodyScope((JetFunction) container, bindingContext);
        }
        else if (containingDeclaration instanceof PackageFragmentDescriptor)
            return ((PackageFragmentDescriptor) containingDeclaration).getMemberScope();

        return null;
    }

    @Nullable
    static JetScope getFunctionBodyScope(JetFunction element, BindingContext bindingContext) {
        JetExpression body = element.getBodyExpression();

        if (body != null) {
            for (PsiElement child : body.getChildren()) {
                if (child instanceof JetExpression)
                    return bindingContext.get(BindingContext.RESOLUTION_SCOPE, (JetExpression)child);
            }
        }

        return null;
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
        if (!(usageInfo instanceof MoveRenameUsageInfo)) return false;
        PsiElement referencedElement = ((MoveRenameUsageInfo) usageInfo).getReferencedElement();
        return referencedElement instanceof PsiMethod && !(referencedElement instanceof KotlinLightMethod);
    }

    @Nullable
    private static UsageInfo createFunctionCallUsage(
            UsageInfo originalUsageInfo,
            JetChangeInfo javaMethodChangeInfo
    ) {
        JetCallElement callElement = PsiTreeUtil.getParentOfType(originalUsageInfo.getElement(), JetCallElement.class);
        return callElement != null ? new JavaMethodKotlinCallUsage(callElement, javaMethodChangeInfo) : null;
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

            for (UsageInfo usage : javaUsageInfos) {
                if (usage instanceof OverriderUsageInfo && beforeMethodChange) continue;
                for (ChangeSignatureUsageProcessor processor : processors) {
                    if (usage instanceof OverriderUsageInfo) {
                        processor.processUsage(javaChangeInfo, usage, true, javaUsageInfos);
                    }
                    if (processor.processUsage(javaChangeInfo, usage, beforeMethodChange, javaUsageInfos)) break;
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
            JetChangeInfo javaMethodChangeInfo = JetChangeInfo.fromJavaChangeInfo(changeInfo, originalJavaMethodDescriptor);
            originalJavaMethodDescriptor = null;

            for (int i = 0; i < usages.length; i++) {
                UsageInfo oldUsageInfo = usages[i];
                if (!isJavaMethodUsage(oldUsageInfo)) continue;

                UsageInfo newUsageInfo = createFunctionCallUsage(oldUsageInfo, javaMethodChangeInfo);
                if (newUsageInfo != null) {
                    usages[i] = newUsageInfo;
                    if (oldUsageInfo == usageInfo) {
                        usageInfo = newUsageInfo;
                    }
                }
            }
        }

        if (usageInfo instanceof JavaMethodKotlinCallUsage) {
            return ((JavaMethodKotlinCallUsage) usageInfo).processUsage();
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
        ((JetChangeInfo)changeInfo).primaryMethodUpdated();
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
