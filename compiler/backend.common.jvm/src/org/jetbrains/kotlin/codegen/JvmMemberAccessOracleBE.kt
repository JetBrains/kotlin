/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

interface JvmMemberAccessOracleBE {
    fun tryMakeFieldAccessible(declaringClass: Type, name: String): Boolean
    fun tryMakeMethodAccessible(declaringClass: Type, signature: JvmMethodSignature): Boolean
}