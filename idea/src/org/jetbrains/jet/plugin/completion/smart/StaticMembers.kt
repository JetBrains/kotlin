/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.plugin.util.makeNotNullable

// adds java static members, enum members and members from class object
class StaticMembers(val bindingContext: BindingContext, val resolveSession: ResolveSessionForBodies) {
    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: JetExpression,
                               enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
        if (scope == null) return

        val expectedInfosByClass = expectedInfos.groupBy { TypeUtils.getClassDescriptor(it.`type`) }
        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (classDescriptor != null && !classDescriptor.getName().isSpecial()) {
                addToCollection(collection, classDescriptor, expectedInfosForClass, scope, enumEntriesToSkip)
            }
        }
    }

    private fun addToCollection(
            collection: MutableCollection<LookupElement>,
            classDescriptor: ClassDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
            scope: JetScope,
            enumEntriesToSkip: Set<DeclarationDescriptor>) {

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !Visibilities.isVisible(descriptor, scope.getContainingDeclaration())) return

            val classifier: (ExpectedInfo) -> ExpectedInfoClassification
            if (descriptor is CallableDescriptor) {
                val returnType = descriptor.getReturnType()
                if (returnType == null) return
                classifier = {
                    expectedInfo ->
                        when {
                            returnType.isSubtypeOf(expectedInfo.`type`) -> ExpectedInfoClassification.MATCHES
                            returnType.isNullable() && returnType.makeNotNullable().isSubtypeOf(expectedInfo.`type`) -> ExpectedInfoClassification.MAKE_NOT_NULLABLE
                            else -> ExpectedInfoClassification.NOT_MATCHES
                        }
                }
            }
            else if (DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor)) {
                classifier = { ExpectedInfoClassification.MATCHES } /* we do not need to check type of enum entry because it's taken from proper enum */
            }
            else if (descriptor is ClassDescriptor && DescriptorUtils.isObject(descriptor)) {
                classifier = { expectedInfo ->
                    if (descriptor.getDefaultType().isSubtypeOf(expectedInfo.`type`)) ExpectedInfoClassification.MATCHES else ExpectedInfoClassification.NOT_MATCHES
                }
            }
            else {
                return
            }

            collection.addLookupElements(expectedInfos, classifier, { createLookupElement(descriptor, classDescriptor) })
        }

        if (classDescriptor is JavaClassDescriptor) {
            val pseudoPackage = classDescriptor.getCorrespondingPackageFragment()
            if (pseudoPackage != null) {
                pseudoPackage.getMemberScope().getAllDescriptors().forEach(::processMember)
            }
        }

        val classObject = classDescriptor.getClassObjectDescriptor()
        if (classObject != null) {
            classObject.getDefaultType().getMemberScope().getAllDescriptors().forEach(::processMember)
        }

        var members = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
            members = members.filter { DescriptorUtils.isObject(it) }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElement(memberDescriptor: DeclarationDescriptor, classDescriptor: ClassDescriptor): LookupElement {
        val lookupElement = createLookupElement(memberDescriptor, resolveSession)
        val qualifierPresentation = classDescriptor.getName().asString()
        val lookupString = qualifierPresentation + "." + lookupElement.getLookupString()
        val qualifierText = DescriptorUtils.getFqName(classDescriptor).asString() //TODO: escape keywords

        return object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

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
                var text = qualifierText + "." + memberDescriptor.getName().asString() //TODO: escape

                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
                context.setTailOffset(context.getStartOffset() + text.length)

                if (memberDescriptor is FunctionDescriptor) {
                    getDelegate().handleInsert(context)
                }

                shortenReferences(context, context.getStartOffset(), context.getTailOffset())
            }
        }
    }
}
