/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.components.ShortenOption.Companion.defaultCallableShortenOption
import org.jetbrains.kotlin.analysis.api.components.ShortenOption.Companion.defaultClassShortenOption
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtUserType

public enum class ShortenOption {
    /** Skip shortening references to this symbol. */
    DO_NOT_SHORTEN,

    /** Only shorten references to this symbol if it's already imported in the file. Otherwise, leave it as it is. */
    SHORTEN_IF_ALREADY_IMPORTED,

    /** Shorten references to this symbol and import it into the file. */
    SHORTEN_AND_IMPORT,

    /** Shorten references to this symbol and import this symbol and all its sibling symbols with star import on the parent. */
    SHORTEN_AND_STAR_IMPORT;

    public companion object {
        public val defaultClassShortenOption: (KtClassLikeSymbol) -> ShortenOption = {
            if (it.classIdIfNonLocal?.isNestedClass == true) {
                SHORTEN_IF_ALREADY_IMPORTED
            } else {
                SHORTEN_AND_IMPORT
            }
        }

        public val defaultCallableShortenOption: (KtCallableSymbol) -> ShortenOption = { symbol ->
            if (symbol is KtEnumEntrySymbol) DO_NOT_SHORTEN
            else SHORTEN_AND_IMPORT
        }
    }
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

/**
 * These classes represent information about the elements to shorten
 */

public data class ShorteningImportInfo(
    val importToAdd: FqName,
    val importIsStar: Boolean
)

public sealed class KtElementShortening {
    public abstract val element: SmartPsiElementPointer<*>
    public abstract val importInfo: ShorteningImportInfo?
    public abstract val childElement: KtElementShortening?

    public val psi: KtElement?
        get() = element.element as? KtElement
}

public class KtDotQualifierShortening(
    override val element: SmartPsiElementPointer<KtDotQualifiedExpression>,
    override val importInfo: ShorteningImportInfo?,
    override val childElement: KtDotQualifierShortening?
) : KtElementShortening()

public class KtUserTypeShortening(
    override val element: SmartPsiElementPointer<KtUserType>,
    override val importInfo: ShorteningImportInfo?,
    override val childElement: KtUserTypeShortening?
) : KtElementShortening()

/**
 * This is a class representing results of KtReferenceShortener work
 *
 * @property targetFile The file in which the qualifiers to shorten is located
 * @property ktDotQualifierShortenings shortenings of KtDotQualifiedExpression elements in the file
 * @property ktUserTypeShortenings shortenings of KtUserType elements in the file
 * @property isEmpty The flag responsible for detection if any shortening is applicable
 */

public interface ShortenCommand {

    public val targetFile: KtFile?
    public val ktDotQualifierShortenings: List<KtDotQualifierShortening>
    public val ktUserTypeShortenings: List<KtUserTypeShortening>
    public val isEmpty: Boolean

    /**
     * Collects all distinct imports from possible shortenings to launch reference shortening correctly without duplicating them
     */
    public fun getAllDistinctImports(considerInnerElements: Boolean = false): List<ShorteningImportInfo> =
        buildSet {
            ktDotQualifierShortenings.mapNotNullTo(this) { it.importInfo }
            ktUserTypeShortenings.mapNotNullTo(this) { it.importInfo }
        }.toList()

    /**
     * Launches reference shortening using the information stored in properties of this class
     */

    // TODO: should implementation be logically moved here? FE10 logic should be the same when it comes.
    public fun invokeShortening() {}
}