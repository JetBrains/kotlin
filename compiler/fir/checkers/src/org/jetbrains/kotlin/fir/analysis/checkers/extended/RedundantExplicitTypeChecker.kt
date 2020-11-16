/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

object RedundantExplicitTypeChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirProperty) return
        if (!declaration.isLocal) return

        val initializer = declaration.initializer ?: return
        val typeReference = declaration.returnTypeRef

        if (typeReference.source?.kind is FirFakeSourceElementKind) return

        val type = declaration.returnTypeRef.coneType

        if (typeReference is FirTypeAlias) return
        if (typeReference.annotations.isNotEmpty()) return

        when (initializer) {
            is FirConstExpression<*> -> {
                when (initializer.source?.elementType) {
                    KtNodeTypes.BOOLEAN_CONSTANT -> {
                        if (!type.isSame(StandardClassIds.Boolean)) return
                    }
                    KtNodeTypes.INTEGER_CONSTANT -> {
                        if (initializer.kind == FirConstKind.Long) {
                            if (!type.isSame(StandardClassIds.Long)) return
                        } else {
                            if (!type.isSame(StandardClassIds.Int)) return
                        }
                    }
                    KtNodeTypes.FLOAT_CONSTANT -> {
                        if (initializer.kind == FirConstKind.Float) {
                            if (!type.isSame(StandardClassIds.Float)) return
                        } else {
                            if (!type.isSame(StandardClassIds.Double)) return
                        }
                    }
                    KtNodeTypes.CHARACTER_CONSTANT -> {
                        if (!type.isSame(StandardClassIds.Char)) return
                    }
                    KtNodeTypes.STRING_TEMPLATE -> {
                        if (!type.isSame(StandardClassIds.String)) return
                    }
                    else -> return
                }
            }
            is FirNamedReference -> {
                if (typeReference.text != initializer.name.identifier) return
            }
            is FirFunctionCall -> {
                if (typeReference.text != initializer.calleeReference.name.asString()) return
            }
            is FirGetClassCall -> {
                return
            }
            is FirResolvedQualifier -> {
                if (!type.isSame(initializer.classId)) return
            }
            is FirStringConcatenationCall -> {
                if (!type.isSame(StandardClassIds.String)) return
            }
            else -> return
        }

        reporter.report(declaration.returnTypeRef.source, FirErrors.REDUNDANT_EXPLICIT_TYPE)
    }

    private val FirTypeRef.text: String?
        get() {
            return when (source) {
                is FirPsiSourceElement<*> -> {
                    source.psi?.text
                }
                is FirLightSourceElement -> {
                    source?.lighterASTNode?.toString()
                }
                else -> null
            }
        }

    private fun ConeKotlinType.isSame(other: ClassId?): Boolean {
        if (this.nullability.isNullable) return false
        if (this.type.classId == other) return true
        return false
    }

}