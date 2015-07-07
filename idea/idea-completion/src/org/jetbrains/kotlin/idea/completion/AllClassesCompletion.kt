/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext

class AllClassesCompletion(private val parameters: CompletionParameters,
                           private val kotlinIndicesHelper: KotlinIndicesHelper,
                           private val prefixMatcher: PrefixMatcher,
                           private val kindFilter: (ClassKind) -> Boolean) {
    fun collect(classDescriptorCollector: (ClassDescriptor) -> Unit, javaClassCollector: (PsiClass) -> Unit) {
        //TODO: this is a temporary hack until we have built-ins in indices
        val builtIns = JavaToKotlinClassMap.INSTANCE.allKotlinClasses()
        val filteredBuiltIns = builtIns
                .filter { kindFilter(it.getKind()) && prefixMatcher.prefixMatches(it.getName().asString()) }
        filteredBuiltIns.forEach { classDescriptorCollector(it) }

        kotlinIndicesHelper
                .getKotlinClasses({ prefixMatcher.prefixMatches(it) }, kindFilter)
                .forEach { classDescriptorCollector(it) }

        if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as JetFile)) {
            addAdaptedJavaCompletion(javaClassCollector)
        }
    }

    private fun addAdaptedJavaCompletion(collector: (PsiClass) -> Unit) {
        AllClassesGetter.processJavaClasses(parameters, prefixMatcher, true, { psiClass ->
            if (psiClass!! !is KotlinLightClass) { // Kotlin class should have already been added as kotlin element before
                if (psiClass.isSyntheticKotlinClass()) return@processJavaClasses // filter out synthetic classes produced by Kotlin compiler

                val kind = when {
                    psiClass.isAnnotationType() -> ClassKind.ANNOTATION_CLASS
                    psiClass.isInterface() -> ClassKind.INTERFACE
                    psiClass.isEnum() -> ClassKind.ENUM_CLASS
                    else -> ClassKind.CLASS
                }
                if (kindFilter(kind)) {
                    collector(psiClass)
                }
            }
        })
    }

    private fun PsiClass.isSyntheticKotlinClass(): Boolean {
        if (!getName().contains('$')) return false // optimization to not analyze annotations of all classes
        return getModifierList()?.findAnnotation(javaClass<kotlin.jvm.internal.KotlinSyntheticClass>().getName()) != null
    }
}
