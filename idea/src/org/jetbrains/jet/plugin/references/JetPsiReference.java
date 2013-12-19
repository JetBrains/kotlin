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

package org.jetbrains.jet.plugin.references;

import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil.findDeclarationsForDescriptorWithoutTrace;

public abstract class JetPsiReference implements PsiPolyVariantReference {

    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.references.JetPsiReference");

    @NotNull
    protected final JetReferenceExpression myExpression;

    protected JetPsiReference(@NotNull JetReferenceExpression expression) {
        this.myExpression = expression;
    }

    @NotNull
    @Override
    public PsiElement getElement() {
        return myExpression;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(resolveToPsiElements());
    }

    @Override
    public PsiElement resolve() {
        Collection<? extends PsiElement> psiElements = resolveToPsiElements();
        if (psiElements.size() == 1) {
            return psiElements.iterator().next();
        }
        return null;
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return "<TBD>";
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        PsiElement target = resolve();
        PsiElement mirrorElement = element instanceof KotlinLightMethod ? ((KotlinLightMethod) element).getOrigin() : null;
        return target == element || (mirrorElement != null && target == mirrorElement) || (target != null && target.getNavigationElement() == element);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @NotNull
    private Collection<? extends PsiElement> resolveToPsiElements() {
        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(myExpression);

        return resolveToPsiElements(context);
    }

    @NotNull
    private Collection<? extends PsiElement> resolveToPsiElements(@NotNull BindingContext context) {
        Collection<? extends DeclarationDescriptor> targetDescriptors = getTargetDescriptors(context);

        if (targetDescriptors != null) {
            assert !(targetDescriptors.isEmpty()) : "targetDescriptors is not null, but empty, for " + myExpression.getText();
            Set<PsiElement> result = Sets.newHashSet();
            Project project = myExpression.getProject();
            for (DeclarationDescriptor target : targetDescriptors) {
                result.addAll(BindingContextUtils.descriptorToDeclarations(context, target));
                result.addAll(findDeclarationsForDescriptorWithoutTrace(project, target));

                if (target instanceof PackageViewDescriptor) {
                    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                    String fqName = ((PackageViewDescriptor) target).getFqName().asString();
                    ContainerUtil.addIfNotNull(result, psiFacade.findPackage(fqName));
                    ContainerUtil.addIfNotNull(result, psiFacade.findClass(fqName, GlobalSearchScope.allScope(project)));
                }
            }
            return result;
        }

        Collection<? extends PsiElement> labelTargets = getLabelTargets(context);
        if (labelTargets != null) {
            return labelTargets;
        }

        return Collections.emptySet();
    }

    @Nullable
    protected Collection<? extends DeclarationDescriptor> getTargetDescriptors(@NotNull BindingContext context) {
        DeclarationDescriptor targetDescriptor = context.get(BindingContext.REFERENCE_TARGET, myExpression);
        if (targetDescriptor != null) {
            return Collections.singleton(targetDescriptor);
        }
        return context.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, myExpression);
    }

    @Nullable
    private Collection<? extends PsiElement> getLabelTargets(@NotNull BindingContext context) {
        PsiElement labelTarget = context.get(BindingContext.LABEL_TARGET, myExpression);
        if (labelTarget != null) {
            return Collections.singleton(labelTarget);
        }
        return context.get(BindingContext.AMBIGUOUS_LABEL_TARGET, myExpression);
    }
}
