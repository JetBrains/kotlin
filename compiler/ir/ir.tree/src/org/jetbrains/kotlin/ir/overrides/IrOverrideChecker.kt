/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract.CONFLICTS_ONLY
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Contract.SUCCESS_ONLY
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition.Result.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextWithAdditionalAxioms
import org.jetbrains.kotlin.ir.types.createIrTypeCheckerState
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

class IrOverrideChecker(
    private val typeSystem: IrTypeSystemContext,
    private val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) {
    fun getBothWaysOverridability(
        overriderDescriptor: IrOverridableMember,
        candidateDescriptor: IrOverridableMember,
    ): Result {
        val result1 = isOverridableBy(
            candidateDescriptor,
            overriderDescriptor,
            checkIsInlineFlag = false,
        ).result

        val result2 = isOverridableBy(
            overriderDescriptor,
            candidateDescriptor,
            checkIsInlineFlag = false,
        ).result

        return if (result1 == result2) result1 else Result.INCOMPATIBLE
    }

    fun isOverridableBy(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        checkIsInlineFlag: Boolean,
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(superMember, subMember, checkIsInlineFlag)

        return runExternalOverridabilityConditions(superMember, subMember, basicResult)
    }

    private fun isOverridableByWithoutExternalConditions(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        checkIsInlineFlag: Boolean,
    ): OverrideCompatibilityInfo {
        val superTypeParameters: List<IrTypeParameter>
        val subTypeParameters: List<IrTypeParameter>

        val superValueParameters: List<IrValueParameter>
        val subValueParameters: List<IrValueParameter>

        when (superMember) {
            is IrSimpleFunction -> when {
                subMember !is IrSimpleFunction -> return incompatible("Member kind mismatch")
                superMember.hasExtensionReceiver != subMember.hasExtensionReceiver -> return incompatible("Receiver presence mismatch")
                superMember.isSuspend != subMember.isSuspend -> return incompatible("Incompatible suspendability")
                checkIsInlineFlag && superMember.isInline -> return incompatible("Inline function can't be overridden")

                else -> {
                    superTypeParameters = superMember.typeParameters
                    subTypeParameters = subMember.typeParameters
                    superValueParameters = superMember.compiledValueParameters
                    subValueParameters = subMember.compiledValueParameters
                }
            }
            is IrProperty -> when {
                subMember !is IrProperty -> return incompatible("Member kind mismatch")
                superMember.getter.hasExtensionReceiver != subMember.getter.hasExtensionReceiver -> return incompatible("Receiver presence mismatch")
                checkIsInlineFlag && superMember.isInline -> return incompatible("Inline property can't be overridden")

                else -> {
                    superTypeParameters = superMember.typeParameters
                    subTypeParameters = subMember.typeParameters
                    superValueParameters = superMember.compiledValueParameters
                    subValueParameters = subMember.compiledValueParameters
                }
            }
            else -> error("Unexpected type of declaration: ${superMember::class.java}, $superMember")
        }

        when {
            superMember.name != subMember.name -> {
                // Check name after member kind checks. This way FO builder will first check types of overridable members and crash
                // if member types are not supported (ex: IrConstructor).
                return incompatible("Name mismatch")
            }

            superTypeParameters.size != subTypeParameters.size -> return incompatible("Type parameter number mismatch")
            superValueParameters.size != subValueParameters.size -> return incompatible("Value parameter number mismatch")
        }

        // TODO: check the bounds. See OverridingUtil.areTypeParametersEquivalent()
//        superTypeParameters.forEachIndexed { index, parameter ->
//            if (!AbstractTypeChecker.areTypeParametersEquivalent(
//                    typeCheckerContext as AbstractTypeCheckerContext,
//                    subTypeParameters[index].type,
//                    parameter.type
//                )
//            ) return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch")
//        }

        val typeCheckerState = createIrTypeCheckerState(
            IrTypeSystemContextWithAdditionalAxioms(
                typeSystem,
                superTypeParameters,
                subTypeParameters
            )
        )

        superValueParameters.forEachIndexed { index, parameter ->
            if (!AbstractTypeChecker.equalTypes(
                    typeCheckerState,
                    subValueParameters[index].type,
                    parameter.type
                )
            ) return incompatible("Value parameter type mismatch")
        }

        return success()
    }

    private fun runExternalOverridabilityConditions(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
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
                CONFLICT -> return conflict("External condition failed")
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
                CONFLICT -> return conflict("External condition failed")
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

private val IrSimpleFunction?.hasExtensionReceiver: Boolean
    get() = this?.extensionReceiverParameter != null

internal val IrSimpleFunction?.hasDispatchReceiver: Boolean
    get() = this?.dispatchReceiverParameter != null

private val IrSimpleFunction.compiledValueParameters: List<IrValueParameter>
    get() = ArrayList<IrValueParameter>(valueParameters.size + 1).apply {
        extensionReceiverParameter?.let(::add)
        addAll(valueParameters)
    }

private val IrProperty.compiledValueParameters: List<IrValueParameter>
    get() = getter?.extensionReceiverParameter?.let(::listOf).orEmpty()

private val IrProperty.typeParameters: List<IrTypeParameter>
    get() = getter?.typeParameters.orEmpty()

private val IrProperty.isInline: Boolean
    get() = getter?.isInline == true || setter?.isInline == true
