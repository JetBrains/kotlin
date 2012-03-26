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

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {

    private static class CompletionSession {
        public boolean isSomethingAdded = false;
        public int customInvocationCount = 0;
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
                addClasses(parameters, result);
            }
            else {
                for (Object variant : jetReference.getVariants()) {
                    if (isTypeDeclaration(variant)) {
                        addReferenceVariant(result, variant, session);
                    }
                }
            }

            return;
        }

        for (Object variant : jetReference.getVariants()) {
            addReferenceVariant(result, variant, session);
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
            addClasses(parameters, result);
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

        BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) position.getContainingFile());

        for (DeclarationDescriptor jetCallableExtension : jetCallableExtensions) {
            result.addElement(DescriptorLookupConverter.createLookupElement(context, jetCallableExtension));
        }
    }

    private static void addReferenceVariant(
            @NotNull CompletionResultSet result,
            @NotNull Object variant,
            @NotNull CompletionSession session) {

        if (variant instanceof LookupElement) {
            addCompletionToResult(result, (LookupElement) variant, session);
        }
        else {
            addCompletionToResult(result, LookupElementBuilder.create(variant.toString()), session);
        }
    }

    public static boolean isTypeDeclaration(@NotNull Object variant) {
        if (variant instanceof LookupElement) {
            Object object = ((LookupElement)variant).getObject();
            if (object instanceof JetLookupObject) {
                DeclarationDescriptor descriptor = ((JetLookupObject) object).getDescriptor();
                return (descriptor instanceof ClassDescriptor) ||
                       (descriptor instanceof NamespaceDescriptor) ||
                       (descriptor instanceof TypeParameterDescriptor);
            }
        }

        return false;
    }

    private static void addJetTopLevelFunctions(JetSimpleNameExpression expression, @NotNull CompletionResultSet result, @NotNull PsiElement position,
                                                @NotNull CompletionSession session) {

        String actualPrefix = result.getPrefixMatcher().getPrefix();

        Project project = position.getProject();

        JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

        BindingContext resolutionContext = namesCache.getResolutionContext(scope);

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
            @NotNull final CompletionResultSet result
    ) {
        JetClassCompletionContributor.addClasses(parameters, result, new Consumer<LookupElement>() {
            @Override
            public void consume(@NotNull LookupElement element) {
                result.addElement(element);
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
                JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) element.getParent();

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
                        return (JetSimpleNameReference) reference;
                    }
                }
            }
        }

        return null;
    }

    private static void addCompletionToResult(
            @NotNull CompletionResultSet result,
            @NotNull LookupElement element,
            @NotNull CompletionSession session) {

        if (result.getPrefixMatcher().prefixMatches(element)) {
            result.addElement(element);
            session.isSomethingAdded = true;
        }
    }

    //private static LookupPositionObject getLookupPosition(LookupElement element) {
    //    Object lookupObject = element.getObject();
    //    if (lookupObject instanceof PsiElement) {
    //        return new LookupPositionObject((PsiElement) lookupObject);
    //    }
    //    else if (lookupObject instanceof JetLookupObject) {
    //        PsiElement psiElement = ((JetLookupObject) lookupObject).getPsiElement();
    //        if (psiElement != null) {
    //            return new LookupPositionObject(psiElement);
    //        }
    //    }
    //
    //    return null;
    //}

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
