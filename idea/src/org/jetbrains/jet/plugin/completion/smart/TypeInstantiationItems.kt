package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertHandler
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.plugin.completion.handlers.CaretPosition
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import com.intellij.codeInsight.AutoPopupController

class TypeInstantiationItems(val bindingContext: BindingContext, val resolveSession: ResolveSessionForBodies) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        val expectedInfosGrouped: Map<JetType, List<ExpectedInfo>> = expectedInfos.groupBy { TypeUtils.makeNotNullable(it.`type`) }
        for ((jetType, types) in expectedInfosGrouped) {
            val tail = mergeTails(types.map { it.tail })
            addToCollection(collection, jetType, tail)
        }
    }

    private fun addToCollection(collection: MutableCollection<LookupElement>, jetType: JetType, tail: Tail?) {
        if (KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(jetType)) return // do not show "object: ..." for function types

        val classifier = jetType.getConstructor().getDeclarationDescriptor()
        if (!(classifier is ClassDescriptor)) return
        //TODO: check for constructor's visibility

        var lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, classifier)

        var lookupString = lookupElement.getLookupString()

        val typeArgs = jetType.getArguments()
        var itemText = lookupString + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderTypeArguments(typeArgs)

        val insertHandler: InsertHandler<LookupElement>
        val typeText = DescriptorUtils.getFqName(classifier).toString() + DescriptorRenderer.SOURCE_CODE.renderTypeArguments(typeArgs)
        if (classifier.getModality() == Modality.ABSTRACT) {
            val constructorParenthesis = if (classifier.getKind() != ClassKind.TRAIT) "()" else ""
            itemText += constructorParenthesis
            itemText = "object: " + itemText + "{...}"
            lookupString = "object" //?
            insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                val text = "object: $typeText$constructorParenthesis {}"
                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                editor.getCaretModel().moveToOffset(startOffset + text.length - 1)

                shortenReferences(context, startOffset, startOffset + text.length)

                ImplementMethodsHandler().invoke(context.getProject(), editor, context.getFile(), true)
            }
            lookupElement = lookupElement.suppressAutoInsertion()
        }
        else {
            itemText += "()"
            val constructors: Collection<ConstructorDescriptor> = classifier.getConstructors()
            val caretPosition =
                    if (constructors.size == 0)
                        CaretPosition.AFTER_BRACKETS
                    else if (constructors.size == 1)
                        if (constructors.first().getValueParameters().isEmpty()) CaretPosition.AFTER_BRACKETS else CaretPosition.IN_BRACKETS
                    else
                        CaretPosition.IN_BRACKETS
            insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                val text = typeText + "()"
                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                val endOffset = startOffset + text.length
                editor.getCaretModel().moveToOffset(if (caretPosition == CaretPosition.IN_BRACKETS) endOffset - 1 else endOffset)

                shortenReferences(context, startOffset, endOffset)

                AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(editor, null)
            }
        }

        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

            override fun renderElement(presentation: LookupElementPresentation) {
                getDelegate().renderElement(presentation)
                presentation.setItemText(itemText)
            }

            override fun handleInsert(context: InsertionContext) {
                insertHandler.handleInsert(context, getDelegate())
            }
        }

        collection.add(lookupElement.addTail(tail))
    }
}
