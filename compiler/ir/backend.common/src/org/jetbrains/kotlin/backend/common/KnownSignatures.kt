/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IrTypeSignature.Companion.WILDCARD
import org.jetbrains.kotlin.ir.declarations.IrFunction

@Suppress("ClassName")
object KnownSignatures {

    object kotlin : IrPackageSignature(null) {
        object Any : IrClassSignature(this) {
            val toString by Fun()
            val equals by Fun(Any.asNullable)
            val hashCode by Fun()
        }

        object Boolean : IrClassSignature(this)
        object Int : IrClassSignature(this)

        object Array : IrClassSignature(this) {
            val get by Fun(Int)
            val set by Fun(Int, WILDCARD)
        }

        object collections : IrPackageSignature(this) {
            object List : IrClassSignature(this)
        }
    }

    object java : IrPackageSignature(null) {
        object util : IrPackageSignature(this) {
            object List : IrClassSignature(this)
        }
    }
}


private fun test(function: IrFunction) {
    function.matches(KnownSignatures.kotlin.Array.get)
}