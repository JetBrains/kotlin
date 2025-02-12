/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

/**
 * Hold common logic of all JVM incremental runners
 *
 * Subclasses differ significantly based on their sourcesToCompile calculation and compile avoidance approach,
 * but it doesn't make sense to copy all this shared code into them
 * (and it's hard to break away from inheritance-based architecture)
 */
abstract class IncrementalJvmCompilerRunnerBase() {
}
