package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.refactoring.JetNameValidator
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.template.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable

class LambdaItems(val project: Project) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, functionExpectedInfos: Collection<ExpectedInfo>) {
        val distinctTypes = functionExpectedInfos.map { it.`type` }.toSet()

        fun createLookupElement(lookupString: String, textBeforeCaret: String, textAfterCaret: String, shortenRefs: Boolean)
                = LookupElementBuilder.create(lookupString)
                .withInsertHandler(ArtificialElementInsertHandler(textBeforeCaret, textAfterCaret, shortenRefs))
                .suppressAutoInsertion()

        val singleType = if (distinctTypes.size == 1) distinctTypes.single() else null
        val singleSignatureLength = singleType?.let { KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(it).size }
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = createLookupElement("{...}", "{ ", " }", shortenRefs = false)
            collection.add(lookupElement.addTail(functionExpectedInfos))
        }

        if (singleSignatureLength != 0) {
            for (functionType in distinctTypes) {
                val parameterTypes = functionParameterTypes(functionType)
                val parametersPresentation = parameterTypes.map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }.makeString(", ")
                val useExplicitTypes = distinctTypes.stream().any { it != functionType && functionParameterTypes(it).size == parameterTypes.size }

                fun wrap(s: String) = if (useParenthesis(useExplicitTypes, parameterTypes)) "($s)" else s
                val lookupString = "{ ${wrap(parametersPresentation)} -> ... }"

                val lookupElement = LookupElementBuilder.create(lookupString)
                        .withInsertHandler(LambdaInsertHandler(functionType, useExplicitTypes))
                        .suppressAutoInsertion()
                collection.add(lookupElement.addTail(functionExpectedInfos.filter { it.`type` == functionType }))
            }
        }
    }


    private fun useParenthesis(useExplicitTypes: Boolean, parameterTypes: List<JetType>) = useExplicitTypes || parameterTypes.size != 1

    private inner class LambdaInsertHandler(val functionType: JetType, val useExplicitTypes: Boolean) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.getDocument()
            val editor = context.getEditor()
            val offset = context.getStartOffset()
            val placeholder = "{}"
            document.replaceString(offset, context.getTailOffset(), placeholder)
            val rangeMarker = document.createRangeMarker(offset, offset + placeholder.length)

            // we start template later to not interfere with insertion of tail type
            val commandProcessor = CommandProcessor.getInstance()
            val commandName = commandProcessor.getCurrentCommandName()
            val commandGroupId = commandProcessor.getCurrentCommandGroupId()
            context.setLaterRunnable {
                commandProcessor.executeCommand(project, {
                    ApplicationManager.getApplication()!!.runWriteAction(Computable<Unit> {
                        try {
                            if (rangeMarker.isValid()) {
                                document.deleteString(rangeMarker.getStartOffset(), rangeMarker.getEndOffset())
                                editor.getCaretModel().moveToOffset(rangeMarker.getStartOffset())
                                TemplateManager.getInstance(project).startTemplate(editor, buildTemplate())
                            }
                        }
                        finally {
                            rangeMarker.dispose()
                        }
                    })
                }, commandName, commandGroupId)
            }
        }

        private fun buildTemplate(): Template {
            val parameterTypes = functionParameterTypes(functionType)

            val nameValidator = JetNameValidator.getEmptyValidator(project) //TODO: check for names in scope

            val useParenthesis = useParenthesis(useExplicitTypes, parameterTypes)

            val manager = TemplateManager.getInstance(project)

            val template = manager.createTemplate("", "")
            template.setToShortenLongNames(true)
            //template.setToReformat(true) //TODO
            template.addTextSegment("{ ")
            if (useParenthesis) {
                template.addTextSegment("(")
            }

            for (i in parameterTypes.indices) {
                val parameterType = parameterTypes[i]
                if (i > 0) {
                    template.addTextSegment(", ")
                }
                template.addVariable(ParameterNameExpression(JetNameSuggester.suggestNames(parameterType, nameValidator, "p")), true)
                if (useExplicitTypes) {
                    template.addTextSegment(": " + DescriptorRenderer.SOURCE_CODE.renderType(parameterType))
                }
            }

            if (useParenthesis) {
                template.addTextSegment(")")
            }
            template.addTextSegment(" -> ")
            template.addEndVariable()
            template.addTextSegment(" }")
            return template
        }
    }

    private fun functionParameterTypes(functionType: JetType): List<JetType>
            = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(functionType).map { it.getType() }

    private class ParameterNameExpression(val nameSuggestions: Array<String>) : Expression() {
        override fun calculateResult(context: ExpressionContext?) = TextResult(nameSuggestions[0])

        override fun calculateQuickResult(context: ExpressionContext?): Result? = null

        override fun calculateLookupItems(context: ExpressionContext?)
                = Array<LookupElement>(nameSuggestions.size, { LookupElementBuilder.create(nameSuggestions[it]) })
    }
}
