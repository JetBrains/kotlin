/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.NO_CHECK_LAMBDA_INLINING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_INLINE_CHECK_IN
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class BytecodeInliningHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        InlineTestUtil.checkNoCallsToInline(
            info.classFileFactory.getClassFiles(),
            skipParameterCheckingInDirectives = NO_CHECK_LAMBDA_INLINING in module.directives,
            skippedMethods = module.directives[SKIP_INLINE_CHECK_IN].toSet()
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
