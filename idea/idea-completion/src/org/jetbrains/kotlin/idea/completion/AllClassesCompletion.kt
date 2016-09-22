/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.PsiLiteral
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isJavaClassNotToBeUsedInKotlin
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class AllClassesCompletion(private val parameters: CompletionParameters,
                           private val kotlinIndicesHelper: KotlinIndicesHelper,
                           private val prefixMatcher: PrefixMatcher,
                           private val resolutionFacade: ResolutionFacade,
                           private val kindFilter: (ClassKind) -> Boolean,
                           private val includeTypeAliases: Boolean,
                           private val includeJavaClassesNotToBeUsed: Boolean
) {
    fun collect(classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit, javaClassCollector: (PsiClass) -> Unit) {

        //TODO: this is a temporary solution until we have built-ins in indices
        // we need only nested classes because top-level built-ins are all added through default imports
        for (builtinPackage in resolutionFacade.moduleDescriptor.builtIns.builtInsPackageFragments) {
            collectClassesFromScope(builtinPackage.getMemberScope()) {
                if (it.containingDeclaration is ClassDescriptor) {
                    classifierDescriptorCollector(it)
                }
            }
        }

        kotlinIndicesHelper
                .getKotlinClasses({ prefixMatcher.prefixMatches(it) }, kindFilter)
                .forEach { classifierDescriptorCollector(it) }

        if (includeTypeAliases) {
            kotlinIndicesHelper
                    .getTopLevelTypeAliases(prefixMatcher.asStringNameFilter())
                    .forEach { classifierDescriptorCollector(it) }
        }

        if (!ProjectStructureUtil.isJsKotlinModule(parameters.originalFile as KtFile)) {
            addAdaptedJavaCompletion(javaClassCollector)
        }
    }

    private fun collectClassesFromScope(scope: MemberScope, collector: (ClassDescriptor) -> Unit) {
        for (descriptor in scope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)) {
            if (descriptor is ClassDescriptor) {
                if (kindFilter(descriptor.kind) && prefixMatcher.prefixMatches(descriptor.name.asString())) {
                    collector(descriptor)
                }

                collectClassesFromScope(descriptor.unsubstitutedInnerClassesScope, collector)
            }
        }
    }

    private fun addAdaptedJavaCompletion(collector: (PsiClass) -> Unit) {
        AllClassesGetter.processJavaClasses(parameters, prefixMatcher, true, { psiClass ->
            if (psiClass!! !is KtLightClass) { // Kotlin class should have already been added as kotlin element before
                if (psiClass.isSyntheticKotlinClass()) return@processJavaClasses // filter out synthetic classes produced by Kotlin compiler

                val kind = when {
                    psiClass.isAnnotationType -> ClassKind.ANNOTATION_CLASS
                    psiClass.isInterface -> ClassKind.INTERFACE
                    psiClass.isEnum -> ClassKind.ENUM_CLASS
                    else -> ClassKind.CLASS
                }
                if (kindFilter(kind) && !isNotToBeUsed(psiClass)) {
                    collector(psiClass)
                }
            }
        })
    }

    private fun PsiClass.isSyntheticKotlinClass(): Boolean {
        if ('$' !in name!!) return false // optimization to not analyze annotations of all classes
        val metadata = modifierList?.findAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())
        return (metadata?.findAttributeValue(JvmAnnotationNames.KIND_FIELD_NAME) as? PsiLiteral)?.value ==
                KotlinClassHeader.Kind.SYNTHETIC_CLASS.id
    }

    private fun isNotToBeUsed(javaClass: PsiClass): Boolean {
        if (includeJavaClassesNotToBeUsed) return false
        val fqName = javaClass.getKotlinFqName()
        return fqName != null && isJavaClassNotToBeUsedInKotlin(fqName)
    }
}
