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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.TipsManager;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {

    private static class CompletionSession {
        public boolean isSomethingAdded = false;
        public int customInvocationCount = 0;

        @Nullable
        public DeclarationDescriptor inDescriptor = null;
    }

    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                           @NotNull CompletionResultSet result) {
                       result.restartCompletionWhenNothingMatches();

                       CompletionSession session = new CompletionSession();
                       session.customInvocationCount = parameters.getInvocationCount();

                       PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       JetSimpleNameReference jetReference = getJetReference(parameters);
                       if (jetReference != null) {

                           BindingContext jetContext =
                                   WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile)position.getContainingFile())
                                           .getBindingContext();

                           JetScope scope = jetContext.get(BindingContext.RESOLUTION_SCOPE, jetReference.getExpression());
                           session.inDescriptor = scope != null ? scope.getContainingDeclaration() : null;

                           completeForReference(parameters, result, position, jetReference, session);

                           if (!session.isSomethingAdded && session.customInvocationCount == 0) {
                               // Rerun completion if nothing was found
                               session.customInvocationCount = 1;
                               completeForReference(parameters, result, position, jetReference, session);
                           }
                       }

                       // Prevent from adding reference variants from standard reference contributor
                       result.stopHere();
                   }
               });
    }

    private static void completeForReference(
            @NotNull CompletionParameters parameters,
            @NotNull CompletionResultSet result,
            @NotNull PsiElement position,
            @NotNull JetSimpleNameReference jetReference,
            @NotNull CompletionSession session
    ) {
        if (isOnlyKeywordCompletion(position)) {
            return;
        }

        if (shouldRunTypeCompletionOnly(position, jetReference)) {
            if (session.customInvocationCount > 0) {
                addClasses(parameters, result, session);
            }
            else {
                for (LookupElement variant : getReferenceVariants(jetReference, result, session)) {
                    if (isTypeDeclaration(variant)) {
                        addCheckedCompletionToResult(result, variant, session);
                    }
                }
            }

            return;
        }

        for (LookupElement variant : getReferenceVariants(jetReference, result, session)) {
            addCheckedCompletionToResult(result, variant, session);
        }

        String prefix = result.getPrefixMatcher().getPrefix();

        // Try to avoid computing not-imported descriptors for empty prefix
        if (prefix.isEmpty()) {
            if (session.customInvocationCount < 2) {
                return;
            }

            if (PsiTreeUtil.getParentOfType(jetReference.getElement(), JetDotQualifiedExpression.class) == null) {
                return;
            }
        }

        if (shouldRunTopLevelCompletion(parameters, session)) {
            addClasses(parameters, result, session);
            addJetTopLevelFunctions(jetReference.getExpression(), result, position, session);
        }

        if (shouldRunExtensionsCompletion(parameters, prefix, session)) {
            addJetExtensions(jetReference.getExpression(), result, position);
        }
    }

    private static boolean isOnlyKeywordCompletion(PsiElement position) {
        return PsiTreeUtil.getParentOfType(position, JetModifierList.class) != null;
    }

    private static void addJetExtensions(JetSimpleNameExpression expression, CompletionResultSet result, PsiElement position) {
        final PrefixMatcher prefixMatcher = result.getPrefixMatcher();
        JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
        Condition<String> matchPrefixCondition = new Condition<String>() {
            @Override
            public boolean value(String callableName) {
                return prefixMatcher.prefixMatches(callableName);
            }
        };

        Collection<DeclarationDescriptor> jetCallableExtensions = namesCache.getJetCallableExtensions(
                matchPrefixCondition, expression, GlobalSearchScope.allScope(position.getProject()));

        BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile)position.getContainingFile())
                .getBindingContext();

        for (DeclarationDescriptor jetCallableExtension : jetCallableExtensions) {
            result.addElement(DescriptorLookupConverter.createLookupElement(context, jetCallableExtension));
        }
    }

    public static boolean isTypeDeclaration(@NotNull Object variant) {
        if (variant instanceof LookupElement) {
            Object object = ((LookupElement)variant).getObject();
            if (object instanceof JetLookupObject) {
                DeclarationDescriptor descriptor = ((JetLookupObject)object).getDescriptor();
                return (descriptor instanceof ClassDescriptor) ||
                       (descriptor instanceof NamespaceDescriptor) ||
                       (descriptor instanceof TypeParameterDescriptor);
            }
        }

        return false;
    }

    private static void addJetTopLevelFunctions(JetSimpleNameExpression expression,
            @NotNull CompletionResultSet result,
            @NotNull PsiElement position,
            @NotNull CompletionSession session) {

        String actualPrefix = result.getPrefixMatcher().getPrefix();

        Project project = position.getProject();

        JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

        BindingContext resolutionContext =
                WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile)position.getContainingFile()).getBindingContext();

        for (String name : functionNames) {
            if (name.contains(actualPrefix)) {
                for (FunctionDescriptor function : namesCache.getTopLevelFunctionDescriptorsByName(name, expression, scope)) {
                    addCompletionToResult(result, DescriptorLookupConverter.createLookupElement(resolutionContext, function), session);
                }
            }
        }
    }

    /**
     * Jet classes will be added as java completions for unification
     */
    private static void addClasses(
            @NotNull CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final CompletionSession session
    ) {
        JetClassCompletionContributor.addClasses(parameters, result, new Consumer<LookupElement>() {
            @Override
            public void consume(@NotNull LookupElement element) {
                addCompletionToResult(result, element, session);
            }
        });
    }

    private static boolean shouldRunTypeCompletionOnly(PsiElement position, JetSimpleNameReference jetReference) {
        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        JetTypeReference typeReference = PsiTreeUtil.getParentOfType(position, JetTypeReference.class);
        if (typeReference != null) {
            JetSimpleNameExpression firstPartReference = PsiTreeUtil.findChildOfType(typeReference, JetSimpleNameExpression.class);
            return firstPartReference == jetReference.getExpression();
        }

        return false;
    }

    private static boolean shouldRunTopLevelCompletion(@NotNull CompletionParameters parameters, CompletionSession session) {
        if (session.customInvocationCount == 0) {
            return false;
        }

        PsiElement element = parameters.getPosition();
        if (element.getNode().getElementType() == JetTokens.IDENTIFIER) {
            if (element.getParent() instanceof JetSimpleNameExpression) {
                JetSimpleNameExpression nameExpression = (JetSimpleNameExpression)element.getParent();

                // Top level completion should be executed for simple which is not in qualified expression
                return (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) == null);
            }
        }

        return false;
    }

    private static boolean shouldRunExtensionsCompletion(CompletionParameters parameters, String prefix, CompletionSession session) {
        if (session.customInvocationCount == 0 && prefix.length() < 3) {
            return false;
        }

        return getJetReference(parameters) != null;
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

    private static void addCompletionToResult(
            @NotNull CompletionResultSet result,
            @NotNull LookupElement element,
            @NotNull CompletionSession session
    ) {
        if (!result.getPrefixMatcher().prefixMatches(element) || !isVisibleElement(element, session)) {
            return;
        }

        addCheckedCompletionToResult(result, element, session);
    }

    private static void addCheckedCompletionToResult(
            @NotNull CompletionResultSet result,
            @NotNull LookupElement element,
            @NotNull CompletionSession session
    ) {
        result.addElement(element);
        session.isSomethingAdded = true;
    }

    private static boolean isVisibleElement(LookupElement element, CompletionSession session) {
        if (session.inDescriptor != null) {
            if (element.getObject() instanceof JetLookupObject) {
                JetLookupObject jetObject = (JetLookupObject)element.getObject();
                DeclarationDescriptor descriptor = jetObject.getDescriptor();
                return isVisibleDescriptor(descriptor, session);
            }
        }

        return true;
    }

    private static boolean isVisibleDescriptor(DeclarationDescriptor descriptor, CompletionSession session) {
        if (session.customInvocationCount >= 2) {
            // Show everything if user insist on showing completion list
            return true;
        }

        if (descriptor instanceof DeclarationDescriptorWithVisibility) {
            return Visibilities.isVisible((DeclarationDescriptorWithVisibility)descriptor, session.inDescriptor);
        }

        return true;
    }

    @NotNull
    private static LookupElement[] getReferenceVariants(
            @NotNull JetSimpleNameReference reference,
            @NotNull final CompletionResultSet result,
            @NotNull final CompletionSession session
    ) {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile)reference.getExpression().getContainingFile())
                .getBindingContext();

        Collection<DeclarationDescriptor> descriptors = TipsManager.getReferenceVariants(reference.getExpression(), bindingContext);

        Collection<DeclarationDescriptor> checkedDescriptors = Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                if (descriptor == null) {
                    return false;
                }

                return result.getPrefixMatcher().prefixMatches(descriptor.getName()) && isVisibleDescriptor(descriptor, session);
            }
        });

        return DescriptorLookupConverter.collectLookupElements(bindingContext, checkedDescriptors);
    }
}
