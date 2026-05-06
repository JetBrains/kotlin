/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import kotlin.collections.forEach

/**
 * Checks that all FIR declarations in the given [files] have distinct [KtSourceElement]s. This is a requirement of FIR as Data (KT-84343)
 * and checked in various tests.
 *
 * @param lazyErrorHeadline A headline for the error message if the check fails. The parameters are the two FIR declarations that have
 *  conflicting source elements.
 */
fun checkDistinctSourceElements(files: List<FirFile>, lazyErrorHeadline: (FirDeclaration, FirDeclaration) -> String) {
    val declarationBySourceElement = hashMapOf<KtSourceElement, FirDeclaration>()

    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val sourceElement = declaration.symbol.source ?: return
            val previousDeclaration = declarationBySourceElement.put(sourceElement, declaration)

            // We have to compare `previousDeclaration` and `declaration` with reference equality, because regular equality might defer to
            // the source element whose uniqueness we want to check in the first place.
            //
            // Source element uniqueness doesn't prevent the *exact* same FIR declaration instance from appearing multiple times. It's about
            // different FIR declaration instances sharing equal source elements accidentally.
            if (previousDeclaration != null && previousDeclaration !== declaration) {
                throwDuplicateSourceElementsError(lazyErrorHeadline, previousDeclaration, declaration)
            }
        }
    }

    files.forEach { it.accept(visitor) }
}

private fun throwDuplicateSourceElementsError(
    lazyErrorHeadline: (FirDeclaration, FirDeclaration) -> String,
    previousDeclaration: FirDeclaration,
    declaration: FirDeclaration,
): Nothing {
    val message = buildString {
        append(lazyErrorHeadline(previousDeclaration, declaration))
        appendLine(":")

        append("  First declaration:  ")
        append(previousDeclaration::class.simpleName)
        append(" with ")
        append(previousDeclaration.source.toLocationDescription())
        appendLine()

        append("  Second declaration: ")
        append(declaration::class.simpleName)
        append(" with ")
        append(declaration.source.toLocationDescription())
        appendLine()
    }

    error(message)
}

private fun KtSourceElement?.toLocationDescription(): String =
    when (this) {
        null -> "<unknown location: no source element>"

        is KtPsiSourceElement -> {
            val pos = StringUtil.offsetToLineColumn(psi.containingFile.text, startOffset)
            val lineColumn = "${pos.line + 1}:${pos.column + 1}"

            buildString {
                append(this@toLocationDescription)
                append(" on line ")
                append(lineColumn)
            }
        }

        else -> toString()
    }
