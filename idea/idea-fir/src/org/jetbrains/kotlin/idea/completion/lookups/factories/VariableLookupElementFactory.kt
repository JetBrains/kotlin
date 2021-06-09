/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.completion.lookups.*
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer.renderVariable
import org.jetbrains.kotlin.idea.completion.lookups.ImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.TailTextProvider.getTailText
import org.jetbrains.kotlin.idea.completion.lookups.TailTextProvider.insertLambdaBraces
import org.jetbrains.kotlin.idea.completion.lookups.addCallableImportIfRequired
import org.jetbrains.kotlin.idea.completion.lookups.shortenReferencesForFirCompletion
import org.jetbrains.kotlin.idea.core.withRootPrefixIfNeeded
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtFunctionalType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.render

internal class VariableLookupElementFactory {
    fun KtAnalysisSession.createLookup(
        symbol: KtVariableLikeSymbol,
        options: CallableInsertionOptions,
    ): LookupElementBuilder {
        val rendered = renderVariable(symbol)
        val builder = when (options.insertionStrategy) {
            CallableInsertionStrategy.AsCall -> {
                val functionalType = symbol.annotatedType.type as KtFunctionalType
                val lookupObject = FunctionCallLookupObject(
                    symbol.name,
                    options,
                    rendered,
                    inputValueArguments = functionalType.parameterTypes.isNotEmpty(),
                    insertEmptyLambda = insertLambdaBraces(functionalType),
                )

                val tailText = functionalType.parameterTypes.joinToString(prefix = "(", postfix = ")") {
                    it.render(CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS)
                }

                val typeText = functionalType.returnType.render(CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS)

                LookupElementBuilder.create(lookupObject, symbol.name.asString())
                    .withTailText(tailText, true)
                    .withTypeText(typeText)
                    .withInsertHandler(FunctionInsertionHandler)
            }
            else -> {
                val lookupObject = VariableLookupObject(symbol.name, options, rendered)
                LookupElementBuilder.create(lookupObject, symbol.name.asString())
                    .withTypeText(symbol.annotatedType.type.render(KtTypeRendererOptions.SHORT_NAMES))
                    .withTailText(getTailText(symbol), true)
                    .markIfSyntheticJavaProperty(symbol)
                    .withInsertHandler(VariableInsertionHandler)
            }
        }

        return withSymbolInfo(symbol, builder)
    }

    private fun LookupElementBuilder.markIfSyntheticJavaProperty(symbol: KtVariableLikeSymbol): LookupElementBuilder = when (symbol) {
        is KtSyntheticJavaPropertySymbol -> {
            val getterName = symbol.javaGetterName.asString()
            val setterName = symbol.javaSetterName?.asString()
            this.withTailText((" (from ${buildSyntheticPropertyTailText(getterName, setterName)})"))
                .withLookupStrings(listOfNotNull(getterName, setterName))
        }
        else -> this
    }

    private fun buildSyntheticPropertyTailText(getterName: String, setterName: String?): String =
        if (setterName != null) "$getterName()/$setterName()" else "$getterName()"
}

/**
 * Simplest lookup object so two lookup elements for the same property will clash.
 */
private data class VariableLookupObject(
    override val shortName: Name,
    override val options: CallableInsertionOptions,
    override val renderedDeclaration: String,
) : KotlinCallableLookupObject()


private object VariableInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as VariableLookupObject

        when (val importStrategy = lookupObject.options.importingStrategy) {
            is ImportStrategy.AddImport -> {
                addCallableImportIfRequired(targetFile, importStrategy.nameToImport)
            }

            is ImportStrategy.InsertFqNameAndShorten -> {
                context.document.replaceString(
                    context.startOffset,
                    context.tailOffset,
                    importStrategy.fqName.withRootPrefixIfNeeded().render()
                )

                context.commitDocument()
                shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
            }

            is ImportStrategy.DoNothing -> {
            }
        }
    }
}