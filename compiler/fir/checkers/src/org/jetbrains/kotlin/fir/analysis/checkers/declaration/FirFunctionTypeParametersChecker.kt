/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.toFirLightSourceElement

object FirFunctionTypeParametersChecker : FirSimpleFunctionChecker() {

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.source?.let { source ->
            if (source is FirFakeSourceElement<*>) return

            val typeParamsNode = source.treeStructure.typeParametersList(source.lighterASTNode)
            val nameNode = source.treeStructure.nameIdentifier(source.lighterASTNode)
            if (typeParamsNode != null && nameNode != null && typeParamsNode.startOffset > nameNode.startOffset) {
                reporter.reportOn(
                    typeParamsNode.toFirLightSourceElement(source.treeStructure),
                    FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX,
                    context
                )
            }
        }
    }
}
