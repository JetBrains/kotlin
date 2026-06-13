/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.DEPRECATED
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.KA_SESSION
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.KA_SESSION_COMPONENT
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
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

    protected fun KtAnnotated.hasDeprecatedAnnotation(): Boolean = hasAnnotation(DEPRECATED)

    protected val KtClassOrObject.isSessionComponent: Boolean
        get() = superTypeListEntries.any { it.textMatches(KA_SESSION_COMPONENT) } || name == KA_SESSION

}
