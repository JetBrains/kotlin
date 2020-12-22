/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.Opcodes

// This object should help compiling against different ASM versions in different bunch versions
object VersionIndependentOpcodes {
    const val ACC_RECORD = Opcodes.ACC_RECORD
}
