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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.completion.handlers.JetJavaClassInsertHandler;
import org.jetbrains.jet.plugin.project.JsModuleDetector;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetClassCompletionContributor extends CompletionContributor {
    public JetClassCompletionContributor() {
        // Should be removed in new idea
        //extend(CompletionType.CLASS_NAME, PlatformPatterns.psiElement(),
        //       new CompletionProvider<CompletionParameters>() {
        //           @Override
        //           protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
        //                                         final @NotNull CompletionResultSet result) {
        //               final CompletionResultSet jetResult = JetCompletionSorting.addJetSorting(parameters, result);
        //
        //               final PsiElement position = parameters.getPosition();
        //               if (!(position.getContainingFile() instanceof JetFile)) {
        //                   return;
        //               }
        //
        //               final PsiReference ref = position.getContainingFile().findReferenceAt(parameters.getOffset());
        //               if (ref instanceof JetSimpleNameReference) {
        //                   addClasses(parameters, result, new Consumer<LookupElement>() {
        //                       @Override
        //                       public void consume(LookupElement lookupElement) {
        //                           jetResult.addElement(lookupElement);
        //                       }
        //                   });
        //               }
        //
        //               result.stopHere();
        //           }
        //       });
    }

    /**
     * Jet classes will be added as java completions for unification
     */
    static void addClasses(
            @NotNull final CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final Consumer<LookupElement> consumer
    ) {
        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));

        // TODO: Make icon for standard types
        final Collection<DeclarationDescriptor> jetOnlyClasses = JetShortNamesCache.getJetOnlyTypes();
        final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile)parameters.getPosition().getContainingFile()).getBindingContext();

        for (DeclarationDescriptor jetOnlyClass : jetOnlyClasses) {
            consumer.consume(DescriptorLookupConverter.createLookupElement(bindingContext, jetOnlyClass));
        }

        if (!JsModuleDetector.isJsModule((JetFile)parameters.getOriginalFile())) {
            JavaClassNameCompletionContributor.addAllClasses(
                    parameters,
                    false,
                    JavaCompletionSorting.addJavaSorting(parameters, tempResult).getPrefixMatcher(),
                    new Consumer<LookupElement>() {
                        @Override
                        public void consume(LookupElement lookupElement) {
                            if (lookupElement instanceof JavaPsiClassReferenceElement) {
                                JavaPsiClassReferenceElement javaPsiReferenceElement = (JavaPsiClassReferenceElement) lookupElement;

                                PsiClass object = javaPsiReferenceElement.getObject();
                                if (addAsJetLookupElement(object, bindingContext, consumer)) {
                                    return;
                                }

                                // Redefine standard java insert handler which is going to insert fqn
                                javaPsiReferenceElement.setInsertHandler(JetJavaClassInsertHandler.JAVA_CLASS_INSERT_HANDLER);
                                consumer.consume(lookupElement);
                            }
                        }
                    });
        }
        else {
            Project project = parameters.getOriginalFile().getProject();
            JetShortNamesCache namesCache = JetCacheManager.getInstance(project).getNamesCache();
            Collection<ClassDescriptor> descriptors = namesCache.getJetClassesDescriptors(
                    new Condition<String>() {
                        @Override
                        public boolean value(String shortName) {
                            return result.getPrefixMatcher().prefixMatches(shortName);
                        }
                    },
                    (JetFile) parameters.getOriginalFile());

            for (ClassDescriptor descriptor : descriptors) {
                consumer.consume(DescriptorLookupConverter.createLookupElement(bindingContext, descriptor));
            }
        }
    }

    private static boolean addAsJetLookupElement(PsiClass aClass, BindingContext bindingContext, Consumer<LookupElement> consumer) {
        if (aClass instanceof JetLightClass) {
            ClassDescriptor descriptor = bindingContext.get(
                    BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, ((JetLightClass)aClass).getFqName());

            if (descriptor != null) {
                LookupElement element = DescriptorLookupConverter.createLookupElement(bindingContext, descriptor);
                consumer.consume(element);
                return true;
            }
        }

        return false;
    }
}
