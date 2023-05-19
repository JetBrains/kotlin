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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import java.io.File

abstract class AbstractIncrementalFirJvmCompilerRunnerTest : AbstractIncrementalJvmCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            useK2 = true
            languageVersion = "2.0"
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true) // TODO: investigate cases that need isGradleEnabled - the combination looks fragile
}
