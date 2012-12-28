/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.completion.weigher.JetCompletionSorting;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;

public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(
                           @NotNull CompletionParameters parameters,
                           ProcessingContext context,
                           @NotNull CompletionResultSet result
                   ) {

                       PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       JetSimpleNameReference jetReference = getJetReference(parameters);
                       if (jetReference != null) {
                           result.restartCompletionWhenNothingMatches();
                           CompletionSession session = new CompletionSession(parameters, result, jetReference, position);

                           session.completeForReference();

                           if (!session.getJetResult().isSomethingAdded() && session.getCustomInvocationCount() == 0) {
                               // Rerun completion if nothing was found
                               session = new CompletionSession(parameters, result, jetReference, position, 1);

                               session.completeForReference();
                           }
                       }

                       // Prevent from adding reference variants from standard reference contributor
                       result.stopHere();
                   }
               });
    }

    @Nullable
    private static JetSimpleNameReference getJetReference(@NotNull CompletionParameters parameters) {
        PsiElement element = parameters.getPosition();
        if (element.getParent() != null) {
            PsiElement parent = element.getParent();
            PsiReference[] references = parent.getReferences();

            if (references.length != 0) {
                for (PsiReference reference : references) {
                    if (reference instanceof JetSimpleNameReference) {
                        return (JetSimpleNameReference)reference;
                    }
                }
            }
        }

        return null;
    }

    private static class CompletionSession {
        @Nullable
        private final DeclarationDescriptor inDescriptor;
        private final int customInvocationCount;
        private final CompletionParameters parameters;
        private final JetCompletionResultSet jetResult;
        private final JetSimpleNameReference jetReference;

        public CompletionSession(
                @NotNull CompletionParameters parameters,
                @NotNull CompletionResultSet result,
                @NotNull JetSimpleNameReference jetReference,
                @NotNull PsiElement position
        ) {

            this(parameters, result, jetReference, position, parameters.getInvocationCount());
        }

        public CompletionSession(
                @NotNull CompletionParameters parameters,
                @NotNull CompletionResultSet result,
                @NotNull JetSimpleNameReference jetReference,
                @NotNull PsiElement position,
                int customInvocationCount
        ) {
            this.parameters = parameters;
            this.jetReference = jetReference;
            this.customInvocationCount = customInvocationCount;

            ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) position.getContainingFile());
            BindingContext expressionBindingContext = ResolveSessionUtils.resolveToExpression(resolveSession, jetReference.getExpression());
            JetScope scope = expressionBindingContext.get(BindingContext.RESOLUTION_SCOPE, jetReference.getExpression());

            inDescriptor = scope != null ? scope.getContainingDeclaration() : null;

            this.jetResult = new JetCompletionResultSet(
                    JetCompletionSorting.addJetSorting(parameters, result),
                    resolveSession,
                    expressionBindingContext, new Condition<DeclarationDescriptor>() {
                @Override
                public boolean value(DeclarationDescriptor descriptor) {
                    return isVisibleDescriptor(descriptor);
                }
            });
        }

        void completeForReference() {
            if (isOnlyKeywordCompletion(getPosition())) {
                return;
            }

            if (shouldRunOnlyTypeCompletion()) {
                if (customInvocationCount >= 1) {
                    JetTypesCompletionHelper.addJetTypes(parameters, jetResult);
                }
                else {
                    addReferenceVariants(new Condition<DeclarationDescriptor>() {
                        @Override
                        public boolean value(DeclarationDescriptor descriptor) {
                            return isPartOfTypeDeclaration(descriptor);
                        }
                    });
                }

                return;
            }

            addReferenceVariants(Conditions.<DeclarationDescriptor>alwaysTrue());

            String prefix = jetResult.getResult().getPrefixMatcher().getPrefix();

            // Try to avoid computing not-imported descriptors for empty prefix
            if (prefix.isEmpty()) {
                if (customInvocationCount < 2) {
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

        private static boolean isOnlyKeywordCompletion(PsiElement position) {
            return PsiTreeUtil.getParentOfType(position, JetModifierList.class) != null;
        }

        private void addJetExtensions() {
            JetShortNamesCache namesCache = JetCacheManager.getInstance(getPosition().getProject()).getNamesCache();

            Collection<DeclarationDescriptor> jetCallableExtensions = namesCache.getJetCallableExtensions(
                    jetResult.getShortNameFilter(),
                    jetReference.getExpression(),
                    getResolveSession(),
                    GlobalSearchScope.allScope(getPosition().getProject()));

            jetResult.addAllElements(jetCallableExtensions);
        }

        public static boolean isPartOfTypeDeclaration(@NotNull DeclarationDescriptor descriptor) {
            if (descriptor instanceof NamespaceDescriptor || descriptor instanceof TypeParameterDescriptor) {
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

            JetShortNamesCache namesCache = JetCacheManager.getInstance(project).getNamesCache();
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
            JetShortNamesCache namesCache = JetCacheManager.getInstance(getPosition().getProject()).getNamesCache();
            GlobalSearchScope scope = GlobalSearchScope.allScope(getPosition().getProject());
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
            if (customInvocationCount == 0) {
                return false;
            }

            PsiElement element = getPosition();
            if (getPosition().getNode().getElementType() == JetTokens.IDENTIFIER) {
                if (element.getParent() instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression nameExpression = (JetSimpleNameExpression)element.getParent();

                    // Top level completion should be executed for simple name which is not in qualified expression
                    if (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) != null) {
                        return false;
                    }

                    // Don't call top level completion in qualified named position of user type
                    PsiElement parent = nameExpression.getParent();
                    if (parent instanceof JetUserType && ((JetUserType) parent).getQualifier() != null) {
                        return false;
                    }

                    return true;
                }
            }

            return false;
        }

        private boolean shouldRunExtensionsCompletion() {
            return !(customInvocationCount == 0 && jetResult.getResult().getPrefixMatcher().getPrefix().length() < 3);
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
            if (customInvocationCount >= 2) {
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

        private ResolveSession getResolveSession() {
            return jetResult.getResolveSession();
        }

        private PsiElement getPosition() {
            return parameters.getPosition();
        }

        public JetCompletionResultSet getJetResult() {
            return jetResult;
        }

        public int getCustomInvocationCount() {
            return customInvocationCount;
        }
    }
}
