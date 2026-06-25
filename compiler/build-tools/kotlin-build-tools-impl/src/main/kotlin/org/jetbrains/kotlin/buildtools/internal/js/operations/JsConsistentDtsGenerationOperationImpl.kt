/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.js.operations.JsConsistentDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

/**
 * Empty implementation of design option C. The KLIBs and all export-relevant configuration are inherited
 * from [linking]; this operation has no configuration of its own.
 */
internal class JsConsistentDtsGenerationOperationImpl private constructor(
    override val options: Options,
    override val linking: JsLinkingOperation,
    override val outputDirectory: Path,
) : BuildOperationImpl<CompilationResult>(), JsConsistentDtsGenerationOperation, JsConsistentDtsGenerationOperation.Builder,
    DeepCopyable<JsConsistentDtsGenerationOperationImpl> {

    constructor(linking: JsLinkingOperation, outputDirectory: Path) : this(
        options = Options(JsConsistentDtsGenerationOperation::class),
        linking = linking,
        outputDirectory = outputDirectory,
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        // placeholder
        return CompilationResult.COMPILATION_SUCCESS
    }

    override fun toBuilder(): JsConsistentDtsGenerationOperation.Builder = deepCopy()

    override fun build(): JsConsistentDtsGenerationOperation = deepCopy()

    override fun deepCopy(): JsConsistentDtsGenerationOperationImpl =
        JsConsistentDtsGenerationOperationImpl(options.deepCopy(), linking, outputDirectory)
}
