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

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.caches.KotlinIndicesHelper
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

class AllClassesCompletion(val parameters: CompletionParameters,
                           val lookupElementFactory: LookupElementFactory,
                           val resolutionFacade: ResolutionFacade,
                           val bindingContext: BindingContext,
                           val moduleDescriptor: ModuleDescriptor,
                           val scope: GlobalSearchScope,
                           val prefixMatcher: PrefixMatcher,
                           val kindFilter: (ClassKind) -> Boolean,
                           val visibilityFilter: (DeclarationDescriptor) -> Boolean) {
    fun collect(result: LookupElementsCollector) {
        //TODO: this is a temporary hack until we have built-ins in indices
        val builtIns = JavaToKotlinClassMap.INSTANCE.allKotlinClasses() + listOf(KotlinBuiltIns.getInstance().getNothing())
        val filteredBuiltIns = builtIns.filter { kindFilter(it.getKind()) && prefixMatcher.prefixMatches(it.getName().asString()) }
        result.addDescriptorElements(filteredBuiltIns, suppressAutoInsertion = true)

        val helper = KotlinIndicesHelper(scope.getProject(), resolutionFacade, bindingContext, scope, moduleDescriptor, visibilityFilter)
        result.addDescriptorElements(helper.getClassDescriptors({ prefixMatcher.prefixMatches(it) }, kindFilter),
                                     suppressAutoInsertion = true)

        if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as JetFile)) {
            addAdaptedJavaCompletion(result)
        }
        else {
            //TODO: this is a temporary solution for Kotlin/Javascript library until we have virtual file system based on serialized descriptors
            addKotlinJavascriptCompletion(result)
        }
    }

    private fun addKotlinJavascriptCompletion(collector: LookupElementsCollector) {
        val classDescriptors = allClassDescriptors.getValue().filter { prefixMatcher.prefixMatches(it.getName().asString()) && kindFilter(it.getKind()) }
        collector.addDescriptorElements(classDescriptors, suppressAutoInsertion = true)
    }

    private val allClassDescriptors = CachedValuesManager.getManager(scope.getProject()).createCachedValue( {
        val provider = (moduleDescriptor as ModuleDescriptorImpl).getPackageFragmentProvider()
        val fragments = DescriptorUtils.getPackagesFqNames(moduleDescriptor).flatMap { provider.getPackageFragments(it) }
        val classDescriptors = fragments.flatMap { it.getMemberScope().getAllDescriptors().filter { it is ClassDescriptor} }.map { it as ClassDescriptor }
        CachedValueProvider.Result(classDescriptors, ProjectRootModificationTracker.getInstance(scope.getProject()))
    }, false)

    private fun addAdaptedJavaCompletion(collector: LookupElementsCollector) {
        AllClassesGetter.processJavaClasses(parameters, prefixMatcher, true, { psiClass ->
            if (psiClass!! !is KotlinLightClass) { // Kotlin class should have already been added as kotlin element before
                val kind = when {
                    psiClass.isAnnotationType() -> ClassKind.ANNOTATION_CLASS
                    psiClass.isInterface() -> ClassKind.TRAIT
                    psiClass.isEnum() -> ClassKind.ENUM_CLASS
                    else -> ClassKind.CLASS
                }
                if (kindFilter(kind)) {
                    collector.addElementWithAutoInsertionSuppressed(lookupElementFactory.createLookupElementForJavaClass(psiClass))
                }
            }
        })
    }
}
