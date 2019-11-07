/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.resolve.calls.CandidateApplicability
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirUnresolvedReferenceError(val name: Name? = null) : FirDiagnostic() {
    override val reason: String get() = "Unresolved reference" + if (name != null) ": ${name.asString()}" else ""
}

class FirUnresolvedSymbolError(val classId: ClassId) : FirDiagnostic() {
    override val reason: String get() = "Symbol not found for $classId"
}

class FirUnresolvedNameError(val name: Name) : FirDiagnostic() {
    override val reason: String get() = "Unresolved name: $name"
}

class FirInapplicableCandidateError(val applicability: CandidateApplicability, val candidates: Collection<AbstractFirBasedSymbol<*>>) : FirDiagnostic() {
    override val reason: String get() = "Inapplicable($applicability): ${candidates.map { describeSymbol(it) }}"
}

class FirAmbiguityError(val name: Name, val candidates: Collection<AbstractFirBasedSymbol<*>>) : FirDiagnostic() {
    override val reason: String get() = "Ambiguity: $name, ${candidates.map { describeSymbol(it) }}"
}

class FirOperatorAmbiguityError(val candidates: Collection<AbstractFirBasedSymbol<*>>) : FirDiagnostic() {
    override val reason: String get() = "Operator overload ambiguity. Compatible candidates: ${candidates.map { describeSymbol(it) }}"
}

class FirVariableExpectedError : FirDiagnostic() {
    override val reason: String get() = "Variable expected"
}

private fun describeSymbol(symbol: AbstractFirBasedSymbol<*>): String {
    return when (symbol) {
        is FirClassLikeSymbol<*> -> symbol.classId.asString()
        is FirCallableSymbol<*> -> symbol.callableId.toString()
        else -> "$symbol"
    }
}
