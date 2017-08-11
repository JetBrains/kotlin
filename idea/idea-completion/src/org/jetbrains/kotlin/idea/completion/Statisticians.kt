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

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionStatistician
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.ProximityStatistician
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

class KotlinCompletionStatistician : CompletionStatistician() {
    override fun serialize(element: LookupElement, location: CompletionLocation): StatisticsInfo? {
        val o = (element.`object` as? DeclarationLookupObject) ?: return null

        val context = element.getUserDataDeep(STATISTICS_INFO_CONTEXT_KEY) ?: ""

        if (o.descriptor != null) {
            return KotlinStatisticsInfo.forDescriptor(o.descriptor!!.original, context)
        }
        else {
            val fqName = o.importableFqName ?: return StatisticsInfo.EMPTY
            return StatisticsInfo(context, fqName.asString())
        }
    }
}

class KotlinProximityStatistician : ProximityStatistician() {
    override fun serialize(element: PsiElement, location: ProximityLocation): StatisticsInfo? {
        if (element !is KtDeclaration) return null
        val descriptor = element.resolveToDescriptorIfAny() ?: return null
        return KotlinStatisticsInfo.forDescriptor(descriptor)
    }
}

object KotlinStatisticsInfo {
    private val SIGNATURE_RENDERER = DescriptorRenderer.withOptions {
        withDefinedIn = false
        withoutReturnType = true
        startFromName = true
        receiverAfterName = true
        modifiers = emptySet()
        defaultParameterValueRenderer = null
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    }

    fun forDescriptor(descriptor: DeclarationDescriptor, context: String = ""): StatisticsInfo {
        if (descriptor is ClassDescriptor) {
            return descriptor.importableFqName?.let { StatisticsInfo("", it.asString()) } ?: StatisticsInfo.EMPTY
        }

        val container = descriptor.containingDeclaration
        val containerFqName = when (container) {
                                  is ClassDescriptor -> container.importableFqName?.asString()
                                  is PackageFragmentDescriptor -> container.fqName.asString()
                                  is ModuleDescriptor -> ""
                                  else -> null
                              }  ?: return StatisticsInfo.EMPTY
        val signature = SIGNATURE_RENDERER.render(descriptor)
        return StatisticsInfo(context, "$containerFqName###$signature")
    }
}
