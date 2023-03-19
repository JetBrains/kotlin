/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*

object FirContractChecker : FirFunctionChecker() {
    // TODO: The message should vary. Migrate this to [ConeEffectExtractor] when creating fine-grained errors.
    private const val UNEXPECTED_CONSTRUCTION = "unexpected construction in contract description"

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirContractDescriptionOwner) return
        val contractDescription = declaration.contractDescription as? FirResolvedContractDescription ?: return

        checkUnresolvedEffects(contractDescription, context, reporter)
        checkContractNotAllowed(declaration, contractDescription, context, reporter)
    }

    private fun checkUnresolvedEffects(
        contractDescription: FirResolvedContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Any statements that [ConeEffectExtractor] cannot extract effects will be in `unresolvedEffects`.
        for (unresolvedEffect in contractDescription.unresolvedEffects) {
            val statement = unresolvedEffect.statement
            if (statement.source == null || statement.source!!.kind is KtFakeSourceElementKind) continue

            // TODO: report on fine-grained locations, e.g., ... implies unresolved => report on unresolved, not the entire statement.
            //  but, sometimes, it's just reported on `contract`...
            reporter.reportOn(statement.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, UNEXPECTED_CONSTRUCTION, context)
        }
    }

    private fun checkContractNotAllowed(
        declaration: FirFunction,
        contractDescription: FirResolvedContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val source = contractDescription.source
        if (source?.kind !is KtRealSourceElementKind) return

        fun contractNotAllowed(message: String) = reporter.reportOn(source, FirErrors.CONTRACT_NOT_ALLOWED, message, context)

        if (declaration is FirPropertyAccessor || declaration is FirAnonymousFunction) contractNotAllowed("Contracts are only allowed for functions")
        else if (declaration.isAbstract || declaration.isOpen || declaration.isOverride) contractNotAllowed("Contracts are not allowed for open or override functions")
        else if (declaration.isOperator) contractNotAllowed("Contracts are not allowed for operator functions")
        else if (declaration.symbol.callableId.isLocal || declaration.visibility == Visibilities.Local) contractNotAllowed("Contracts are not allowed for local functions")
    }

}
