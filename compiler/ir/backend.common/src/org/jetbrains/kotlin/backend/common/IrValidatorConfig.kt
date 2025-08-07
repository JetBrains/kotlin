/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.checkers.IrChecker
import org.jetbrains.kotlin.backend.common.checkers.declaration.*
import org.jetbrains.kotlin.backend.common.checkers.expression.*
import org.jetbrains.kotlin.backend.common.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.backend.common.checkers.type.IrTypeParameterScopeChecker

data class IrValidatorConfig(
    val checkTreeConsistency: Boolean = false,
    val checkUnboundSymbols: Boolean = false,
    val checkers: Set<IrChecker> = emptySet(),
) {
    fun withCheckers(vararg checkers: IrChecker) = copy(checkers = this.checkers + checkers)
    fun withoutCheckers(vararg checkers: IrChecker) = copy(checkers = this.checkers - checkers.toSet())
}

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

fun IrValidatorConfig.withAllChecks() = withBasicChecks()
    .withVarargChecks()
    .withTypeChecks()
    .withCheckers(
        IrCallValueArgumentCountChecker,
        IrCallTypeArgumentCountChecker,
        IrVisibilityChecker,
        IrValueAccessScopeChecker,
        IrTypeParameterScopeChecker,
        IrCrossFileFieldUsageChecker,
        IrFieldVisibilityChecker,
        IrExpressionBodyInFunctionChecker
    )
