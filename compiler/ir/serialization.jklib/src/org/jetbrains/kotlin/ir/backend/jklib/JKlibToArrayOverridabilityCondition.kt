/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Result
import org.jetbrains.kotlin.ir.overrides.MemberWithOriginal
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId

/**
 * Overridability condition to match toArray(Array) methods despite Java/Kotlin signature
 * differences.
 */
class JKlibToArrayOverridabilityCondition : IrExternalOverridabilityCondition {
    override fun isOverridable(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): Result {
        val superFun = superMember.member as? IrSimpleFunction ?: return Result.UNKNOWN
        val subFun = subMember.member as? IrSimpleFunction ?: return Result.UNKNOWN

        if (superFun.name.asString() == "toArray" && subFun.name.asString() == "toArray") {
            val superParams = superFun.parameters.filter { it.kind == IrParameterKind.Regular }
            val subParams = subFun.parameters.filter { it.kind == IrParameterKind.Regular }
            if (superParams.size == 1 && subParams.size == 1) {
                val superParamType = superParams[0].type
                val subParamType = subParams[0].type
                val superFq = superParamType.classOrNull?.owner?.classId?.asSingleFqName()?.asString()
                val subFq = subParamType.classOrNull?.owner?.classId?.asSingleFqName()?.asString()
                if (superFq == "kotlin.Array" && subFq == "kotlin.Array") {
                    return Result.OVERRIDABLE
                }
            }
        }

        return Result.UNKNOWN
    }

    override val contract: Contract
        get() = Contract.SUCCESS_ONLY
}
