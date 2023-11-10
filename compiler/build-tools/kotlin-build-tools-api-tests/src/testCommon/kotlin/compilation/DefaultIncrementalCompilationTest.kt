/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider

abstract class DefaultIncrementalCompilationTest : BaseIncrementalCompilationTest<DefaultIncrementalScenarioDsl>() {
    override fun scenario(buildRunnerProvider: BuildRunnerProvider, init: DefaultIncrementalScenarioDsl.() -> Unit) {
        maybeSkip(buildRunnerProvider)
        DefaultIncrementalScenarioDsl(this, buildRunnerProvider, workingDirectory, project).apply(init)
    }
}