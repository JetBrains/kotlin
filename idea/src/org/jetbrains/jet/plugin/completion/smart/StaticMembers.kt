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
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.completion.handlers.CaretPosition
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetExpression

// adds java static members, enum members and members from class object
class StaticMembers(val bindingContext: BindingContext, val resolveSession: ResolveSessionForBodies) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedTypes: Collection<ExpectedTypeInfo>, context: JetExpression) {
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
        if (scope == null) return

        val expectedTypesByClass = expectedTypes.groupBy { TypeUtils.getClassDescriptor(it.`type`) }
        for ((classDescriptor, expectedTypesForClass) in expectedTypesByClass) {
            if (classDescriptor != null && !classDescriptor.getName().isSpecial()) {
                addToCollection(collection, classDescriptor, expectedTypesForClass, scope)
            }
        }
    }

    private fun addToCollection(
            collection: MutableCollection<LookupElement>,
            classDescriptor: ClassDescriptor,
            expectedTypes: Collection<ExpectedTypeInfo>,
            scope: JetScope) {

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !Visibilities.isVisible(descriptor, scope.getContainingDeclaration())) return

            val matchedExpectedTypes = expectedTypes.filter {
                expectedType ->
                  descriptor is CallableDescriptor && descriptor.getReturnType()?.let { it.isSubtypeOf(expectedType.`type`) } ?: false
                    || descriptor is ClassDescriptor && descriptor.getKind() == ClassKind.ENUM_ENTRY
            }
            if (matchedExpectedTypes.isEmpty()) return

            val lookupElement = createLookupElement(descriptor, classDescriptor)
            collection.add(addTailToLookupElement(lookupElement, matchedExpectedTypes))
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

        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
            classDescriptor.getDefaultType().getMemberScope().getAllDescriptors().forEach(::processMember)
        }
    }

    private fun createLookupElement(memberDescriptor: DeclarationDescriptor, classDescriptor: ClassDescriptor): LookupElement {
        val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, memberDescriptor)
        val qualifierPresentation = classDescriptor.getName().asString()
        val lookupString = qualifierPresentation + "." + lookupElement.getLookupString()
        val qualifierText = DescriptorUtils.getFqName(classDescriptor).asString() //TODO: escape keywords

        val caretPosition: CaretPosition?
        if (memberDescriptor is FunctionDescriptor) {
            caretPosition = if (memberDescriptor.getValueParameters().empty) CaretPosition.AFTER_BRACKETS else CaretPosition.IN_BRACKETS
        }
        else {
            caretPosition = null
        }

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
                    presentation.setTypeText(DescriptorRenderer.TEXT.renderType(classDescriptor.getDefaultType()))
                }
            }

            override fun handleInsert(context: InsertionContext) {
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                var text = qualifierText + "." + memberDescriptor.getName().asString() //TODO: escape
                if (memberDescriptor is FunctionDescriptor) {
                    text += "()"
                    //TODO: auto-popup parameter info and other functionality from JetFunctionInsertHandler
                }

                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                val endOffset = startOffset + text.length
                editor.getCaretModel().moveToOffset(if (caretPosition == CaretPosition.IN_BRACKETS) endOffset - 1 else endOffset)

                shortenReferences(context, startOffset, startOffset + qualifierText.length)
            }
        }
    }
}
