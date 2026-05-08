/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.utils

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * Checks that all [FirDeclaration]s reachable from the given [roots] have distinct keys as extracted by [keyExtractor]. If the key
 * extractor returns `null`, the declaration is skipped.
 *
 * This is a general building block for "FIR as Data" (KT-84343) constraints that require distinctness of declaration properties within a
 * set of FIR roots.
 *
 * @param lazyErrorTitle A title for the error message if the check fails. The parameters are the two FIR declarations that have conflicting
 *  keys.
 * @param formatLocation Renders a declaration's location for the error message. Defaults to printing the source as-is. Callers in test
 *  fixtures can pass a richer renderer (e.g., line:column for PSI sources) without forcing such dependencies into core compiler code.
 */
inline fun <K : Any> checkDistinctKeys(
    roots: List<FirElement>,
    crossinline keyExtractor: (FirDeclaration) -> K?,
    crossinline lazyErrorTitle: (FirDeclaration, FirDeclaration) -> String,
    crossinline formatLocation: (FirDeclaration) -> String = { it.source.toString() },
) {
    val declarationByKey = mutableMapOf<K, FirDeclaration>()

    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val key = keyExtractor(declaration) ?: return
            val previousDeclaration = declarationByKey.put(key, declaration)

            // We have to compare `previousDeclaration` and `declaration` with reference equality, because regular equality might defer to
            // the key whose uniqueness we want to check in the first place (e.g. source elements).
            //
            // Key uniqueness doesn't prevent the *exact* same FIR declaration instance from appearing multiple times. It's about different
            // FIR declaration instances sharing equal keys accidentally.
            if (previousDeclaration != null && previousDeclaration !== declaration) {
                throwDuplicateKeysError(
                    headline = lazyErrorTitle(previousDeclaration, declaration),
                    key = key,
                    previousDeclaration = previousDeclaration,
                    previousLocation = formatLocation(previousDeclaration),
                    declaration = declaration,
                    location = formatLocation(declaration),
                )
            }
        }
    }

    roots.forEach { it.accept(visitor) }
}

@PublishedApi
internal fun throwDuplicateKeysError(
    headline: String,
    key: Any,
    previousDeclaration: FirDeclaration,
    previousLocation: String,
    declaration: FirDeclaration,
    location: String,
): Nothing {
    val message = buildString {
        append(headline)
        appendLine(":")

        append("  Key: ")
        appendLine(key)

        append("  First declaration:  ")
        append(previousDeclaration::class.simpleName)
        append(" with ")
        append(previousLocation)
        appendLine()

        append("  Second declaration: ")
        append(declaration::class.simpleName)
        append(" with ")
        append(location)
        appendLine()
    }

    error(message)
}
