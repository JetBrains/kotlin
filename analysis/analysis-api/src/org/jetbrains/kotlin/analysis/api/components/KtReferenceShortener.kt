/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.analysis.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public enum class ShortenOption {
    /** Skip shortening references to this symbol. */
    DO_NOT_SHORTEN,

    /** Only shorten references to this symbol if it's already imported in the file. Otherwise, leave it as it is. */
    SHORTEN_IF_ALREADY_IMPORTED,

    /** Shorten references to this symbol and import it into the file. */
    SHORTEN_AND_IMPORT,

    /** Shorten references to this symbol and import this symbol and all its sibling symbols with star import on the parent. */
    SHORTEN_AND_STAR_IMPORT
}

public abstract class KtReferenceShortener : KtAnalysisSessionComponent() {
    public abstract fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand
}

public interface KtReferenceShortenerMixIn : KtAnalysisSessionMixIn {
    public companion object {
        private val defaultClassShortenOption: (KtClassLikeSymbol) -> ShortenOption = {
            if (it.classIdIfNonLocal?.isNestedClass == true) {
                ShortenOption.SHORTEN_IF_ALREADY_IMPORTED
            } else {
                ShortenOption.SHORTEN_AND_IMPORT
            }
        }

        private val defaultCallableShortenOption: (KtCallableSymbol) -> ShortenOption = { symbol ->
            if (symbol is KtEnumEntrySymbol) ShortenOption.DO_NOT_SHORTEN
            else ShortenOption.SHORTEN_AND_IMPORT
        }

        /**
         * Shorten references in the given [element]. See [shortenReferencesInRange] for more details.
         */
        public fun shortenReferences(
            element: KtElement,
            classShortenOption: (KtClassLikeSymbol) -> ShortenOption = defaultClassShortenOption,
            callableShortenOption: (KtCallableSymbol) -> ShortenOption = defaultCallableShortenOption
        ): Unit = shortenReferencesInRange(
            element.containingKtFile,
            element.textRange,
            classShortenOption,
            callableShortenOption
        )

        /**
         * Shorten references in the given [file] and [range]. The function must be invoked on EDT thread because it modifies the underlying
         * PSI. This method analyse Kotlin code and hence could block the EDT thread for longer period of time. Hence, this method should be
         * called only to shorten references in *newly generated code* by IDE actions. In other cases, please consider using
         * [org.jetbrains.kotlin.analysis.api.components.KtReferenceShortenerMixIn] in a background thread to perform the analysis and then
         * modify PSI on the EDt thread by invoking [org.jetbrains.kotlin.analysis.api.components.ShortenCommand.invokeShortening]. */
        @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
        public fun shortenReferencesInRange(
            file: KtFile,
            range: TextRange = file.textRange,
            classShortenOption: (KtClassLikeSymbol) -> ShortenOption = defaultClassShortenOption,
            callableShortenOption: (KtCallableSymbol) -> ShortenOption = defaultCallableShortenOption
        ) {
            ApplicationManager.getApplication().assertIsDispatchThread()
            val shortenCommand = hackyAllowRunningOnEdt {
                analyse(file) {
                    collectPossibleReferenceShortenings(file, range, classShortenOption, callableShortenOption)
                }
            }
            shortenCommand.invokeShortening()
        }
    }

    /**
     * Collects possible references to shorten. By default, it shortens a fully-qualified members to the outermost class and does not
     * shorten enum entries.
     */
    public fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange = file.textRange,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption = defaultClassShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption = defaultCallableShortenOption
    ): ShortenCommand =
        analysisSession.referenceShortener.collectShortenings(file, selection, classShortenOption, callableShortenOption)

    public fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption = defaultClassShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption = defaultCallableShortenOption
    ): ShortenCommand =
        analysisSession.referenceShortener.collectShortenings(
            element.containingKtFile,
            element.textRange,
            classShortenOption,
            callableShortenOption
        )
}

public interface ShortenCommand {
    public fun invokeShortening()
    public val isEmpty: Boolean
}