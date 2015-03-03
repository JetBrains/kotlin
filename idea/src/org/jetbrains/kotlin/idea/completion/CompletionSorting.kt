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

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.WeighingContext
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.ImportPath
import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.completion.smart.NameSimilarityWeigher
import org.jetbrains.kotlin.idea.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import com.intellij.psi.PsiClass
import java.util.HashSet
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor

public fun CompletionResultSet.addKotlinSorting(parameters: CompletionParameters): CompletionResultSet {
    var sorter = CompletionSorter.defaultSorter(parameters, getPrefixMatcher())!!

    sorter = sorter.weighBefore("stats", PriorityWeigher, DeprecatedWeigher, KindWeigher)

    if (parameters.getCompletionType() == CompletionType.SMART) {
        sorter = sorter.weighBefore("kotlin.kind", NameSimilarityWeigher, SmartCompletionPriorityWeigher)
    }

    sorter = sorter.weighAfter("stats", JetDeclarationRemotenessWeigher(parameters.getOriginalFile() as JetFile))

    sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher)

    return withRelevanceSorter(sorter)
}

private object PriorityWeigher : LookupElementWeigher("kotlin.priority") {
    override fun weigh(element: LookupElement, context: WeighingContext)
            = element.getUserData(ITEM_PRIORITY_KEY) ?: ItemPriority.DEFAULT
}

private object SmartCompletionPriorityWeigher : LookupElementWeigher("kotlin.smartCompletionPriority") {
    override fun weigh(element: LookupElement, context: WeighingContext)
            = element.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) ?: SmartCompletionItemPriority.DEFAULT
}

private object KindWeigher : LookupElementWeigher("kotlin.kind") {
    private enum class Weight {
        variable // variable or property
        function
        keyword
        default
        packages
    }

    private data class CompoundWeight(val weight: Weight, val callableWeight: CallableWeight? = null) : Comparable<CompoundWeight> {
        override fun compareTo(other: CompoundWeight): Int {
            if (callableWeight != null && other.callableWeight != null && callableWeight != other.callableWeight) {
                return callableWeight.compareTo(other.callableWeight)
            }
            return weight.compareTo(other.weight)
        }
    }

    override fun weigh(element: LookupElement): CompoundWeight {
        val o = element.getObject()

        return when (o) {
            is DeclarationDescriptorLookupObject -> {
                val descriptor = o.descriptor
                when (descriptor) {
                    is VariableDescriptor -> CompoundWeight(Weight.variable, element.getUserData(CALLABLE_WEIGHT_KEY))
                    is FunctionDescriptor -> CompoundWeight(Weight.function, element.getUserData(CALLABLE_WEIGHT_KEY))
                    is PackageViewDescriptor -> CompoundWeight(Weight.packages)
                    else -> CompoundWeight(Weight.default)
                }
            }

            is KeywordLookupObject -> CompoundWeight(Weight.keyword)

            else -> CompoundWeight(Weight.default)
        }
    }
}

private object DeprecatedWeigher : LookupElementWeigher("kotlin.deprecated") {
    override fun weigh(element: LookupElement): Int {
        val o = element.getObject()
        return if (o is DeclarationDescriptorLookupObject && KotlinBuiltIns.isDeprecated(o.descriptor)) 1 else 0
    }
}

private object PreferMatchingItemWeigher : LookupElementWeigher("kotlin.preferMatching", false, true) {
    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<Int> {
        val prefix = context.itemPattern(element)
        return if (element.getLookupString() == prefix) 0 else 1
    }
}

private class JetDeclarationRemotenessWeigher(private val file: JetFile) : LookupElementWeigher("kotlin.declarationRemoteness") {
    private val importCache = ImportCache()

    private enum class Weight {
        kotlinDefaultImport
        thisFile
        preciseImport
        allUnderImport
        default
        hasImportFromSamePackage
        notImported
        notToBeUsedInKotlin
    }

    override fun weigh(element: LookupElement): Weight {
        val o = element.getObject()
        if (o is DeclarationDescriptorLookupObject) {
            val elementFile = o.psiElement?.getContainingFile()
            if (elementFile is JetFile && elementFile.getOriginalFile() == file) {
                return Weight.thisFile
            }
        }

        val qualifiedName = qualifiedName(o)
        // Invalid name can be met for default object descriptor: Test.MyTest.A.<no name provided>.testOther
        if (qualifiedName != null && isValidJavaFqName(qualifiedName)) {
            val importPath = ImportPath(qualifiedName)
            val fqName = importPath.fqnPart()
            return when {
                JavaToKotlinClassMap.INSTANCE.mapPlatformClass(fqName).isNotEmpty() -> Weight.notToBeUsedInKotlin
                ImportInsertHelper.getInstance(file.getProject()).isImportedWithDefault(importPath, file) -> Weight.kotlinDefaultImport
                importCache.isImportedWithPreciseImport(fqName) -> Weight.preciseImport
                importCache.isImportedWithAllUnderImport(fqName) -> Weight.allUnderImport
                importCache.hasPreciseImportFromPackage(fqName.parent()) -> Weight.hasImportFromSamePackage
                else -> Weight.notImported
            }
        }

        return Weight.default
    }

    private fun qualifiedName(lookupObject: Any): String? {
        return when (lookupObject) {
            is DeclarationDescriptorLookupObject -> DescriptorUtils.getFqName(lookupObject.descriptor).toString()
            is PsiClass -> lookupObject.getQualifiedName()
            else -> null
        }
    }

    private inner class ImportCache {
        private val preciseImports = HashSet<FqName>()
        private val preciseImportPackages = HashSet<FqName>()
        private val allUnderImports = HashSet<FqName>()

        ;{
            for (import in file.getImportDirectives()) {
                val importPath = import.getImportPath() ?: continue
                val fqName = importPath.fqnPart()
                if (importPath.isAllUnder()) {
                    allUnderImports.add(fqName)
                }
                else {
                    preciseImports.add(fqName)
                    preciseImportPackages.add(fqName.parent())
                }
            }
        }

        fun isImportedWithPreciseImport(name: FqName) = name in preciseImports
        fun isImportedWithAllUnderImport(name: FqName) = name.parent() in allUnderImports
        fun hasPreciseImportFromPackage(packageName: FqName) = packageName in preciseImportPackages
    }
}
