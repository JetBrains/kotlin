/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider

abstract class DefaultNonIncrementalCompilationTest : BaseCompilationTest<DefaultNonIncrementalScenarioDsl>() {
    override fun scenario(buildRunnerProvider: BuildRunnerProvider, init: DefaultNonIncrementalScenarioDsl.() -> Unit) {
        maybeSkip(buildRunnerProvider)
        DefaultNonIncrementalScenarioDsl(this, buildRunnerProvider, workingDirectory, project).apply(init)
    }
}