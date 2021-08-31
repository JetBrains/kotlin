/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.NO_CHECK_LAMBDA_INLINING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_INLINE_CHECK_IN
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class BytecodeInliningHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {}

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val classFiles = testServices.moduleStructure.modules.flatMap {
            testServices.dependencyProvider.getArtifact(it, ArtifactKinds.Jvm).classFileFactory.getClassFiles()
        }
        val allDirectives = testServices.moduleStructure.allDirectives
        InlineTestUtil.checkNoCallsToInline(
            classFiles,
            skipParameterCheckingInDirectives = NO_CHECK_LAMBDA_INLINING in allDirectives,
            skippedMethods = allDirectives[SKIP_INLINE_CHECK_IN].toSet()
        )

    }
}
