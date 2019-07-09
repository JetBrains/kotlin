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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.completion.decorateAsStaticMember
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.PropertyDelegateAdditionalData
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.core.multipleFuzzyTypes
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.*

// adds java static members, enum members and members from companion object
class StaticMembers(
    private val bindingContext: BindingContext,
    private val lookupElementFactory: LookupElementFactory,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor
) {
    fun addToCollection(
        collection: MutableCollection<LookupElement>,
        expectedInfos: Collection<ExpectedInfo>,
        context: KtSimpleNameExpression,
        enumEntriesToSkip: Set<DeclarationDescriptor>
    ) {
        val expectedInfosByClass = HashMap<ClassDescriptor, MutableList<ExpectedInfo>>()
        for (expectedInfo in expectedInfos) {
            for (fuzzyType in expectedInfo.multipleFuzzyTypes) {
                val classDescriptor = fuzzyType.type.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                expectedInfosByClass.getOrPut(classDescriptor) { ArrayList() }.add(expectedInfo)
            }

            if (expectedInfo.additionalData is PropertyDelegateAdditionalData) {
                val delegatesClass =
                    resolutionFacade.resolveImportReference(moduleDescriptor, FqName("kotlin.properties.Delegates")).singleOrNull()
                if (delegatesClass is ClassDescriptor) {
                    addToCollection(
                        collection,
                        delegatesClass,
                        listOf(expectedInfo),
                        context,
                        enumEntriesToSkip,
                        SmartCompletionItemPriority.DELEGATES_STATIC_MEMBER
                    )
                }
            }
        }

        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (!classDescriptor.name.isSpecial) {
                addToCollection(
                    collection,
                    classDescriptor,
                    expectedInfosForClass,
                    context,
                    enumEntriesToSkip,
                    SmartCompletionItemPriority.STATIC_MEMBER
                )
            }
        }
    }

    private fun addToCollection(
        collection: MutableCollection<LookupElement>,
        classDescriptor: ClassDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        context: KtSimpleNameExpression,
        enumEntriesToSkip: Set<DeclarationDescriptor>,
        defaultPriority: SmartCompletionItemPriority
    ) {
        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !descriptor.isVisible(
                    context,
                    null,
                    bindingContext,
                    resolutionFacade
                )
            ) return

            val matcher: (ExpectedInfo) -> ExpectedInfoMatch = when {
                descriptor is CallableDescriptor -> {
                    val returnType = descriptor.fuzzyReturnType() ?: return
                    { expectedInfo -> returnType.matchExpectedInfo(expectedInfo) }
                }
                DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor) -> {
                    /* we do not need to check type of enum entry because it's taken from proper enum */
                    { ExpectedInfoMatch.match(TypeSubstitutor.EMPTY) }
                }
                else -> return
            }

            val priority = when {
                DescriptorUtils.isEnumEntry(descriptor) -> SmartCompletionItemPriority.ENUM_ENTRIES
                else -> defaultPriority
            }

            collection.addLookupElements(descriptor, expectedInfos, matcher) { createLookupElements(it, priority) }
        }

        classDescriptor.staticScope.getContributedDescriptors().forEach(::processMember)

        val companionObject = classDescriptor.companionObjectDescriptor
        if (companionObject != null) {
            companionObject.defaultType.memberScope.getContributedDescriptors()
                .filter { !it.isExtension }
                .forEach(::processMember)
        }

        val members = classDescriptor.defaultType.memberScope.getContributedDescriptors().filter { member ->
            when (classDescriptor.kind) {
                ClassKind.ENUM_CLASS -> member is ClassDescriptor // enum member
                ClassKind.OBJECT -> member is CallableMemberDescriptor || DescriptorUtils.isNonCompanionObject(member)
                else -> DescriptorUtils.isNonCompanionObject(member)
            }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElements(
        memberDescriptor: DeclarationDescriptor,
        priority: SmartCompletionItemPriority
    ): Collection<LookupElement> {
        return lookupElementFactory.createStandardLookupElementsForDescriptor(memberDescriptor, useReceiverTypes = false)
            .map {
                it.decorateAsStaticMember(memberDescriptor, classNameAsLookupString = true)!!
                    .assignSmartCompletionPriority(priority)
            }
    }
}
