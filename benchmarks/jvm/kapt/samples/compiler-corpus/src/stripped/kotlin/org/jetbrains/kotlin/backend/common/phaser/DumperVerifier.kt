/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.ActionState
import org.jetbrains.kotlin.config.phaser.BeforeOrAfter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.ir.validation.withBasicChecks
import org.jetbrains.kotlin.ir.validation.withTypeChecks
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

private val IrElement.elementName: String
    get() = when (this) {
        is IrModuleFragment ->
            this.name.asString()

        is IrFile ->
            this.name

        else ->
            this.toString()
    }

private fun ActionState.isDumpNeeded() =
    when (beforeOrAfter) {
        BeforeOrAfter.BEFORE -> config.shouldDumpStateBefore(phase)
        BeforeOrAfter.AFTER -> config.shouldDumpStateAfter(phase)
    }

private fun ActionState.isValidationNeeded() =
    when (beforeOrAfter) {
        BeforeOrAfter.BEFORE -> config.shouldValidateStateBefore(phase)
        BeforeOrAfter.AFTER -> config.shouldValidateStateAfter(phase)
    }

private fun dumpIrElement(actionState: ActionState, data: IrElement): String { TODO() }

fun <Context, Data> findKotlinBackendIr(context: Context, data: Data): IrElement? = TODO()

fun <Context : ErrorReportingContext, Data> getIrValidator(checkTypes: Boolean): Action<Data, Context> = TODO()

fun <Data, Context : ErrorReportingContext> getIrDumper(): Action<Data, Context> = TODO()

/**
 * IR dump and verify actions.
 *
 * Types are not checked in the IR during validation. But we may (and probably should) reconsider.
 */
val DEFAULT_IR_ACTIONS: Set<Action<IrElement, LoweringContext>> = setOf(getIrDumper(), getIrValidator(checkTypes = false))
