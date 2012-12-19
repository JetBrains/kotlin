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
import com.intellij.psi.PsiClass;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;
import org.jetbrains.jet.plugin.libraries.DecompiledDataFactory;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

public class JetTypesCompletionHelper {
    private JetTypesCompletionHelper() {}

    static void addJetTypes(
            @NotNull final CompletionParameters parameters,
            @NotNull final JetCompletionResultSet jetCompletionResult
    ) {
        jetCompletionResult.addAllElements(KotlinBuiltIns.getInstance().getNonPhysicalClasses());

        Project project = parameters.getOriginalFile().getProject();
        JetShortNamesCache namesCache = JetShortNamesCache.getKotlinInstance(project);
        jetCompletionResult.addAllElements(namesCache.getJetClassesDescriptors(
                jetCompletionResult.getShortNameFilter(), jetCompletionResult.getResolveSession()));

        if (!JsModuleDetector.isJsModule((JetFile) parameters.getOriginalFile())) {
            addAdaptedJavaCompletion(parameters,jetCompletionResult);
        }
    }

    /**
     * Add java elements with performing conversion to kotlin elements if necessary.
     */
    static void addAdaptedJavaCompletion(
            @NotNull final CompletionParameters parameters,
            @NotNull final JetCompletionResultSet jetCompletionResult
    ) {
        CompletionResultSet tempResult = jetCompletionResult.getResult().withPrefixMatcher(
                CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));
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
                            if (addJavaClassAsJetLookupElement(psiClass, jetCompletionResult)) {
                                return;
                            }

                            jetCompletionResult.addElement(DescriptorLookupConverter.setCustomInsertHandler(javaPsiReferenceElement));
                        }
                    }
                });

    }

    private static boolean addJavaClassAsJetLookupElement(PsiClass aClass, JetCompletionResultSet jetCompletionResult) {
        if (aClass instanceof JetLightClass) {
            // Do nothing. Kotlin not-compiled class should have already been added as kotlin element before.
            return true;
        }

        if (DecompiledDataFactory.isCompiledFromKotlin(aClass)) {
            if (!DecompiledDataFactory.isKotlinObject(aClass)) {
                String qualifiedName = aClass.getQualifiedName();
                if (qualifiedName != null) {
                    FqName fqName = new FqName(qualifiedName);
                    jetCompletionResult.addAllElements(
                            ResolveSessionUtils.getClassDescriptorsByFqName(jetCompletionResult.getResolveSession(), fqName));
                }
            }

            return true;
        }

        return false;
    }
}
