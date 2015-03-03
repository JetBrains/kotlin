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
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.idea.completion.isVisible
import org.jetbrains.kotlin.idea.completion.ExpectedInfo

// adds java static members, enum members and members from default object
class StaticMembers(
        val bindingContext: BindingContext,
        val resolutionFacade: ResolutionFacade,
        val lookupElementFactory: LookupElementFactory
) {
    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: JetSimpleNameExpression,
                               enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val expectedInfosByClass = expectedInfos.groupBy { TypeUtils.getClassDescriptor(it.type) }
        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (classDescriptor != null && !classDescriptor.getName().isSpecial()) {
                addToCollection(collection, classDescriptor, expectedInfosForClass, context, enumEntriesToSkip)
            }
        }
    }

    private fun addToCollection(
            collection: MutableCollection<LookupElement>,
            classDescriptor: ClassDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
            context: JetSimpleNameExpression,
            enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !descriptor.isVisible(scope.getContainingDeclaration(), bindingContext, context)) return

            val classifier: (ExpectedInfo) -> ExpectedInfoClassification
            if (descriptor is CallableDescriptor) {
                val returnType = descriptor.fuzzyReturnType() ?: return
                classifier = { expectedInfo -> returnType.classifyExpectedInfo(expectedInfo) }
            }
            else if (DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor)) {
                classifier = { ExpectedInfoClassification.matches(TypeSubstitutor.EMPTY) } /* we do not need to check type of enum entry because it's taken from proper enum */
            }
            else {
                return
            }

            collection.addLookupElements(descriptor, expectedInfos, classifier) {
                descriptor -> createLookupElement(descriptor, classDescriptor)
            }
        }

        classDescriptor.getStaticScope().getAllDescriptors().forEach(::processMember)

        val defaultObject = classDescriptor.getDefaultObjectDescriptor()
        if (defaultObject != null) {
            defaultObject.getDefaultType().getMemberScope().getAllDescriptors()
                    .filter { !it.isExtension }
                    .forEach(::processMember)
        }

        var members = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
            members = members.filter { DescriptorUtils.isObject(it) }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElement(memberDescriptor: DeclarationDescriptor, classDescriptor: ClassDescriptor): LookupElement {
        val lookupElement = lookupElementFactory.createLookupElement(memberDescriptor, resolutionFacade, bindingContext, false)
        val qualifierPresentation = classDescriptor.getName().asString()
        val qualifierText = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classDescriptor)

        return object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getAllLookupStrings(): Set<String> {
                return setOf(lookupElement.getLookupString(), qualifierPresentation)
            }

            override fun renderElement(presentation: LookupElementPresentation) {
                getDelegate().renderElement(presentation)

                presentation.setItemText(qualifierPresentation + "." + presentation.getItemText())

                val tailText = " (" + DescriptorUtils.getFqName(classDescriptor.getContainingDeclaration()) + ")"
                if (memberDescriptor is FunctionDescriptor) {
                    presentation.appendTailText(tailText, true)
                }
                else {
                    presentation.setTailText(tailText, true)
                }

                if (presentation.getTypeText().isNullOrEmpty()) {
                    presentation.setTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(classDescriptor.getDefaultType()))
                }
            }

            override fun handleInsert(context: InsertionContext) {
                var text = qualifierText + "." + IdeDescriptorRenderers.SOURCE_CODE.renderName(memberDescriptor.getName())

                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
                context.setTailOffset(context.getStartOffset() + text.length)

                if (memberDescriptor is FunctionDescriptor) {
                    getDelegate().handleInsert(context)
                }

                shortenReferences(context, context.getStartOffset(), context.getTailOffset())
            }
        }.assignSmartCompletionPriority(SmartCompletionItemPriority.STATIC_MEMBER)
    }
}
