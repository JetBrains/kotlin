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

package org.jetbrains.jet.plugin.completion;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.JavaCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.plugin.completion.weigher.WeigherPackage;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.CancelableResolveSession;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;

class CompletionSession {
    @Nullable
    private final DeclarationDescriptor inDescriptor;
    private final CompletionParameters parameters;
    private final JetCompletionResultSet jetResult;
    private final JetSimpleNameReference jetReference;

    public CompletionSession(
            @NotNull CompletionParameters parameters,
            @NotNull CompletionResultSet result,
            @NotNull JetSimpleNameReference jetReference,
            @NotNull PsiElement position
    ) {
        this.parameters = parameters;
        this.jetReference = jetReference;

        CancelableResolveSession resolveSession =
                AnalyzerFacadeWithCache.getLazyResolveSessionForFile((JetFile) position.getContainingFile());
        BindingContext expressionBindingContext = resolveSession.resolveToElement(jetReference.getExpression());
        JetScope scope = expressionBindingContext.get(BindingContext.RESOLUTION_SCOPE, jetReference.getExpression());

        inDescriptor = scope != null ? scope.getContainingDeclaration() : null;

        this.jetResult = new JetCompletionResultSet(
                WeigherPackage.addJetSorting(result, parameters),
                resolveSession,
                expressionBindingContext, new Condition<DeclarationDescriptor>() {
            @Override
            public boolean value(DeclarationDescriptor descriptor) {
                return isVisibleDescriptor(descriptor);
            }
        });
    }

    public void completeForReference() {
        assert parameters.getCompletionType() == CompletionType.BASIC;

        if (isOnlyKeywordCompletion(getPosition())) {
            return;
        }

        if (shouldRunOnlyTypeCompletion()) {
            if (parameters.getInvocationCount() >= 2) {
                JetTypesCompletionHelper.addJetTypes(parameters, jetResult);
            }
            else {
                addReferenceVariants(new Condition<DeclarationDescriptor>() {
                    @Override
                    public boolean value(DeclarationDescriptor descriptor) {
                        return isPartOfTypeDeclaration(descriptor);
                    }
                });
                JavaCompletionContributor.advertiseSecondCompletion(parameters.getPosition().getProject(), jetResult.getResult());
            }

            return;
        }

        addReferenceVariants(Conditions.<DeclarationDescriptor>alwaysTrue());

        String prefix = jetResult.getResult().getPrefixMatcher().getPrefix();

        // Try to avoid computing not-imported descriptors for empty prefix
        if (prefix.isEmpty()) {
            if (parameters.getInvocationCount() < 2) {
                return;
            }

            if (PsiTreeUtil.getParentOfType(jetReference.getExpression(), JetDotQualifiedExpression.class) == null) {
                return;
            }
        }

        if (shouldRunTopLevelCompletion()) {
            JetTypesCompletionHelper.addJetTypes(parameters, jetResult);
            addJetTopLevelFunctions();
            addJetTopLevelObjects();
        }

        if (shouldRunExtensionsCompletion()) {
            addJetExtensions();
        }
    }

    public void completeSmart() {
        assert parameters.getCompletionType() == CompletionType.SMART;

        final SmartCompletionData data = CompletionPackage.buildSmartCompletionData(jetReference.getExpression(), getResolveSession());
        if (data != null) {
            addReferenceVariants(new Condition<DeclarationDescriptor>() {
                @Override
                public boolean value(DeclarationDescriptor descriptor) {
                    return data.accepts(descriptor);
                }
            });
            for (LookupElement element : data.getAdditionalElements()) {
                jetResult.addElement(element);
            }
        }
    }

    private static boolean isOnlyKeywordCompletion(PsiElement position) {
        return PsiTreeUtil.getParentOfType(position, JetModifierList.class) != null;
    }

    private void addJetExtensions() {
        Project project = getPosition().getProject();
        JetShortNamesCache namesCache = JetShortNamesCache.getKotlinInstance(project);

        Collection<DeclarationDescriptor> jetCallableExtensions = namesCache.getJetCallableExtensions(
                jetResult.getShortNameFilter(),
                jetReference.getExpression(),
                getResolveSession(),
                GlobalSearchScope.allScope(project));

        jetResult.addAllElements(jetCallableExtensions);
    }

    private static boolean isPartOfTypeDeclaration(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof PackageViewDescriptor || descriptor instanceof TypeParameterDescriptor) {
            return true;
        }

        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            ClassKind kind = classDescriptor.getKind();
            return !(kind == ClassKind.OBJECT || kind == ClassKind.CLASS_OBJECT);
        }

        return false;
    }

    private void addJetTopLevelFunctions() {
        String actualPrefix = jetResult.getResult().getPrefixMatcher().getPrefix();
        Project project = getPosition().getProject();

        JetShortNamesCache namesCache = JetShortNamesCache.getKotlinInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

        // TODO: Fix complete extension not only on contains
        for (String name : functionNames) {
            if (name.contains(actualPrefix)) {
                jetResult.addAllElements(namesCache.getTopLevelFunctionDescriptorsByName(
                        name, jetReference.getExpression(), getResolveSession(), scope));
            }
        }
    }

    private void addJetTopLevelObjects() {
        Project project = getPosition().getProject();
        JetShortNamesCache namesCache = JetShortNamesCache.getKotlinInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<String> objectNames = namesCache.getAllTopLevelObjectNames();

        for (String name : objectNames) {
            if (jetResult.getResult().getPrefixMatcher().prefixMatches(name)) {
                jetResult.addAllElements(namesCache.getTopLevelObjectsByName(name, jetReference.getExpression(), getResolveSession(), scope));
            }
        }
    }

    private boolean shouldRunOnlyTypeCompletion() {
        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        JetTypeReference typeReference = PsiTreeUtil.getParentOfType(getPosition(), JetTypeReference.class);
        if (typeReference != null) {
            JetSimpleNameExpression firstPartReference = PsiTreeUtil.findChildOfType(typeReference, JetSimpleNameExpression.class);
            return firstPartReference == jetReference.getExpression();
        }

        return false;
    }

    private boolean shouldRunTopLevelCompletion() {
        if (parameters.getInvocationCount() < 2) {
            return false;
        }

        PsiElement element = getPosition();
        if (getPosition().getNode().getElementType() == JetTokens.IDENTIFIER) {
            if (element.getParent() instanceof JetSimpleNameExpression) {
                if (!JetPsiUtil.isSelectorInQualified((JetSimpleNameExpression) element.getParent())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldRunExtensionsCompletion() {
        return !(parameters.getInvocationCount() <= 1 && jetResult.getResult().getPrefixMatcher().getPrefix().length() < 3);
    }

    private void addReferenceVariants(@NotNull final Condition<DeclarationDescriptor> filterCondition) {
        Collection<DeclarationDescriptor> descriptors = TipsManager.getReferenceVariants(
                jetReference.getExpression(), getExpressionBindingContext());

        Collection<DeclarationDescriptor> filterDescriptors = Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                return descriptor != null && filterCondition.value(descriptor);
            }
        });

        jetResult.addAllElements(filterDescriptors);
    }

    private boolean isVisibleDescriptor(DeclarationDescriptor descriptor) {
        if (parameters.getInvocationCount() >= 2) {
            // Show everything if user insist on showing completion list
            return true;
        }

        if (descriptor instanceof DeclarationDescriptorWithVisibility) {
            if (inDescriptor != null) {
                //noinspection ConstantConditions
                return Visibilities.isVisible((DeclarationDescriptorWithVisibility) descriptor, inDescriptor);
            }
        }

        return true;
    }

    private BindingContext getExpressionBindingContext() {
        return jetResult.getBindingContext();
    }

    private CancelableResolveSession getResolveSession() {
        return jetResult.getResolveSession();
    }

    private PsiElement getPosition() {
        return parameters.getPosition();
    }

    public JetCompletionResultSet getJetResult() {
        return jetResult;
    }

    public CompletionParameters getParameters() {
        return parameters;
    }
}
