/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*

object FirPropertyTypeParametersChecker : FirPropertyChecker() {

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val boundsByName = declaration.typeParameters.map { it.name to it.bounds }.toMap()
        val usedTypes = HashSet<ConeKotlinType>()
        fun collectAllTypes(type: ConeKotlinType) {
            if (usedTypes.add(type)) {
                type.typeArguments.forEach { it.type?.let(::collectAllTypes) }
                if (type is ConeTypeParameterType) {
                    boundsByName[type.lookupTag.name]?.forEach { collectAllTypes(it.coneType) }
                }
            }
        }
        declaration.receiverTypeRef?.let { collectAllTypes(it.coneType) }

        val usedNames = usedTypes.filterIsInstance<ConeTypeParameterType>().map { it.lookupTag.name }
        declaration.typeParameters.filterNot { usedNames.contains(it.name) }.forEach { danglingParam ->
            reporter.reportOn(danglingParam.source, FirErrors.TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER, context)
        }
    }

}
