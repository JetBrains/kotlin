/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy.Companion.defaultCallableShortenStrategy
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy.Companion.defaultClassShortenStrategy
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtUserType

/**
 * @property removeThis If set to `true`, reference shortener will detect redundant `this` qualifiers
 * and will collect them to [ShortenCommand.listOfQualifierToShortenInfo].
 * @property removeThisLabels If set to `true`, reference shortener will detect redundant labels on `this` expressions,
 * and will collect them to [ShortenCommand.thisLabelsToShorten]
 */
@KaIdeApi
public data class ShortenOptions(
    public val removeThis: Boolean = false,
    public val removeThisLabels: Boolean = false,
) {
    @KaIdeApi
    public companion object {
        public val DEFAULT: ShortenOptions = ShortenOptions()

        public val ALL_ENABLED: ShortenOptions = ShortenOptions(removeThis = true, removeThisLabels = true)
    }
}

@KaIdeApi
public enum class ShortenStrategy {
    /** Skip shortening references to this symbol. */
    DO_NOT_SHORTEN,

    /**
     * Only shorten references to this symbol if it's possible without adding a new import directive to the file. Otherwise, leave it as
     * it is.
     *
     * Example:
     *   package a.b.c
     *   import foo.bar
     *   fun test() {}
     *   fun runFunctions() {
     *     foo.bar()     // -> bar()
     *     a.b.c.test()  // -> test()
     *   }
     */
    SHORTEN_IF_ALREADY_IMPORTED,

    /**
     * Shorten references to this symbol and import it into the file if importing it is needed for the shortening.
     *
     * Example:
     *   package a.b.c
     *   fun test() {}
     *   fun runFunctions() {
     *     foo.bar()     // -> bar() and add a new import directive `import foo.bar`
     *     a.b.c.test()  // -> test()
     *   }
     */
    SHORTEN_AND_IMPORT,

    /**
     * Shorten references to this symbol and import this symbol and all its sibling symbols with star import on the parent if importing them
     * is needed for the shortening.
     */
    SHORTEN_AND_STAR_IMPORT;

    @KaIdeApi
    public companion object {
        @KaIdeApi
        public val defaultClassShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy = {
            if (it.classId?.isNestedClass == true) {
                SHORTEN_IF_ALREADY_IMPORTED
            } else {
                SHORTEN_AND_IMPORT
            }
        }

        @KaIdeApi
        public val defaultCallableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy = { symbol ->
            when (symbol) {
                is KaEnumEntrySymbol -> DO_NOT_SHORTEN

                is KaConstructorSymbol -> {
                    val isNestedClassConstructor = symbol.containingClassId?.isNestedClass == true

                    if (isNestedClassConstructor) {
                        SHORTEN_IF_ALREADY_IMPORTED
                    } else {
                        SHORTEN_AND_IMPORT
                    }
                }

                else -> {
                    val isNotTopLevel = symbol.callableId?.classId != null

                    if (isNotTopLevel) {
                        SHORTEN_IF_ALREADY_IMPORTED
                    } else {
                        SHORTEN_AND_IMPORT
                    }
                }
            }
        }
    }
}

@KaIdeApi
public interface KaReferenceShortener {
    /**
     * Collects possible references to shorten. By default, it shortens a fully-qualified members to the outermost class and does not
     * shorten enum entries.  In case of KDoc shortens reference only if it is already imported.
     *
     * N.B. This API is not implemented for the FE10 implementation!
     * For a K1- and K2-compatible API, use [org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility].
     *
     * Also see [org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferences] and functions around it.
     */
    @KaIdeApi
    public fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange = file.textRange,
        shortenOptions: ShortenOptions = ShortenOptions.DEFAULT,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy = defaultClassShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy = defaultCallableShortenStrategy
    ): ShortenCommand

    /**
     * Collects possible references to shorten in [element]s text range. By default, it shortens a fully-qualified members to the outermost
     * class and does not shorten enum entries.
     *
     * N.B. This API is not implemented for the FE10 implementation!
     * For a K1- and K2-compatible API, use [org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility].
     *
     * Also see [org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferences] and functions around it.
     */
    @KaIdeApi
    public fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        shortenOptions: ShortenOptions = ShortenOptions.DEFAULT,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy = defaultClassShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy = defaultCallableShortenStrategy
    ): ShortenCommand
}

/**
 * A class to keep a [KtUserType] to shorten and what shape the shortened result has to be. [shortenedReference] is the expected result of
 * shortening in a string form. If [shortenedReference] is null, it means the shortening will simply delete the qualifier. Note that
 * currently the only usage of [shortenedReference] is the case we have the import-alias. For example, [shortenedReference] will be
 * "AliasType" when we shorten:
 * ```
 * import my.package.NewType as AliasType
 * ... my.package.Ne<caret>wType ... // -> we can replace this with `AliasType`.
 * ```
 */
@KaIdeApi
public data class TypeToShortenInfo(val typeToShorten: SmartPsiElementPointer<KtUserType>, val shortenedReference: String?)

/**
 * A class to keep a [KtDotQualifiedExpression] to shorten and what shape the shortened result has to be. [shortenedReference] is the
 * expected result of shortening in a string form. If [shortenedReference] is null, it means the shortening will simply delete the
 * qualifier. Note that currently the only usage of [shortenedReference] is the case we have the import-alias. For example,
 * [shortenedReference] will be "bar" when we shorten:
 * ```
 * import my.package.foo as bar
 * ... my.package.fo<caret>o ... // -> we can replace this with `bar`.
 * ```
 */
@KaIdeApi
public data class QualifierToShortenInfo(
    val qualifierToShorten: SmartPsiElementPointer<KtDotQualifiedExpression>,
    val shortenedReference: String?,
)

/**
 * A class with a reference to [KtThisExpression] with a label qualifier ([KtThisExpression.labelQualifier]) that can be safely removed
 * without changing the semantics of the code.
 */
@KaIdeApi
public data class ThisLabelToShortenInfo(
    val labelToShorten: SmartPsiElementPointer<KtThisExpression>,
)

@KaIdeApi
public interface ShortenCommand {
    public val targetFile: SmartPsiElementPointer<KtFile>
    public val importsToAdd: Set<FqName>
    public val starImportsToAdd: Set<FqName>
    public val listOfTypeToShortenInfo: List<TypeToShortenInfo>
    public val listOfQualifierToShortenInfo: List<QualifierToShortenInfo>
    public val thisLabelsToShorten: List<ThisLabelToShortenInfo>
    public val kDocQualifiersToShorten: List<SmartPsiElementPointer<KDocName>>

    public val isEmpty: Boolean
        get() = listOfTypeToShortenInfo.isEmpty() &&
                listOfQualifierToShortenInfo.isEmpty() &&
                thisLabelsToShorten.isEmpty() &&
                kDocQualifiersToShorten.isEmpty()
}