/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

/**
 * Normally we can RequireRebuild based on input's change,
 * but the decision can also come later, between incremental compilation steps.
 *
 * This exception is used for KT-63837 where common sources need to be rebuilt for consistency.
 */
class RequireRebuildForCorrectnessInKMPException() : Exception()
