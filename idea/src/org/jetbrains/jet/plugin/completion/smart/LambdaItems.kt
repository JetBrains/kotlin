package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.refactoring.JetNameValidator
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import com.intellij.openapi.project.Project

class LambdaItems(val project: Project) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, functionExpectedTypes: Collection<ExpectedTypeInfo>) {
        val distinctTypes = functionExpectedTypes.map { it.`type` }.toSet()

        fun createLookupElement(lookupString: String, textBeforeCaret: String, textAfterCaret: String, shortenRefs: Boolean)
                = LookupElementBuilder.create(lookupString)
                .withInsertHandler(ArtificialElementInsertHandler(textBeforeCaret, textAfterCaret, shortenRefs))
                .suppressAutoInsertion()

        val singleType = if (distinctTypes.size == 1) distinctTypes.single() else null
        val singleSignatureLength = singleType?.let { KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(it).size }
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = createLookupElement("{...}", "{ ", " }", shortenRefs = false)
            collection.add(addTailToLookupElement(lookupElement, functionExpectedTypes))
        }

        if (singleSignatureLength != 0) {
            fun functionParameterTypes(functionType: JetType)
                    = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(functionType).map { it.getType() }

            for (functionType in distinctTypes) {
                val parameterTypes = functionParameterTypes(functionType)
                val parametersPresentation = parameterTypes.map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }.makeString(", ")

                val useExplicitTypes = distinctTypes.stream().any { it != functionType && functionParameterTypes(it).size == parameterTypes.size }
                val nameValidator = JetNameValidator.getEmptyValidator(project)

                fun parameterName(parameterType: JetType) = JetNameSuggester.suggestNames(parameterType, nameValidator, "p")[0]

                fun parameterText(parameterType: JetType): String {
                    return if (useExplicitTypes)
                        parameterName(parameterType) + ": " + DescriptorRenderer.SOURCE_CODE.renderType(parameterType)
                    else
                        parameterName(parameterType)
                }

                val parametersText = parameterTypes.map(::parameterText).makeString(", ")

                val useParenthesis = parameterTypes.size != 1
                fun wrap(s: String) = if (useParenthesis) "($s)" else s

                val lookupString = "{ ${wrap(parametersPresentation)} -> ... }"
                val lookupElement = createLookupElement(lookupString, "{ ${wrap(parametersText)} -> ", " }", shortenRefs = true)
                collection.add(addTailToLookupElement(lookupElement, functionExpectedTypes.filter { it.`type` == functionType }))
            }
        }
    }

}