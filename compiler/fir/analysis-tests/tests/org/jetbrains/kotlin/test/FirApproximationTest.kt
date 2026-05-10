/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class FirApproximationTest : AbstractFirPsiDiagnosticTest() {
    @Test
    fun `approximation of intersection type with upper bound`() {
        runWithSession { session ->
            val intersectionType = ConeIntersectionType(
                listOf(
                    ConeIntegerLiteralConstantTypeImpl.create(1, false, { true }),
                    ConeClassLikeTypeImpl(StandardClassIds.CharSequence.toLookupTag(), arrayOf(), false)
                ),
                ConeClassLikeTypeImpl(StandardClassIds.Number.toLookupTag(), emptyArray(), false)
            )

            val approximatedType = session.typeApproximator.approximateToSuperType(
                intersectionType,
                TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2
            ) as ConeIntersectionType

            assertTrue(approximatedType.intersectedTypes.none { it is ConeIntegerLiteralConstantType })
            assertNotNull(approximatedType.upperBoundForApproximation)
        }
    }

    private fun runWithSession(f: (FirSession) -> Unit) {
        val disposable = Disposer.newDisposable("FirApproximationTest")
        try {
            val session = createSession(disposable)
            f(session)
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    private fun createSession(
        rootDisposable: Disposable,
    ): FirSession {
        val emptyInput = ArgumentsPipelineArtifact(
            arguments = K2JVMCompilerArguments().apply {
                noStdlib = true
                noReflect = true
                noJdk = true
                allowNoSourceFiles = true
                classpath = KtTestUtil.findMockJdkRtJar().absolutePath
            },
            services = Services.EMPTY,
            rootDisposable,
            GroupingMessageCollector(MessageCollector.NONE, false, false),
            PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "stub for approximation test"),
        )
        val configurationOutput = JvmConfigurationPipelinePhase.executePhase(emptyInput)
        val frontendOutput = JvmFrontendPipelinePhase.executePhase(configurationOutput)!!
        return frontendOutput.frontendOutput.outputs.first().session
    }
}
