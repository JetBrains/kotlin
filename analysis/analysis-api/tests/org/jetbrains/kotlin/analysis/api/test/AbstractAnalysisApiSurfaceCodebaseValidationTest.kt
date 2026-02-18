/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * A base test for checking the Analysis API surface codebase
 */
abstract class AbstractAnalysisApiSurfaceCodebaseValidationTest : AbstractAnalysisApiCodebaseValidationTest() {
    override val sourceDirectories = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf("src/org/jetbrains/kotlin/analysis/api"),
        )
    )

    protected fun KtFile.findSessionComponent(): KtClassOrObject? {
        val declarations = declarations
        val sessionComponent = (declarations.firstOrNull() as? KtClassOrObject)?.takeIf { it.isSessionComponent }

        declarations.asSequence()
            .drop(1)
            .filter { it is KtClassOrObject && it.isSessionComponent }
            .toList()
            .ifNotEmpty {
                error(
                    joinToString(
                        prefix = "Only one session component on the first declaration position is allowed.\n$virtualFilePath violates this rule for:\n",
                        separator = "\n"
                    ) { it.name.toString() }
                )
            }

        return sessionComponent
    }

    /**
     * Adds a new annotation to the given declaration with the given text
     * and returns the resulting file text.
     */
    protected fun fileTextWithNewAnnotation(declaration: KtDeclaration, newAnnotationText: String): String {
        val fileText = declaration.containingKtFile.text
        val elementBeforeModifiers = declaration.annotationEntries.lastOrNull() ?: declaration.docComment
        val positionForNewAnnotation = elementBeforeModifiers
            ?.nextLeaf() // the next leaf is used to find the new line after the last element
            ?.endOffset
            ?: declaration.startOffset

        return buildString {
            append(fileText.take(positionForNewAnnotation))
            appendLine(newAnnotationText)
            append(fileText.drop(positionForNewAnnotation))
        }
    }

    protected fun KtAnnotated.hasAnnotation(annotationName: String): Boolean = annotationEntries.any { annotation ->
        annotation.shortName.toString() == annotationName
    }

    protected fun KtAnnotated.hasDeprecatedAnnotation(): Boolean = hasAnnotation(DEPRECATED_ANNOTATION)

    protected val KtClassOrObject.isSessionComponent: Boolean
        get() = superTypeListEntries.any { it.textMatches(KA_SESSION_COMPONENT) } || name == KA_SESSION_CLASS

    protected companion object {
        @OptIn(KaSessionComponentImplementationDetail::class)
        val KA_SESSION_COMPONENT: String = KaSessionComponent::class.simpleName!!

        val KA_SESSION_CLASS: String = KaSession::class.simpleName!!

        private val DEPRECATED_ANNOTATION: String = Deprecated::class.simpleName!!
    }
}