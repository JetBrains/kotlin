/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.WeighingContext
import org.jetbrains.jet.plugin.completion.*
import org.jetbrains.jet.lang.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.isValidJavaFqName
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper

public fun CompletionResultSet.addKotlinSorting(parameters: CompletionParameters): CompletionResultSet {
    var sorter = CompletionSorter.defaultSorter(parameters, getPrefixMatcher())!!

    sorter = sorter.weighBefore("stats", PriorityWeigher, KindWeigher)

    sorter = sorter.weighAfter(
            "stats",
            JetDeclarationRemotenessWeigher(parameters.getOriginalFile() as JetFile),
            DeprecatedWeigher)

    sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher)

    return withRelevanceSorter(sorter)
}

private object PriorityWeigher : LookupElementWeigher("kotlin.priority") {
    override fun weigh(element: LookupElement, context: WeighingContext)
            = (element.getUserData(ITEM_PRIORITY_KEY) ?: ItemPriority.DEFAULT).ordinal()
}

private object KindWeigher : LookupElementWeigher("kotlin.kind") {
    enum class Weight : Comparable<Weight> {
        override fun compareTo(other: Weight) = ordinal().compareTo(other.ordinal())

        localOrParameter
        property
        keyword
        default
        packages
    }

    override fun weigh(element: LookupElement): Weight {
        val o = element.getObject()
        return when (o) {
            is DeclarationLookupObject -> when (o.descriptor) {
                is LocalVariableDescriptor, is ValueParameterDescriptor -> Weight.localOrParameter
                is PropertyDescriptor -> Weight.property
                is PackageViewDescriptor -> Weight.packages
                else -> Weight.default
            }

            is KeywordLookupObject -> Weight.keyword

            else -> Weight.default
        }
    }
}

private object DeprecatedWeigher : LookupElementWeigher("kotlin.deprecated") {
    override fun weigh(element: LookupElement): Int {
        val o = element.getObject()
        if (o is DeclarationLookupObject) {
            val descriptor = o.descriptor
            if (descriptor != null && KotlinBuiltIns.getInstance().isDeprecated(descriptor)) return 1
        }

        return 0
    }
}

private object PreferMatchingItemWeigher : LookupElementWeigher("kotlin.preferMatching", false, true) {
    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<Int> {
        val prefix = context.itemPattern(element)
        return if (element.getLookupString() == prefix) 0 else 1
    }
}

private class JetDeclarationRemotenessWeigher(private val file: JetFile) : LookupElementWeigher("kotlin.declarationRemoteness") {
    private enum class Weight : Comparable<Weight> {
        override fun compareTo(other: Weight) = ordinal().compareTo(other.ordinal())

        kotlinDefaultImport
        thisFile
        imported
        normal
        notImported
    }

    override fun weigh(element: LookupElement): Weight {
        val o = element.getObject()
        if (o is DeclarationLookupObject) {
            val elementFile = o.psiElement?.getContainingFile()
            if (elementFile is JetFile && elementFile.getOriginalFile() == file) {
                return Weight.thisFile
            }

            val descriptor = o.descriptor
            if (descriptor != null) {
                val fqName = DescriptorUtils.getFqName(descriptor).toString()
                // Invalid name can be met for class object descriptor: Test.MyTest.A.<no name provided>.testOther
                if (isValidJavaFqName(fqName)) {
                    val importPath = ImportPath(fqName)
                    return when {
                        ImportInsertHelper.needImport(importPath, file) -> Weight.notImported
                        ImportInsertHelper.isImportedWithDefault(importPath, file) -> Weight.kotlinDefaultImport
                        else -> Weight.imported
                    }
                }
            }
        }

        return Weight.normal
    }
}
