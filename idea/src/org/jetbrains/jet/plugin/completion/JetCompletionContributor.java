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
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor {
    public JetCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                 final @NotNull CompletionResultSet result) {

                       final HashSet<LookupPositionObject> positions = new HashSet<LookupPositionObject>();

                       if (result.getPrefixMatcher().getPrefix().isEmpty()) {
                           return;
                       }

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

                       final JetSimpleNameReference jetReference = getJetReference(parameters);
                       if (jetReference != null) {
                           for (Object variant : jetReference.getVariants()) {
                               addReferenceVariant(result, variant, positions);
                           }

                           if (shouldRunTopLevelCompletion(parameters)) {
                               addClasses(parameters, result, positions);
                               addJetTopLevelFunctions(result, position, positions);
                               addJetExtensionFunctions(jetReference.getExpression(), result, position);
                           }

                           result.stopHere();
                       }
                   }
               });
    }

    private static void addJetExtensionFunctions(JetSimpleNameExpression expression, CompletionResultSet result, PsiElement position) {

        BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) position.getContainingFile());
        JetExpression receiverExpression = expression.getReceiverExpression();


        if (receiverExpression != null) {
            final JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);
            final JetScope scope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression);

            if (expressionType != null && scope != null) {
                JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
                Collection<String> extensionFunctionsNames = namesCache.getAllJetExtensionFunctionsNames(
                        GlobalSearchScope.allScope(position.getProject()));

                HashSet<String> functionFQNs = new HashSet<String>();

                // Collect all possible extension function qualified names
                for (String name : extensionFunctionsNames) {
                    if (result.getPrefixMatcher().prefixMatches(name)) {
                        Collection<PsiElement> extensionFunctions =
                                namesCache.getJetExtensionFunctionsByName(name, GlobalSearchScope.allScope(position.getProject()));

                        for (PsiElement extensionFunction : extensionFunctions) {
                            if (extensionFunction instanceof JetNamedFunction) {
                                functionFQNs.add(JetPsiUtil.getFQName((JetNamedFunction) extensionFunction));
                            } else if (extensionFunction instanceof PsiMethod) {
                                final PsiMethod function = (PsiMethod) extensionFunction;
                                final PsiClass containingClass = function.getContainingClass();

                                if (containingClass != null) {
                                    final String classFQN = containingClass.getQualifiedName();

                                    if (classFQN != null) {
                                        final String classParentFQN = QualifiedNamesUtil.withoutLastSegment(classFQN);
                                        functionFQNs.add(QualifiedNamesUtil.combine(classParentFQN, function.getName()));
                                    }
                                }
                            }
                        }
                    }
                }

                // Iterate through the function with attempt to resolve found functions
                for (String functionFQN : functionFQNs) {
                    // System.out.println(functionFQN);

                    List<FunctionDescriptor> functionDescriptors = ExpressionTypingUtils.canFindSuitableCall(
                            functionFQN, position.getProject(), receiverExpression, expressionType, scope);

                    // System.out.println(!functionDescriptors.isEmpty());

                    if (!functionDescriptors.isEmpty()) {
                        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
                            result.addElement(DescriptorLookupConverter.createLookupElement(context, functionDescriptor));
                        }
                    }
                }
            }
        }
    }

    private static void addReferenceVariant(
            @NotNull CompletionResultSet result,
            @NotNull Object variant,
            @NotNull final HashSet<LookupPositionObject> positions) {

        if (variant instanceof LookupElement) {
            addCompletionToResult(result, (LookupElement) variant, positions);
        }
        else {
            addCompletionToResult(result, LookupElementBuilder.create(variant.toString()), positions);
        }
    }

    private static void addJetTopLevelFunctions(@NotNull CompletionResultSet result, @NotNull PsiElement position,
                                                @NotNull final HashSet<LookupPositionObject> positions) {

        String actualPrefix = result.getPrefixMatcher().getPrefix();

        final Project project = position.getProject();

        final JetShortNamesCache namesCache = JetCacheManager.getInstance(position.getProject()).getNamesCache();
        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        final Collection<String> functionNames = namesCache.getAllTopLevelFunctionNames();

        BindingContext resolutionContext = namesCache.getResolutionContext(scope);

        for (String name : functionNames) {
            if (name.contains(actualPrefix)) {
                for (FunctionDescriptor function : namesCache.getTopLevelFunctionDescriptorsByName(name, scope)) {
                    addCompletionToResult(result, DescriptorLookupConverter.createLookupElement(resolutionContext, function), positions);
                }
            }
        }
    }

    /**
     * Jet classes will be added as java completions for unification
     */
    private static void addClasses(
            @NotNull final CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final HashSet<LookupPositionObject> positions) {

        JetClassCompletionContributor.addClasses(parameters, result, new Consumer<LookupElement>() {
            @Override
            public void consume(@NotNull LookupElement element) {
                addCompletionToResult(result, element, positions);
            }
        });
    }

    private static boolean shouldRunTopLevelCompletion(@NotNull CompletionParameters parameters) {
        final PsiElement element = parameters.getPosition();

        if (parameters.getInvocationCount() > 1) {
            return true;
        }

        if (element.getNode().getElementType() == JetTokens.IDENTIFIER) {
            if (element.getParent() instanceof JetSimpleNameExpression) {
                JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) element.getParent();
                if (PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class) != null) {
                    return false;
                }

                if (PsiTreeUtil.getParentOfType(nameExpression, JetUserType.class) != null) {
                    return parameters.getInvocationCount() == 1;
                }
            }
        }

        return false;
    }

    @Nullable
    private static JetSimpleNameReference getJetReference(@NotNull CompletionParameters parameters) {
        final PsiElement element = parameters.getPosition();
        if (element.getParent() != null) {
            final PsiElement parent = element.getParent();
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
            @NotNull final CompletionResultSet result,
            @NotNull LookupElement element,
            @NotNull HashSet<LookupPositionObject> positions) {

        final LookupPositionObject lookupPosition = getLookupPosition(element);
        if (lookupPosition != null) {
            if (!positions.contains(lookupPosition)) {
                positions.add(lookupPosition);
                result.addElement(element);
            }

            // There is already an element with same position - ignore duplicate
        }
        else {
            result.addElement(element);
        }
    }

    private static LookupPositionObject getLookupPosition(LookupElement element) {
        final Object lookupObject = element.getObject();
        if (lookupObject instanceof PsiElement) {
//            PsiElement psiElement = (PsiElement) lookupObject;
            return new LookupPositionObject((PsiElement) lookupObject);
        }
        else if (lookupObject instanceof JetLookupObject) {
//            JetLookupObject jetLookupObject = (JetLookupObject) lookupObject;
            final PsiElement psiElement = ((JetLookupObject) lookupObject).getPsiElement();
            if (psiElement != null) {
                return new LookupPositionObject(psiElement);
            }
        }

        return null;
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
