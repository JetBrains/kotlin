/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

object RedundantExplicitTypeChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirProperty) return
        if (!declaration.isLocal) return

        val initializer = declaration.initializer ?: return
        val typeReference = declaration.returnTypeRef

        if (typeReference.source is FirFakeSourceElement<*>) return

        val type = declaration.returnTypeRef.coneType

        if (typeReference is FirTypeAlias) return

        when (initializer) {
            is FirConstExpression<*> -> {
                when (initializer.source?.elementType) {
                    KtNodeTypes.BOOLEAN_CONSTANT -> {
                        if (type.classId != StandardClassIds.Boolean) return
                    }
                    KtNodeTypes.INTEGER_CONSTANT -> {
                        if (initializer.text?.endsWith("L") == true) {
                            if (type.classId != StandardClassIds.Long) return
                        } else {
                            if (type.classId != StandardClassIds.Int) return
                        }
                    }
                    KtNodeTypes.FLOAT_CONSTANT -> {
                        if (initializer.text?.endsWith("f", ignoreCase = true) == true) {
                            if (type.classId != StandardClassIds.Float) return
                        } else {
                            if (type.classId != StandardClassIds.Double) return
                        }
                    }
                    KtNodeTypes.CHARACTER_CONSTANT -> {
                        if (type.classId != StandardClassIds.Char) return
                    }
                    KtNodeTypes.STRING_TEMPLATE -> {
                        if (type.classId != StandardClassIds.String) return
                    }
                    else -> return
                }
            }
            is FirNamedReference -> {
                if (typeReference.text != initializer.name.identifier) return
            }
            is FirFunctionCall -> {
                if (typeReference.text != initializer.calleeReference.name.identifier) return
            }
        }

        reporter.report(declaration.returnTypeRef.source, FirErrors.REDUNDANT_EXPLICIT_TYPE)
    }

    private val FirExpression.text
        get() = this.source.psi?.text

    private val FirTypeRef.text
        get() = this.psi?.text

}