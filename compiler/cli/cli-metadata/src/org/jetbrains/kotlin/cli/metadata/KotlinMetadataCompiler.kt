/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataCliPipeline
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * This class is the entry-point for compiling Kotlin code into a metadata KLib.
 *
 * **Note: `2` in the name stands for Kotlin `TO` metadata compiler.
 * This entry-point used by both K1 and K2.**
 *
 * Please see `/docs/fir/k2_kmp.md` for more info on the K2/FIR implementation.
 */
class KotlinMetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {
    override fun doExecutePhased(
        arguments: K2MetadataCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        val pipeline = MetadataCliPipeline(defaultPerformanceManager)
        return pipeline.execute(arguments, services, basicMessageCollector)
    }

    override val platform: TargetPlatform
        get() = CommonPlatforms.defaultCommonPlatform

    override fun createArguments(): K2MetadataCompilerArguments = K2MetadataCompilerArguments()

    // TODO: update this once a launcher script for K2MetadataCompiler is available
    override fun executableScriptFileName(): String = "kotlinc"

    public override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(KotlinMetadataCompiler(), args)
        }
    }
}
