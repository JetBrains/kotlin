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
import org.jetbrains.kotlin.idea.completion.lookups.CallableImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.TailTextProvider.getTailText
import org.jetbrains.kotlin.idea.completion.lookups.addCallableImportIfRequired
import org.jetbrains.kotlin.idea.completion.lookups.detectImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.shortenReferencesForFirCompletion
import org.jetbrains.kotlin.idea.core.asFqNameWithRootPrefixIfNeeded
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.render

internal class VariableLookupElementFactory {
    fun KtAnalysisSession.createLookup(
        symbol: KtVariableLikeSymbol,
        importStrategy: CallableImportStrategy = detectImportStrategy(symbol)
    ): LookupElementBuilder {
        val lookupObject = VariableLookupObject(
            symbol.name,
            importStrategy = importStrategy,
            renderedReceiverType = symbol.receiverType?.type?.render(CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS),
        )

        return LookupElementBuilder.create(lookupObject, symbol.name.asString())
            .withTypeText(symbol.annotatedType.type.render(KtTypeRendererOptions.SHORT_NAMES))
            .withTailText(getTailText(symbol), true)
            .markIfSyntheticJavaProperty(symbol)
            .withInsertHandler(VariableInsertionHandler)
            .let { withSymbolInfo(symbol, it) }
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
    val importStrategy: CallableImportStrategy,
    override val renderedReceiverType: String?,
) : KotlinCallableLookupObject()


private object VariableInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as VariableLookupObject

        when (val importStrategy = lookupObject.importStrategy) {
            is CallableImportStrategy.AddImport -> {
                addCallableImportIfRequired(targetFile, importStrategy.nameToImport)
            }

            is CallableImportStrategy.InsertFqNameAndShorten -> {
                context.document.replaceString(
                    context.startOffset,
                    context.tailOffset,
                    importStrategy.callableId.asFqNameWithRootPrefixIfNeeded().render()
                )

                context.commitDocument()
                shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
            }

            is CallableImportStrategy.DoNothing -> {
            }
        }
    }
}