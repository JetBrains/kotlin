/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation

import org.jetbrains.kotlin.ir.validation.checkers.IrChecker
import org.jetbrains.kotlin.ir.validation.checkers.IrNestedOffsetRangeChecker
import org.jetbrains.kotlin.ir.validation.checkers.IrOffsetsChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.*
import org.jetbrains.kotlin.ir.validation.checkers.expression.*
import org.jetbrains.kotlin.ir.validation.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.ir.validation.checkers.type.IrTypeParameterScopeChecker

data class IrValidatorConfig(
    val checkTreeConsistency: Boolean = false,
    val checkUnboundSymbols: Boolean = false,
    val checkers: Set<IrChecker> = emptySet(),
) {
    fun withCheckers(vararg checkers: IrChecker) = copy(checkers = this.checkers + checkers)
    fun withoutCheckers(vararg checkers: IrChecker) = copy(checkers = this.checkers - checkers.toSet())
}

/**
 * Specifies a set of basic checks that are applied on the 1st stage of compilation.
 * This should be bigger than [withBasicChecks], which are applied on 2nd stage.
 * New basic checkers should be added here, not breaking backward klib compatibility
 */
fun IrValidatorConfig.withBasicFirstStageChecks() =
    withBasicChecks().withCheckers(
        IrOffsetsChecker,
    )

/**
 * Specifies a set of basic checks that are applied on the 2nd stage of compilation.
 * Extending this list will probably break backwards klib compatibility, which is checked by Custom*CompilerFirstStageTestGenerated testrunners
 * So, consider adding new checkers to [withBasicFirstStageChecks] instead
 */
fun IrValidatorConfig.withBasicChecks() = withCheckers(
    IrFunctionDispatchReceiverChecker, IrConstructorReceiverChecker, IrFunctionParametersChecker,
    IrPropertyAccessorsChecker, IrFunctionPropertiesChecker,
    IrSetValueAssignabilityChecker,
    IrTypeOperatorTypeOperandChecker,
    IrPrivateDeclarationOverrideChecker,
)

fun IrValidatorConfig.withTypeChecks() = withCheckers(
    IrConstTypeChecker,
    IrStringConcatenationTypeChecker,
    IrGetObjectValueTypeChecker,
    IrGetValueTypeChecker,
    IrUnitTypeExpressionChecker,
    IrNothingTypeExpressionChecker,
    IrGetFieldTypeChecker,
    IrCallTypeChecker,
    IrTypeOperatorTypeChecker,
    IrDynamicTypeFieldAccessChecker,
)

fun IrValidatorConfig.withVarargChecks() = withCheckers(
    IrVarargTypesChecker,
    IrValueParameterVarargTypesChecker,
)

fun IrValidatorConfig.withInlineFunctionCallsiteCheck(checkInlineFunctionUseSites: InlineFunctionUseSiteChecker?) =
    if (checkInlineFunctionUseSites != null) {
        withCheckers(IrNoInlineUseSitesChecker(checkInlineFunctionUseSites))
    } else this

fun IrValidatorConfig.withAllChecks() = withBasicFirstStageChecks()
    .withVarargChecks()
    .withTypeChecks()
    .withCheckers(
        IrCallValueArgumentCountChecker,
        IrCallTypeArgumentCountChecker,
        IrVisibilityChecker.Strict,
        IrValueAccessScopeChecker,
        IrTypeParameterScopeChecker,
        IrCrossFileFieldUsageChecker,
        IrFieldVisibilityChecker,
        IrExpressionBodyInFunctionChecker,
        IrNestedOffsetRangeChecker,
    )
