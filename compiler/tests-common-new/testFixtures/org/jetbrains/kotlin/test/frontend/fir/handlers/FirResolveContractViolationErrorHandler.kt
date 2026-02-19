/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

class FirResolveContractViolationErrorHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val session = part.session
            val lazyResolver = session.lazyDeclarationResolver as? FirCompilerLazyDeclarationResolverWithPhaseChecking ?: return
            val exceptions = lazyResolver.getContractViolationExceptions().ifEmpty { return }
            testServices.assertions.failAll(exceptions)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
