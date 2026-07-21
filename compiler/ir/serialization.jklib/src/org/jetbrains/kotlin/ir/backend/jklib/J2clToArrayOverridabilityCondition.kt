/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
class J2clToArrayOverridabilityCondition : IrExternalOverridabilityCondition {
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
