/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isDeclarationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript

/**
 * [LLFirResolveTarget] representing all declarations in [target] recursively.
 * All of them are going to be resolved.
 */
internal class LLFirWholeElementResolveTarget(designation: FirDesignation) : LLFirResolveTarget(designation) {
    override fun visitTargetElement(
        element: FirElementWithResolveState,
        visitor: LLFirResolveTargetVisitor,
    ) {
        if (element !is FirFile) {
            visitor.performAction(element)
        }

        when {
            element !is FirDeclaration || !element.isDeclarationContainer -> {}

            element is FirRegularClass -> visitor.withRegularClass(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            element is FirFile -> visitor.withFile(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            element is FirScript -> visitor.withScript(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            else -> errorWithFirSpecificEntries("Unexpected declaration: ${element::class.simpleName}", fir = element)
        }
    }

    override fun toStringAdditionalSuffix(): String = "*"
}
