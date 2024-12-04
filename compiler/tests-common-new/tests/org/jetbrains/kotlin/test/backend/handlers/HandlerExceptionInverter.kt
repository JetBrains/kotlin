/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.opentest4j.AssertionFailedError

/**
 * A handler that throws if [delegate] does not, and otherwise it suppresses the exception form [delegate].
 * Useful for building handlers that throw "marker" exceptions singnifying some other handlers run as expected.
 */
class HandlerExceptionInverter<A : ResultingArtifact<A>>(
    val delegate: AnalysisHandler<A>,
    val throwException: HandlerExceptionInverter<A>.(TestModule) -> Nothing,
) : AnalysisHandler<A>(delegate.testServices, delegate.failureDisablesNextSteps, delegate.doNotRunIfThereWerePreviousFailures) {
    override val artifactKind get() = delegate.artifactKind

    override fun processModule(module: TestModule, info: A) {
        try {
            delegate.processModule(module, info)
        } catch (e: Exception) {
            // Unsuccessful, no throwing
            return
        } catch (e: AssertionFailedError) {
            // Unsuccessful, no throwing
            return
        }

        throwException(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
