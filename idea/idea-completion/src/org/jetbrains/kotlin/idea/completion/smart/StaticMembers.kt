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
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.completion.decorateAsStaticMember
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.core.multipleFuzzyTypes
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
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
        private val resolutionFacade: ResolutionFacade
) {
    fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: KtSimpleNameExpression,
                               enumEntriesToSkip: Set<DeclarationDescriptor>) {
        val expectedInfosByClass = HashMap<ClassDescriptor, MutableList<ExpectedInfo>>()
        for (expectedInfo in expectedInfos) {
            for (fuzzyType in expectedInfo.multipleFuzzyTypes) {
                val classDescriptor = fuzzyType.type.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                expectedInfosByClass.getOrPut(classDescriptor) { ArrayList() }.add(expectedInfo)
            }
        }

        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (!classDescriptor.name.isSpecial) {
                addToCollection(collection, classDescriptor, expectedInfosForClass, context, enumEntriesToSkip)
            }
        }
    }

    private fun addToCollection(
            collection: MutableCollection<LookupElement>,
            classDescriptor: ClassDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
            context: KtSimpleNameExpression,
            enumEntriesToSkip: Set<DeclarationDescriptor>) {

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !descriptor.isVisible(context, null, bindingContext, resolutionFacade)) return

            val matcher: (ExpectedInfo) -> ExpectedInfoMatch
            if (descriptor is CallableDescriptor) {
                val returnType = descriptor.fuzzyReturnType() ?: return
                matcher = { expectedInfo -> returnType.classifyExpectedInfo(expectedInfo) }
            }
            else if (DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor)) {
                matcher = { ExpectedInfoMatch.match(TypeSubstitutor.EMPTY) } /* we do not need to check type of enum entry because it's taken from proper enum */
            }
            else {
                return
            }

            collection.addLookupElements(descriptor, expectedInfos, matcher) { createLookupElements(it) }
        }

        classDescriptor.staticScope.getContributedDescriptors().forEach(::processMember)

        val companionObject = classDescriptor.companionObjectDescriptor
        if (companionObject != null) {
            companionObject.defaultType.memberScope.getContributedDescriptors()
                    .filter { !it.isExtension }
                    .forEach(::processMember)
        }

        var members = classDescriptor.defaultType.memberScope.getContributedDescriptors()
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) {
            members = members.filter { DescriptorUtils.isNonCompanionObject(it) }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElements(memberDescriptor: DeclarationDescriptor): Collection<LookupElement> {
        return lookupElementFactory.createStandardLookupElementsForDescriptor(memberDescriptor, useReceiverTypes = false)
                .map {
                    it.decorateAsStaticMember(memberDescriptor, classNameAsLookupString = true)!!
                            .assignSmartCompletionPriority(SmartCompletionItemPriority.STATIC_MEMBER)
                }
    }
}
