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

package org.jetbrains.kotlin.idea.liveTemplates.macro;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.IterableTypesDetection;
import org.jetbrains.kotlin.idea.core.IterableTypesDetector;
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.util.ExtensionUtils;
import org.jetbrains.kotlin.idea.util.ScopeUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.KtScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;

import java.util.*;

public abstract class BaseKotlinVariableMacro extends Macro {
    @Nullable
    private KtNamedDeclaration[] getVariables(Expression[] params, ExpressionContext context) {
        if (params.length != 0) return null;

        Project project = context.getProject();
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
        if (!(psiFile instanceof KtFile)) return null;

        KtExpression contextExpression = findContextExpression(psiFile, context.getStartOffset());
        if (contextExpression == null) return null;

        ResolutionFacade resolutionFacade = ResolutionUtils.getResolutionFacade(contextExpression);

        BindingContext bindingContext = resolutionFacade.analyze(contextExpression, BodyResolveMode.FULL);
        LexicalScope scope = ScopeUtils.getResolutionScope(contextExpression, bindingContext, resolutionFacade);

        IterableTypesDetector detector = resolutionFacade.getIdeService(IterableTypesDetection.class).createDetector(scope);

        DataFlowInfo dataFlowInfo = BindingContextUtilsKt.getDataFlowInfo(bindingContext, contextExpression);

        List<VariableDescriptor> filteredDescriptors = new ArrayList<VariableDescriptor>();
        for (DeclarationDescriptor declarationDescriptor : getAllVariables(scope)) {
            if (declarationDescriptor instanceof VariableDescriptor) {
                VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;

                if (variableDescriptor.getExtensionReceiverParameter() != null
                    && ExtensionUtils.substituteExtensionIfCallableWithImplicitReceiver(
                        variableDescriptor, scope, bindingContext, dataFlowInfo).isEmpty()) {
                    continue;
                }

                if (isSuitable(variableDescriptor, project, detector)) {
                    filteredDescriptors.add(variableDescriptor);
                }
            }
        }


        List<KtNamedDeclaration> declarations = new ArrayList<KtNamedDeclaration>();
        for (DeclarationDescriptor declarationDescriptor : filteredDescriptors) {
            PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor);
            assert declaration == null || declaration instanceof PsiNamedElement;

            if (declaration instanceof KtProperty || declaration instanceof KtParameter) {
                declarations.add((KtNamedDeclaration) declaration);
            }
        }

        return declarations.toArray(new KtNamedDeclaration[declarations.size()]);
    }

    private static Collection<DeclarationDescriptor> getAllVariables(LexicalScope scope) {
        Collection<DeclarationDescriptor> result = ContainerUtil.newArrayList();
        result.addAll(ScopeUtilsKt.collectDescriptorsFiltered(scope, DescriptorKindFilter.VARIABLES, KtScope.Companion.getALL_NAME_FILTER()));
        for (ReceiverParameterDescriptor implicitReceiver : ScopeUtilsKt.getImplicitReceiversHierarchy(scope)) {
            result.addAll(implicitReceiver.getType().getMemberScope().getDescriptors(DescriptorKindFilter.VARIABLES, KtScope.Companion.getALL_NAME_FILTER()));
        }
        return result;
    }

    protected abstract boolean isSuitable(
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull Project project,
            @NotNull IterableTypesDetector iterableTypesDetector
    );

    @Nullable
    private static KtExpression findContextExpression(PsiFile psiFile, int startOffset) {
        PsiElement e = psiFile.findElementAt(startOffset);
        while (e != null) {
            if (e instanceof KtExpression) {
                return (KtExpression) e;
            }
            e = e.getParent();
        }
        return null;
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        KtNamedDeclaration[] vars = getVariables(params, context);
        if (vars == null || vars.length == 0) return null;
        return new KotlinPsiElementResult(vars[0]);
    }

    @Override
    public LookupElement[] calculateLookupItems(@NotNull Expression[] params, ExpressionContext context) {
        PsiNamedElement[] vars = getVariables(params, context);
        if (vars == null || vars.length < 2) return null;
        Set<LookupElement> set = new LinkedHashSet<LookupElement>();
        for (PsiNamedElement var : vars) {
            set.add(LookupElementBuilder.create(var));
        }
        return set.toArray(new LookupElement[set.size()]);
    }
}
