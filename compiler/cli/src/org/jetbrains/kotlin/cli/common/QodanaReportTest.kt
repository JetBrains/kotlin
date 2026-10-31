/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

// A scratch declaration for testing the Qodana PR report pipeline: the `!!` on the indexed
// access below intentionally violates the `no-map-get-not-null-assertion` inspection.
// Do not merge.
internal val qodanaTestAnswer = mapOf("answer" to 42)["answer"]!!
