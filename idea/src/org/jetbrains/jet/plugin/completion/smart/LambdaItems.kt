package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.completion.handlers.functionParameterTypes
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.completion.handlers.insertLambdaTemplate
import com.intellij.openapi.util.TextRange
import org.jetbrains.jet.plugin.completion.JetCompletionCharFilter
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.plugin.completion.handlers.buildLambdaPresentation

object LambdaItems {
    public fun addToCollection(collection: MutableCollection<LookupElement>, functionExpectedInfos: Collection<ExpectedInfo>) {
        val distinctTypes = functionExpectedInfos.map { it.`type` }.toSet()

        val singleType = if (distinctTypes.size == 1) distinctTypes.single() else null
        val singleSignatureLength = singleType?.let { KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(it).size }
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = LookupElementBuilder.create("{...}")
                    .withInsertHandler(ArtificialElementInsertHandler("{ ", " }", false))
                    .suppressAutoInsertion()
                    .addTail(functionExpectedInfos)
            lookupElement.putUserData(JetCompletionCharFilter.ACCEPT_OPENING_BRACE, true)
            collection.add(lookupElement)
        }

        if (singleSignatureLength != 0) {
            for (functionType in distinctTypes) {
                val lookupString = buildLambdaPresentation(functionType)
                val lookupElement = LookupElementBuilder.create(lookupString)
                        .withInsertHandler({ (context, lookupElement) ->
                                               val offset = context.getStartOffset()
                                               val placeholder = "{}"
                                               context.getDocument().replaceString(offset, context.getTailOffset(), placeholder)
                                               insertLambdaTemplate(context, TextRange(offset, offset + placeholder.length), functionType)
                                           })
                        .suppressAutoInsertion()
                        .addTail(functionExpectedInfos.filter { it.`type` == functionType })
                lookupElement.putUserData(JetCompletionCharFilter.ACCEPT_OPENING_BRACE, true)
                collection.add(lookupElement)
            }
        }
    }
}
