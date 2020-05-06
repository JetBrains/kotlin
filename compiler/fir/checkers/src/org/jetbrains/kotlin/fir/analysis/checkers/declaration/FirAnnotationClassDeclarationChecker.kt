/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.KtNodeTypes.FUN
import org.jetbrains.kotlin.KtNodeTypes.VALUE_PARAMETER
import org.jetbrains.kotlin.lexer.KtTokens.VAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.VAR_KEYWORD
import org.jetbrains.kotlin.psi.KtParameter

object FirAnnotationClassDeclarationChecker : FirDeclarationChecker<FirDeclaration>() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) return
        if (declaration.classKind != ANNOTATION_CLASS) return
        if (declaration.isLocal) reporter.report(declaration.source, FirErrors.LOCAL_ANNOTATION_CLASS_ERROR)
        for (it in declaration.declarations) {
            when {
                it is FirConstructor && it.isPrimary -> {
                    for (parameter in it.valueParameters)
                        when (val parameterSourceElement = parameter.source) {
                            is FirPsiSourceElement<*> -> {
                                val parameterPsiElement = parameterSourceElement.psi as KtParameter
                                if (!parameterPsiElement.hasValOrVar())
                                    reporter.report(parameterSourceElement, FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER)
                                else if (parameterPsiElement.isMutable)
                                    reporter.report(parameterSourceElement, FirErrors.VAR_ANNOTATION_PARAMETER)
                            }
                            is FirLightSourceElement -> {
                                val kidsRef = Ref<Array<LighterASTNode?>>()
                                parameterSourceElement.tree.getChildren(parameterSourceElement.element, kidsRef)
                                if (kidsRef.get().any { it?.tokenType == VAR_KEYWORD })
                                    reporter.report(parameterSourceElement, FirErrors.VAR_ANNOTATION_PARAMETER)
                                else if (kidsRef.get().all { it?.tokenType != VAL_KEYWORD })
                                    reporter.report(parameterSourceElement, FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER)
                            }
                        }
                }
                it is FirRegularClass -> {
                    // DO NOTHING: nested annotation classes are allowed in 1.3+
                }
                it is FirProperty && it.source?.elementType == VALUE_PARAMETER -> {
                    // DO NOTHING to avoid reporting constructor properties
                }
                it is FirSimpleFunction && it.source?.elementType != FUN -> {
                    // DO NOTHING to avoid reporting synthetic functions
                    // TODO: replace with origin check
                }
                else -> {
                    reporter.report(it.source, FirErrors.ANNOTATION_CLASS_MEMBER)
                }
            }
        }

    }

    private inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.report(
        source: T?,
        factory: FirDiagnosticFactory0<T, P>
    ) {
        source?.let { report(factory.on(it)) }
    }
}