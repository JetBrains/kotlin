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
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.completion.handlers.JetJavaClassInsertHandler;
import org.jetbrains.jet.plugin.project.JsModuleDetector;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetClassCompletionContributor extends CompletionContributor {
    public JetClassCompletionContributor() {}

    /**
     * Jet classes will be added as java completions for unification
     */
    static void addClasses(
            @NotNull final CompletionParameters parameters,
            @NotNull final CompletionResultSet result,
            @NotNull final BindingContext jetContext,
            @NotNull final ResolveSession resolveSession,
            @NotNull final Consumer<LookupElement> consumer
    ) {
        CompletionResultSet tempResult = result.withPrefixMatcher(CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));

        final Collection<DeclarationDescriptor> jetOnlyClasses = JetShortNamesCache.getJetOnlyTypes();
        for (DeclarationDescriptor jetOnlyClass : jetOnlyClasses) {
            consumer.consume(DescriptorLookupConverter.createLookupElement(resolveSession, jetContext, jetOnlyClass));
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

                                PsiClass psiClass = javaPsiReferenceElement.getObject();
                                if (addAsJetLookupElement(parameters, psiClass, resolveSession, jetContext, consumer)) {
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
                    }, resolveSession);

            for (ClassDescriptor descriptor : descriptors) {
                consumer.consume(DescriptorLookupConverter.createLookupElement(resolveSession, jetContext, descriptor));
            }
        }
    }

    private static boolean addAsJetLookupElement(
            CompletionParameters parameters,
            PsiClass aClass,
            ResolveSession resolveSession,
            BindingContext context,
            Consumer<LookupElement> consumer
    ) {
        if (aClass instanceof JetLightClass) {
            Project project = parameters.getPosition().getProject();

            Collection<JetClassOrObject> classOrObjects =
                    JetFullClassNameIndex.getInstance().get(aClass.getQualifiedName(), project, GlobalSearchScope.allScope(project));

            for (JetClassOrObject classOrObject : classOrObjects) {
                if (classOrObject.getContainingFile().getOriginalFile().equals(aClass.getContainingFile())) {
                    Collection<ClassDescriptor> classDescriptors = ResolveSessionUtils.getClassDescriptorsByFqName(
                            resolveSession, ((JetLightClass) aClass).getFqName());
                    for (ClassDescriptor descriptor : classDescriptors) {
                        consumer.consume(DescriptorLookupConverter.createLookupElement(resolveSession, context, descriptor));
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
