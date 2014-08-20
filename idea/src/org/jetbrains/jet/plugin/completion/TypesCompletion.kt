/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Consumer
import org.jetbrains.jet.asJava.KotlinLightClass
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.java.JavaResolverPsiUtils
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.caches.JetFromJavaDescriptorHelper
import org.jetbrains.jet.plugin.caches.JetShortNamesCache
import org.jetbrains.jet.plugin.completion.handlers.JetJavaClassInsertHandler
import org.jetbrains.jet.plugin.project.ProjectStructureUtil
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies

class TypesCompletion(val parameters: CompletionParameters, val resolveSession: ResolveSessionForBodies, val prefixMatcher: PrefixMatcher) {
    fun addAllTypes(result: LookupElementsCollector) {
        result.addDescriptorElements(KotlinBuiltIns.getInstance().getNonPhysicalClasses())

        val project = parameters.getOriginalFile().getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        result.addDescriptorElements(namesCache.getJetClassesDescriptors({ prefixMatcher.prefixMatches(it!!) }, resolveSession, GlobalSearchScope.allScope(project)))

        if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as JetFile)) {
            addAdaptedJavaCompletion(result)
        }
    }

    /**
     * Add java elements with performing conversion to kotlin elements if necessary.
     */
    private fun addAdaptedJavaCompletion(result: LookupElementsCollector) {
        JavaClassNameCompletionContributor.addAllClasses(parameters, false, prefixMatcher, object : Consumer<LookupElement> {
            override fun consume(lookupElement: LookupElement?) {
                if (lookupElement is JavaPsiClassReferenceElement) {
                    val psiClass = lookupElement.getObject()

                    if (addJavaClassAsJetLookupElement(psiClass, result)) return

                    result.addElement(object : LookupElementDecorator<LookupElement>(lookupElement) {
                        override fun handleInsert(context: InsertionContext) {
                            JetJavaClassInsertHandler.handleInsert(context, lookupElement)
                        }
                    })
                }
            }
        })
    }

    private fun addJavaClassAsJetLookupElement(aClass: PsiClass, result: LookupElementsCollector): Boolean {
        if (aClass is KotlinLightClass) {
            // Do nothing. Kotlin not-compiled class should have already been added as kotlin element before.
            return true
        }

        if (JavaResolverPsiUtils.isCompiledKotlinClass(aClass)) {
            if (JetFromJavaDescriptorHelper.getCompiledClassKind(aClass) != ClassKind.CLASS_OBJECT) {
                val qualifiedName = aClass.getQualifiedName()
                if (qualifiedName != null) {
                    result.addDescriptorElements(ResolveSessionUtils.getClassDescriptorsByFqName(resolveSession, FqName(qualifiedName)))
                }
            }

            return true
        }

        return false
    }
}
