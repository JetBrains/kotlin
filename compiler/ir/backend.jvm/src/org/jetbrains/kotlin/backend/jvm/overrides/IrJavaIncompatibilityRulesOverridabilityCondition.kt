/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.overrides

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Result
import org.jetbrains.kotlin.ir.overrides.MemberWithOriginal
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.StandardClassIds

class IrJavaIncompatibilityRulesOverridabilityCondition : IrExternalOverridabilityCondition {
    override fun isOverridable(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): Result {
        if (doesJavaOverrideHaveIncompatibleValueParameterKinds(superMember, subMember)) {
            return Result.INCOMPATIBLE
        }

        return Result.UNKNOWN
    }

    override val contract: Contract
        get() = Contract.CONFLICTS_ONLY

    private fun doesJavaOverrideHaveIncompatibleValueParameterKinds(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): Boolean {
        val originalSuperMember = superMember.original as? IrSimpleFunction ?: return false
        val originalSubMember = subMember.original as? IrSimpleFunction ?: return false
        if (!originalSubMember.dispatchReceiverParameter!!.type.getClass()!!.isFromJava()) return false
        require(originalSubMember.valueParameters.size == originalSuperMember.valueParameters.size) {
            "External overridability condition with CONFLICTS_ONLY should not be run with different value parameters size: " +
                    "subMember=${originalSubMember.render()} superMember=${originalSuperMember.render()}"
        }

        return originalSubMember.valueParameters.indices.any { i ->
            isJvmParameterTypePrimitive(originalSuperMember, i) != isJvmParameterTypePrimitive(originalSubMember, i)
        }
    }

    private fun isJvmParameterTypePrimitive(function: IrSimpleFunction, index: Int): Boolean {
        // K1's JavaIncompatibilityRulesOverridabilityCondition also performs some extra checks, which are missing here:
        // 1) isPrimitiveCompareTo. This is not needed in case of IR fake overrides as long as we're not using IrFakeOverrideBuilder
        //    to build overrides for lazy IR, in particular for built-in classes (however this may change in KT-64352).
        // 2) forceSingleValueParameterBoxing. This makes the only parameter of `remove(Int)` in a subclass of `MutableCollection<Int>`
        //    non-primitive. It's unclear what exactly it affects if overrides are built over IR.
        // TODO (KT-65100): investigate whether forceSingleValueParameterBoxing is needed here and test properly.

        val type = function.valueParameters[index].type
        return type.isPrimitiveType() && !type.hasAnnotation(StandardClassIds.Annotations.FlexibleNullability)
                && !type.hasAnnotation(StandardClassIds.Annotations.EnhancedNullability)
    }
}
