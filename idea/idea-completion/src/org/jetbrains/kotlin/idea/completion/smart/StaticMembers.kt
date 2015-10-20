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

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.completion.ExpectedInfo
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.completion.fuzzyType
import org.jetbrains.kotlin.idea.completion.shortenReferences
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils

// adds java static members, enum members and members from companion object
class StaticMembers(
        val bindingContext: BindingContext,
        val lookupElementFactory: LookupElementFactory
) {
    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: KtSimpleNameExpression,
                               enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val expectedInfosByClass = expectedInfos.groupBy {
            expectedInfo -> expectedInfo.fuzzyType?.type?.let { TypeUtils.getClassDescriptor(it) }
        }
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
            context: KtSimpleNameExpression,
            enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !descriptor.isVisible(scope.getContainingDeclaration(), bindingContext, context)) return

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

            collection.addLookupElements(descriptor, expectedInfos, matcher) {
                descriptor -> createLookupElements(descriptor, classDescriptor)
            }
        }

        classDescriptor.getStaticScope().getAllDescriptors().forEach(::processMember)

        val companionObject = classDescriptor.getCompanionObjectDescriptor()
        if (companionObject != null) {
            companionObject.getDefaultType().getMemberScope().getAllDescriptors()
                    .filter { !it.isExtension }
                    .forEach(::processMember)
        }

        var members = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
            members = members.filter { DescriptorUtils.isNonCompanionObject(it) }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElements(memberDescriptor: DeclarationDescriptor, classDescriptor: ClassDescriptor): Collection<LookupElement> {
        val lookupElements = lookupElementFactory.createLookupElementsInSmartCompletion(memberDescriptor, bindingContext, useReceiverTypes = false)
        val qualifierPresentation = classDescriptor.getName().asString()
        val qualifierText = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classDescriptor)

        return lookupElements.map {
            object: LookupElementDecorator<LookupElement>(it) {
                override fun getAllLookupStrings() = setOf(delegate.lookupString, qualifierPresentation)

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
                    val prefix = qualifierText + "."

                    val offset = context.startOffset
                    context.document.insertString(offset, prefix)
                    context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, offset + prefix.length)

                    shortenReferences(context, offset, offset + prefix.length)
                    PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)

                    super.handleInsert(context)
                }
            }.assignSmartCompletionPriority(SmartCompletionItemPriority.STATIC_MEMBER)
        }
    }
}
