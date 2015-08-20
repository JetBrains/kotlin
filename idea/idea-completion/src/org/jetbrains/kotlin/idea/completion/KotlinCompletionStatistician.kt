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
import com.intellij.psi.statistics.StatisticsInfo
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer

public class KotlinCompletionStatistician : CompletionStatistician() {
    override fun serialize(element: LookupElement, location: CompletionLocation): StatisticsInfo? {
        val o = (element.`object` as? DeclarationLookupObject) ?: return null

        if (o.descriptor != null) {
            val container = o.descriptor!!.containingDeclaration
            val containerFqName = when (container) {
                is ClassDescriptor -> container.importableFqName?.asString()
                is PackageFragmentDescriptor -> container.fqName.asString()
                is ModuleDescriptor -> ""
                else -> null
            }  ?: return StatisticsInfo.EMPTY
            val signature = DescriptorRenderer.COMPACT.render(o.descriptor!!) //TODO: more compact presentation
            return StatisticsInfo(containerFqName, signature)
        }
        else {
            val fqName = o.importableFqName ?: return StatisticsInfo.EMPTY
            return StatisticsInfo(fqName.parent().asString(), fqName.shortName().identifier)
        }
    }
}