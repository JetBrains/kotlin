/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState

/**
 * @param member declaration that should be checked for overridability.
 * @param original the original, unsubstituted version of the declaration if the declaration is a fake override.
 */
class MemberWithOriginal(val member: IrOverridableMember, original: IrOverridableMember? = null) {
    internal constructor(fakeOverride: IrFakeOverrideBuilder.FakeOverride) : this(fakeOverride.override, fakeOverride.original)

    val original: IrOverridableMember = original ?: member

    override fun toString(): String = member.render()
}

@JvmInline
value class OverridabilityResult(val overridable: Boolean)

private inline val success get() = OverridabilityResult(true)
private inline val incompatible get() = OverridabilityResult(false)

class IrOverrideChecker(
    private val typeSystem: IrTypeSystemContext,
    private val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) {
    fun getBothWaysOverridability(
        overrider: MemberWithOriginal,
        candidate: MemberWithOriginal,
    ): OverridabilityResult {
        val result1 = isOverridableBy(candidate, overrider, checkIsInlineFlag = false)
        val result2 = isOverridableBy(overrider, candidate, checkIsInlineFlag = false)
        return if (result1 == result2) result1 else incompatible
    }

    fun isOverridableBy(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
        checkIsInlineFlag: Boolean,
    ): OverridabilityResult {
        val basicResult = isOverridableByWithoutExternalConditions(superMember.member, subMember.member, checkIsInlineFlag)

        return runExternalOverridabilityConditions(superMember, subMember, basicResult)
    }

    private fun isOverridableByWithoutExternalConditions(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        checkIsInlineFlag: Boolean,
    ): OverridabilityResult {
        val [superFunction, subFunction] = when (superMember) {
            is IrSimpleFunction -> {
                if (subMember !is IrSimpleFunction) return incompatible
                if (superMember.isSuspend != subMember.isSuspend) return incompatible
                superMember to subMember
            }
            is IrProperty -> {
                if (subMember !is IrProperty) return incompatible
                if (superMember.getter == null || subMember.getter == null) return incompatible
                superMember.getter to subMember.getter
            }
        }

        if (checkIsInlineFlag) {
            val isInline = when (superMember) {
                is IrSimpleFunction -> superMember.isInline
                is IrProperty -> superMember.getter?.isInline == true || superMember.setter?.isInline == true
            }
            if (isInline) return incompatible
        }

        if (superMember.name != subMember.name) {
            // Check name after member kind checks. This way FO builder will first check types of overridable members and crash
            // if member types are not supported (ex: IrConstructor).
            return incompatible
        }

        val superTypeParameters = superFunction?.typeParameters.orEmpty()
        val subTypeParameters = subFunction?.typeParameters.orEmpty()
        if (superTypeParameters.size != subTypeParameters.size) return incompatible

        val superValueParameters = superFunction?.nonDispatchParameters.orEmpty()
        val subValueParameters = subFunction?.nonDispatchParameters.orEmpty()
        if (superValueParameters.size != subValueParameters.size) return incompatible

        if (superValueParameters.map { it.kind } != subValueParameters.map { it.kind }) {
            return incompatible
        }

        val typeCheckerState = createIrTypeCheckerState(
            IrTypeSystemContextWithAdditionalAxioms(typeSystem, superTypeParameters, subTypeParameters)
        )

        for ([index, superTypeParameter] in superTypeParameters.withIndex()) {
            if (!areTypeParametersEquivalent(superTypeParameter, subTypeParameters[index], typeCheckerState)) {
                return incompatible
            }
        }

        for ([index, superValueParameter] in superValueParameters.withIndex()) {
            if (!AbstractTypeChecker.equalTypes(typeCheckerState, subValueParameters[index].type, superValueParameter.type)) {
                return incompatible
            }
        }

        return success
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
        basicResult: OverridabilityResult,
    ): OverridabilityResult {
        var wasSuccess = basicResult.overridable

        for (externalCondition in externalOverridabilityConditions) {
            // Do not run CONFLICTS_ONLY while there was no success
            if (externalCondition.contract == CONFLICTS_ONLY) continue
            if (wasSuccess && externalCondition.contract == SUCCESS_ONLY) continue
            val result =
                externalCondition.isOverridable(superMember, subMember)
            when (result) {
                OVERRIDABLE -> wasSuccess = true
                INCOMPATIBLE -> return incompatible
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
                INCOMPATIBLE -> return incompatible
                OVERRIDABLE -> error(
                    "Contract violation in ${externalCondition.javaClass} condition. It's not supposed to end with success"
                )
                UNKNOWN -> {}
            }
        }

        return success
    }
}
