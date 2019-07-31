/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirComputingImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl

interface ReturnTypeCalculator {
    fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef
}

class ReturnTypeCalculatorWithJump(val session: FirSession, val scopeSession: ScopeSession) :
    ReturnTypeCalculator {

    private fun cycleErrorType(declaration: FirTypedDeclaration): FirResolvedTypeRef? {
        if (declaration.returnTypeRef is FirComputingImplicitTypeRef) {
            declaration.transformReturnTypeRef(
                TransformImplicitType,
                FirErrorTypeRefImpl(null, "cycle")
            )
            return declaration.returnTypeRef as FirResolvedTypeRef
        }
        return null
    }

    override fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {

        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            // TODO?
            declaration.transformReturnTypeRef(
                TransformImplicitType,
                FirErrorTypeRefImpl(
                    null,
                    "Unsupported: implicit VP type"
                )
            )
        }
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(declaration is FirCallableMemberDeclaration<*>) { "${declaration::class}: ${declaration.render()}" }


        val symbol = declaration.symbol as ConeCallableSymbol
        val id = symbol.callableId

        val provider = session.service<FirProvider>()

        val file = provider.getFirCallableContainerFile(symbol)

        val outerClasses = generateSequence(id.classId) { classId ->
            classId.outerClassId
        }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it) }

        if (file == null || outerClasses.any { it == null }) return FirErrorTypeRefImpl(
            null,
            "I don't know what todo"
        )

        declaration.transformReturnTypeRef(
            TransformImplicitType,
            FirComputingImplicitTypeRef
        )

        val transformer = FirDesignatedBodyResolveTransformer(
            (listOf(file) + outerClasses.filterNotNull().asReversed() + listOf(declaration)).iterator(),
            file.fileSession,
            scopeSession
        )

        file.transform<FirElement, Any?>(transformer, null)


        val newReturnTypeRef = declaration.returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(newReturnTypeRef is FirResolvedTypeRef) { declaration.render() }
        return newReturnTypeRef
    }
}