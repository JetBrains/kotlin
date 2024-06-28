/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.objcinterop

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.MemberWithOriginal

/**
 * Describes method overriding rules for Objective-C methods.
 *
 * The same code, as in this class also exists in [org.jetbrains.kotlin.fir.backend.native.FirNativeOverrideChecker]
 * and in [org.jetbrains.kotlin.ir.objcinterop.ObjCOverridabilityCondition].
 *
 * When modifying, all three copies should be synchronized.
 *
 */
object IrObjCOverridabilityCondition : IrExternalOverridabilityCondition {

    override val contract = IrExternalOverridabilityCondition.Contract.BOTH

    override fun isOverridable(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): IrExternalOverridabilityCondition.Result =
        isOverridableImpl(superMember.member, subMember.member)

    private fun isOverridableImpl(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
    ): IrExternalOverridabilityCondition.Result {
        if (superMember.name == subMember.name) { // Slow path:
            // KT-57640: There's no necessity to implement platform-dependent overridability check for properties
            if (superMember is IrFunction && subMember is IrFunction) {
                superMember.getExternalObjCMethodInfo()?.let { superInfo ->
                    val subInfo = subMember.getExternalObjCMethodInfo()
                    if (subInfo != null) {
                        // Overriding Objective-C method by Objective-C method in interop stubs.
                        // Don't even check method signatures:
                        return if (superInfo.selector == subInfo.selector) {
                            IrExternalOverridabilityCondition.Result.OVERRIDABLE
                        } else {
                            IrExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    } else {
                        // Overriding Objective-C method by Kotlin method.
                        if (!parameterNamesMatch(superMember, subMember)) {
                            return IrExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    }
                }
            }
        }

        return IrExternalOverridabilityCondition.Result.UNKNOWN
    }

    private fun parameterNamesMatch(first: IrFunction, second: IrFunction): Boolean {
        // The original Objective-C method selector is represented as
        // function name and parameter names (except first).

        if (first.valueParameters.size != second.valueParameters.size) {
            return false
        }

        first.valueParameters.forEachIndexed { index, parameter ->
            if (index > 0 && parameter.name != second.valueParameters[index].name) {
                return false
            }
        }

        return true
    }
}
