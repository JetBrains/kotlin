/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.overrides

import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
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
        // K1's JavaIncompatibilityRulesOverridabilityCondition also performs an extra check in isPrimitiveCompareTo.
        // It is not needed here as long as we're not using IrFakeOverrideBuilder to build overrides for lazy IR,
        // in particular for built-in classes (however this may change in KT-64352).
        val type = function.valueParameters[index].type
        return type.isPrimitiveType() && !type.hasAnnotation(StandardClassIds.Annotations.FlexibleNullability)
                && !type.hasAnnotation(StandardClassIds.Annotations.EnhancedNullability)
                && !MethodSignatureMapper.shouldBoxSingleValueParameterForSpecialCaseOfRemove(function)
    }
}
