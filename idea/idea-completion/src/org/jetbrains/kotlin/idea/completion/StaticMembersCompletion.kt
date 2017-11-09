/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.collectSyntheticStaticMembersAndConstructors
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import java.util.*

class StaticMembersCompletion(
        private val prefixMatcher: PrefixMatcher,
        private val resolutionFacade: ResolutionFacade,
        private val lookupElementFactory: LookupElementFactory,
        alreadyAdded: Collection<DeclarationDescriptor>,
        private val isJvmModule: Boolean
) {
    private val alreadyAdded = alreadyAdded.mapTo(HashSet()) {
        if (it is ImportedFromObjectCallableDescriptor<*>) it.callableFromObject else it
    }

    fun decoratedLookupElementFactory(itemPriority: ItemPriority): AbstractLookupElementFactory {
        return object : AbstractLookupElementFactory {
            override fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement> {
                if (!useReceiverTypes) return emptyList()
                return lookupElementFactory.createLookupElement(descriptor, useReceiverTypes = false)
                        .decorateAsStaticMember(descriptor, classNameAsLookupString = false)
                        ?.assignPriority(itemPriority)
                        ?.suppressAutoInsertion()
                        .let(::listOfNotNull)
            }

            override fun createLookupElement(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean,
                                             qualifyNestedClasses: Boolean, includeClassTypeArguments: Boolean,
                                             parametersAndTypeGrayed: Boolean) = null
        }
    }

    fun membersFromImports(file: KtFile): Collection<DeclarationDescriptor> {
        val containers = file.importDirectives
                .filter { !it.isAllUnder }
                .mapNotNull {
                    it.targetDescriptors(resolutionFacade)
                            .map { it.containingDeclaration }
                            .distinct()
                            .singleOrNull() as? ClassDescriptor
                }
                .toSet()

        val result = ArrayList<DeclarationDescriptor>()

        val kindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter = prefixMatcher.asNameFilter()
        for (container in containers) {
            val memberScope = if (container.kind == ClassKind.OBJECT) container.unsubstitutedMemberScope else container.staticScope
            val members =
                    memberScope.getDescriptorsFiltered(kindFilter, nameFilter) +
                    memberScope.collectSyntheticStaticMembersAndConstructors(resolutionFacade, kindFilter, nameFilter)
            members.filterTo(result) { it is CallableDescriptor && it !in alreadyAdded }
        }
        return result
    }

    //TODO: filter out those that are accessible from SmartCompletion.additionalItems
    //TODO: what about enum members?
    //TODO: better presentation for lookup elements from imports too
    //TODO: from the same file

    fun processMembersFromIndices(indicesHelper: KotlinIndicesHelper, processor: (DeclarationDescriptor) -> Unit) {
        val descriptorKindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter: (String) -> Boolean = { prefixMatcher.prefixMatches(it) }

        val filter = { declaration: KtNamedDeclaration, objectDeclaration: KtObjectDeclaration ->
            !declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD) && objectDeclaration.isTopLevelOrCompanion()
        }
        indicesHelper.processObjectMembers(descriptorKindFilter, nameFilter, filter) {
            if (it !in alreadyAdded) {
                processor(it)
            }
        }

        if (isJvmModule) {
            indicesHelper.processJavaStaticMembers(descriptorKindFilter, nameFilter){
                if (it !in alreadyAdded) {
                    processor(it)
                }
            }
        }
    }

    private fun KtObjectDeclaration.isTopLevelOrCompanion(): Boolean {
        if (isCompanion()) {
            val owner = parent.parent as? KtClass ?: return false
            return owner.isTopLevel()
        }
        else {
            return isTopLevel()
        }
    }

    fun completeFromImports(file: KtFile, collector: LookupElementsCollector) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER_FROM_IMPORTS)
        membersFromImports(file)
                .flatMap { factory.createStandardLookupElementsForDescriptor(it, useReceiverTypes = true) }
                .forEach { collector.addElement(it) }
    }

    fun completeFromIndices(indicesHelper: KotlinIndicesHelper, collector: LookupElementsCollector) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)
        processMembersFromIndices(indicesHelper) {
            factory.createStandardLookupElementsForDescriptor(it, useReceiverTypes = true).forEach { collector.addElement(it) }
            collector.flushToResultSet()
        }
    }
}
