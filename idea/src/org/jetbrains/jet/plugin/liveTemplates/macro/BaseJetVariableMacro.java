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

package org.jetbrains.jet.plugin.liveTemplates.macro;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForMacros;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseJetVariableMacro extends Macro {
    @Nullable
    private JetNamedDeclaration[] getVariables(Expression[] params, ExpressionContext context) {
        if (params.length != 0) return null;

        Project project = context.getProject();
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
        if (!(psiFile instanceof JetFile)) return null;

        JetExpression contextExpression = findContextExpression(psiFile, context.getStartOffset());
        if (contextExpression == null) return null;

        ResolveSessionForBodies resolveSession = ResolvePackage.getLazyResolveSession((JetFile) psiFile);

        BindingContext bc = resolveSession.resolveToElement(contextExpression);
        JetScope scope = bc.get(BindingContext.RESOLUTION_SCOPE, contextExpression);
        if (scope == null) {
            return null;
        }

        ExpressionTypingComponents components =
                new InjectorForMacros(project, resolveSession.getModuleDescriptor()).getExpressionTypingComponents();

        List<VariableDescriptor> filteredDescriptors = new ArrayList<VariableDescriptor>();
        for (DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors()) {
            if (declarationDescriptor instanceof VariableDescriptor) {
                VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
                if (isSuitable(variableDescriptor, scope, project, components)) {
                    filteredDescriptors.add(variableDescriptor);
                }
            }
        }

        List<JetNamedDeclaration> declarations = new ArrayList<JetNamedDeclaration>();
        for (DeclarationDescriptor declarationDescriptor : TipsManager.excludeNotCallableExtensions(filteredDescriptors, scope)) {
            PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor);
            assert declaration == null || declaration instanceof PsiNamedElement;

            if (declaration instanceof JetProperty || declaration instanceof JetParameter) {
                declarations.add((JetNamedDeclaration) declaration);
            }
        }

        return declarations.toArray(new JetNamedDeclaration[declarations.size()]);
    }

    protected abstract boolean isSuitable(
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull JetScope scope,
            @NotNull Project project,
            @NotNull ExpressionTypingComponents components
    );

    @Nullable
    private static JetExpression findContextExpression(PsiFile psiFile, int startOffset) {
        PsiElement e = psiFile.findElementAt(startOffset);
        while (e != null) {
            if (e instanceof JetExpression) {
                return (JetExpression) e;
            }
            e = e.getParent();
        }
        return null;
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        JetNamedDeclaration[] vars = getVariables(params, context);
        if (vars == null || vars.length == 0) return null;
        return new JetPsiElementResult(vars[0]);
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
