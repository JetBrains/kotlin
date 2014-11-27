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
import org.jetbrains.jet.lang.resolve.java.JavaResolverUtils
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.caches.JetFromJavaDescriptorHelper
import org.jetbrains.jet.plugin.project.ProjectStructureUtil
import org.jetbrains.jet.plugin.caches.KotlinIndicesHelper
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor

class AllClassesCompletion(val parameters: CompletionParameters,
                           val resolutionFacade: ResolutionFacade,
                           val bindingContext: BindingContext,
                           val moduleDescriptor: ModuleDescriptor,
                           val scope: GlobalSearchScope,
                           val prefixMatcher: PrefixMatcher,
                           val kindFilter: (ClassKind) -> Boolean,
                           val visibilityFilter: (DeclarationDescriptor) -> Boolean) {
    fun collect(result: LookupElementsCollector) {
        val builtIns = KotlinBuiltIns.getInstance().getNonPhysicalClasses().filter { kindFilter(it.getKind()) && prefixMatcher.prefixMatches(it.getName().asString()) }
        result.addDescriptorElements(builtIns, suppressAutoInsertion = true)

        val helper = KotlinIndicesHelper(scope.getProject(), resolutionFacade, bindingContext, scope, moduleDescriptor, visibilityFilter)
        result.addDescriptorElements(helper.getClassDescriptors({ prefixMatcher.prefixMatches(it) }, kindFilter),
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
                if (JavaResolverUtils.isCompiledKotlinClass(psiClass)) {
                    addLookupElementForCompiledKotlinClass(psiClass, collector)
                }
                else if (!JavaResolverUtils.isCompiledKotlinPackageClass(psiClass)) {
                    val kind = when {
                        psiClass.isAnnotationType() -> ClassKind.ANNOTATION_CLASS
                        psiClass.isInterface() -> ClassKind.TRAIT
                        psiClass.isEnum() -> ClassKind.ENUM_CLASS
                        else -> ClassKind.CLASS
                    }
                    if (kindFilter(kind)) {
                        collector.addElementWithAutoInsertionSuppressed(LookupElementFactory.DEFAULT.createLookupElementForJavaClass(psiClass))
                    }
                }
            }
        })
    }

    private fun addLookupElementForCompiledKotlinClass(aClass: PsiClass, collector: LookupElementsCollector) {
        if (kindFilter(JetFromJavaDescriptorHelper.getCompiledClassKind(aClass))) {
            val qualifiedName = aClass.getQualifiedName()
            if (qualifiedName != null) {
                val descriptors = ResolveSessionUtils.getClassDescriptorsByFqName(moduleDescriptor, FqName(qualifiedName)).filter(visibilityFilter)
                collector.addDescriptorElements(descriptors, suppressAutoInsertion = true)
            }
        }
    }
}
