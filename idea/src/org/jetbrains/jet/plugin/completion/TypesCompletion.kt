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
import com.intellij.psi.PsiClass
import org.jetbrains.jet.asJava.KotlinLightClass
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.java.JavaResolverPsiUtils
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.caches.JetFromJavaDescriptorHelper
import org.jetbrains.jet.plugin.project.ProjectStructureUtil
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.caches.KotlinIndicesHelper
import org.jetbrains.jet.plugin.search.searchScopeForSourceElementDependencies
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

class TypesCompletion(val parameters: CompletionParameters,
                      val resolveSession: ResolveSessionForBodies,
                      val prefixMatcher: PrefixMatcher,
                      val visibilityFilter: (DeclarationDescriptor) -> Boolean) {
    fun addAllTypes(result: LookupElementsCollector) {
        result.addDescriptorElements(KotlinBuiltIns.getInstance().getNonPhysicalClasses().filter { prefixMatcher.prefixMatches(it.getName().asString()) },
                                     suppressAutoInsertion = true)

        val project = parameters.getOriginalFile().getProject()
        val searchScope = searchScopeForSourceElementDependencies(parameters.getOriginalFile()) ?: return
        result.addDescriptorElements(KotlinIndicesHelper(project, resolveSession, searchScope, visibilityFilter).getClassDescriptors { prefixMatcher.prefixMatches(it) },
                                     suppressAutoInsertion = true)

        if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as JetFile)) {
            addAdaptedJavaCompletion(result)
        }
    }

    /**
     * Add java elements with performing conversion to kotlin elements if necessary.
     */
    private fun addAdaptedJavaCompletion(collector: LookupElementsCollector) {
        AllClassesGetter.processJavaClasses(parameters, prefixMatcher, true, { psiClass ->
            if (psiClass!! !is KotlinLightClass) { // Kotlin non-compiled class should have already been added as kotlin element before
                if (JavaResolverPsiUtils.isCompiledKotlinClass(psiClass)) {
                    addLookupElementForCompiledKotlinClass(psiClass, collector)
                }
                else {
                    collector.addElementWithAutoInsertionSuppressed(KotlinLookupElementFactory.createLookupElementForJavaClass(psiClass))
                }
            }
        })
    }

    private fun addLookupElementForCompiledKotlinClass(aClass: PsiClass, collector: LookupElementsCollector) {
        if (JetFromJavaDescriptorHelper.getCompiledClassKind(aClass) != ClassKind.CLASS_OBJECT) {
            val qualifiedName = aClass.getQualifiedName()
            if (qualifiedName != null) {
                val descriptors = ResolveSessionUtils.getClassDescriptorsByFqName(resolveSession.getModuleDescriptor(), FqName(qualifiedName)).filter(visibilityFilter)
                collector.addDescriptorElements(descriptors, suppressAutoInsertion = true)
            }
        }
    }
}
