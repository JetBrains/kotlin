/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.MethodVisitor

internal fun visitAnnotableParameterCount(mv: MethodVisitor, paramCount: Int) {
    mv.visitAnnotableParameterCount(paramCount, true)
    mv.visitAnnotableParameterCount(paramCount, false)
}