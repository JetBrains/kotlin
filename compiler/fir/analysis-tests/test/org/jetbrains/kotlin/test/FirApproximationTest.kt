/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCompilerLazyDeclarationResolverWithPhaseChecking
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class FirApproximationTest : AbstractFirPsiDiagnosticTest() {
    private val emptyFilePath = "compiler/fir/analysis-tests/testData/dummy/empty.kt"

    @Test
    fun `approximation of intersection type with upper bound`() {
        runWithSession { session ->
            val intersectionType = ConeIntersectionType(
                listOf(
                    ConeIntegerLiteralConstantTypeImpl.create(1, false, { true }, ConeNullability.NOT_NULL),
                    ConeClassLikeTypeImpl(StandardClassIds.CharSequence.toLookupTag(), arrayOf(), false)
                ),
                ConeClassLikeTypeImpl(StandardClassIds.Number.toLookupTag(), emptyArray(), false)
            )

            val approximatedType = session.typeApproximator.approximateToSuperType(
                intersectionType,
                TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation
            ) as ConeIntersectionType

            assertTrue(approximatedType.intersectedTypes.none { it is ConeIntegerLiteralConstantType })
            assertNotNull(approximatedType.upperBoundForApproximation)
        }
    }

    private fun runWithSession(f: (FirSession) -> Unit) {
        testRunner(emptyFilePath, configuration).runTest(emptyFilePath) { configuration ->
            val artifact = configuration.testServices.dependencyProvider
                .let { it.getArtifactSafe(it.getTestModule("main"), FrontendKinds.FIR) }!!
            val session = artifact.partsForDependsOnModules.first().session

            (session.lazyDeclarationResolver as? FirCompilerLazyDeclarationResolverWithPhaseChecking)?.startResolvingPhase(
                FirResolvePhase.BODY_RESOLVE
            )

            f(session)
        }
    }
}