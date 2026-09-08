/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind

class FirCompilerRequiredParameterDescription(
    val name: Name,
    val kind: FirCraParameterKind,
    val position: Int?,
) {
    init {
        require((position != null) xor (kind is FirCraParameterKind.EnumParameter && kind.isVararg))
    }
}

sealed class FirCraParameterKind {
    // We could allow `vararg` literal parameters from the beginning, but
    //  - There would be no way to test the code dealing with them (so far there are no such parameters)
    //  - Checker for non-literal arguments passed to such parameters becomes more complicated:
    //  `varargParam = arrayOf("a", "b")` must be allowed while `"a" + "b"` must be not
    //  although both `arrayOf` and `+` are just function calls
    data class LiteralParameter(val constKind: ConstantValueKind) : FirCraParameterKind()
    data class EnumParameter(val enumClassId: ClassId, val isVararg: Boolean) : FirCraParameterKind()
}
