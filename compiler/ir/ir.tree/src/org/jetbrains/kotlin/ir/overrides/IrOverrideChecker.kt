/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract.CONFLICTS_ONLY
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract.SUCCESS_ONLY
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Result.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextWithAdditionalAxioms
import org.jetbrains.kotlin.ir.types.createIrTypeCheckerState
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.utils.addToStdlib.unreachableBranch

/**
 * @param member declaration that should be checked for overridability.
 * @param original the original, unsubstituted version of the declaration if the declaration is a fake override.
 */
class MemberWithOriginal(val member: IrOverridableMember, original: IrOverridableMember? = null) {
    internal constructor(fakeOverride: IrFakeOverrideBuilder.FakeOverride) : this(fakeOverride.override, fakeOverride.original)

    val original: IrOverridableMember = original ?: member

    override fun toString(): String = member.render()
}

class IrOverrideChecker(
    private val typeSystem: IrTypeSystemContext,
    private val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) {
    fun getBothWaysOverridability(
        overrider: MemberWithOriginal,
        candidate: MemberWithOriginal,
    ): Result {
        val result1 = isOverridableBy(candidate, overrider, checkIsInlineFlag = false).result
        val result2 = isOverridableBy(overrider, candidate, checkIsInlineFlag = false).result

        return if (result1 == result2) result1 else Result.INCOMPATIBLE
    }

    fun isOverridableBy(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
        checkIsInlineFlag: Boolean,
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(superMember.member, subMember.member, checkIsInlineFlag)

        return runExternalOverridabilityConditions(superMember, subMember, basicResult)
    }

    private fun isOverridableByWithoutExternalConditions(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        checkIsInlineFlag: Boolean,
    ): OverrideCompatibilityInfo {
        val (superFunction, subFunction) = when (superMember) {
            is IrSimpleFunction -> {
                if (subMember !is IrSimpleFunction) return incompatible("Member kind mismatch")
                if (superMember.isSuspend != subMember.isSuspend) return incompatible("Incompatible suspendability")
                superMember to subMember
            }
            is IrProperty -> {
                if (subMember !is IrProperty) return incompatible("Member kind mismatch")
                if (superMember.getter == null || subMember.getter == null) return incompatible("Fields are not overridable")
                superMember.getter to subMember.getter
            }
            else -> error("Unexpected type of declaration: ${superMember::class.java}, $superMember")
        }

        if (checkIsInlineFlag) {
            val isInline = when (superMember) {
                is IrSimpleFunction -> superMember.isInline
                is IrProperty -> superMember.getter?.isInline == true || superMember.setter?.isInline == true
                else -> unreachableBranch(superMember)
            }
            if (isInline) return incompatible("Inline declaration can't be overridden")
        }

        if (superMember.name != subMember.name) {
            // Check name after member kind checks. This way FO builder will first check types of overridable members and crash
            // if member types are not supported (ex: IrConstructor).
            return incompatible("Name mismatch")
        }

        val superExtensionReceiver = superFunction?.extensionReceiverParameter
        val subExtensionReceiver = subFunction?.extensionReceiverParameter
        if ((superExtensionReceiver == null) != (subExtensionReceiver == null)) return incompatible("Receiver presence mismatch")

        val superTypeParameters = superFunction?.typeParameters.orEmpty()
        val subTypeParameters = subFunction?.typeParameters.orEmpty()
        if (superTypeParameters.size != subTypeParameters.size) return incompatible("Type parameter number mismatch")

        val superValueParameters = superFunction?.valueParameters.orEmpty()
        val subValueParameters = subFunction?.valueParameters.orEmpty()
        if (superValueParameters.size != subValueParameters.size) return incompatible("Value parameter number mismatch")

        val typeCheckerState = createIrTypeCheckerState(
            IrTypeSystemContextWithAdditionalAxioms(typeSystem, superTypeParameters, subTypeParameters)
        )

        for ((index, superTypeParameter) in superTypeParameters.withIndex()) {
            if (!areTypeParametersEquivalent(superTypeParameter, subTypeParameters[index], typeCheckerState)) {
                return incompatible("Type parameter bounds mismatch")
            }
        }

        if (superExtensionReceiver != null && subExtensionReceiver != null &&
            !AbstractTypeChecker.equalTypes(typeCheckerState, subExtensionReceiver.type, superExtensionReceiver.type)
        ) {
            return incompatible("Extension receiver parameter type mismatch")
        }

        for ((index, superValueParameter) in superValueParameters.withIndex()) {
            if (!AbstractTypeChecker.equalTypes(typeCheckerState, subValueParameters[index].type, superValueParameter.type)) {
                return incompatible("Value parameter type mismatch")
            }
        }

        return success()
    }

    private fun areTypeParametersEquivalent(
        superTypeParameter: IrTypeParameter,
        subTypeParameter: IrTypeParameter,
        typeCheckerState: TypeCheckerState,
    ): Boolean {
        val superBounds = superTypeParameter.superTypes
        val subBounds = subTypeParameter.superTypes.toMutableList()
        if (superBounds.size != subBounds.size) return false
        outer@ for (superBound in superBounds) {
            val it = subBounds.listIterator()
            while (it.hasNext()) {
                val subBound = it.next()
                if (AbstractTypeChecker.equalTypes(typeCheckerState, superBound, subBound)) {
                    it.remove()
                    continue@outer
                }
            }
            return false
        }
        return true
    }


    private fun runExternalOverridabilityConditions(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
        basicResult: OverrideCompatibilityInfo,
    ): OverrideCompatibilityInfo {
        var wasSuccess = basicResult.result == OverrideCompatibilityInfo.Result.OVERRIDABLE

        for (externalCondition in externalOverridabilityConditions) {
            // Do not run CONFLICTS_ONLY while there was no success
            if (externalCondition.contract == CONFLICTS_ONLY) continue
            if (wasSuccess && externalCondition.contract == SUCCESS_ONLY) continue
            val result =
                externalCondition.isOverridable(superMember, subMember)
            when (result) {
                OVERRIDABLE -> wasSuccess = true
                INCOMPATIBLE -> return incompatible("External condition")
                UNKNOWN -> {}
            }
        }

        if (!wasSuccess) return basicResult

        // Search for conflicts from external conditions
        for (externalCondition in externalOverridabilityConditions) {
            // Run all conditions that was not run before (i.e. CONFLICTS_ONLY)
            if (externalCondition.contract != CONFLICTS_ONLY) continue
            val result =
                externalCondition.isOverridable(superMember, subMember)
            when (result) {
                INCOMPATIBLE -> return incompatible("External condition")
                OVERRIDABLE -> error(
                    "Contract violation in ${externalCondition.javaClass} condition. It's not supposed to end with success"
                )
                UNKNOWN -> {}
            }
        }

        return success()
    }
}
