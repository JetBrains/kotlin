/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

abstract class GeneratedDeclarationKey {
    override fun toString(): String {
        // Stabilize the string so the FIR dump is deterministic regardless of object identity.
        return this::class.simpleName!!
    }
}
