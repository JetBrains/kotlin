/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext

/**
 * Adapter interface that can be implemented to enabled to access
 * IR facilities during phase pre- and postprocessing.
 */
interface BackendContextHolder {
    val heldBackendContext: CommonBackendContext
}
