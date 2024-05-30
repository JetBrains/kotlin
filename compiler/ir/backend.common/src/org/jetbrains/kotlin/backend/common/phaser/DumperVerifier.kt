/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.validateIr
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
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

private fun dumpIrElement(actionState: ActionState, data: IrElement): String {
    val beforeOrAfterStr = actionState.beforeOrAfter.name.toLowerCaseAsciiOnly()

    var dumpText = ""
    val elementName: String

    val dumpStrategy = System.getProperty("org.jetbrains.kotlin.compiler.ir.dump.strategy")
    val dump: IrElement.() -> String = if (dumpStrategy == "KotlinLike") IrElement::dumpKotlinLike else IrElement::dump

    val dumpOnlyFqName = actionState.config.dumpOnlyFqName
    if (dumpOnlyFqName != null) {
        elementName = dumpOnlyFqName
        data.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                if (declaration is IrDeclarationWithName && FqName(dumpOnlyFqName) == declaration.fqNameWhenAvailable) {
                    dumpText += declaration.dump()
                } else {
                    super.visitDeclaration(declaration)
                }
            }
        })
    } else {
        elementName = data.elementName
        dumpText = data.dump()
    }

    val title = "// --- IR for $elementName $beforeOrAfterStr ${actionState.phase.description}\n"
    return title + dumpText
}

fun <Context, Data> findKotlinBackendIr(context: Context, data: Data): IrElement? = when {
    data is IrElement -> data
    data is KotlinBackendIrHolder -> data.kotlinIr
    context is KotlinBackendIrHolder -> context.kotlinIr
    else -> null
}

fun <Context : ErrorReportingContext, Data> getIrValidator(checkTypes: Boolean): Action<Data, Context> =
    fun(state: ActionState, data: Data, context: Context) {
        if (!state.isValidationNeeded()) return
        val messageCollector = context.messageCollector
        if (context !is BackendContextHolder) {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: insufficient context."
            )
            return
        }
        val element = findKotlinBackendIr(context, data)
        if (element == null) {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: IR not found."
            )
            return
        }
        validateIr(messageCollector, IrVerificationMode.ERROR) {
            performBasicIrValidation(
                element,
                context.heldBackendContext.irBuiltIns,
                phaseName = "${state.beforeOrAfter.name.toLowerCaseAsciiOnly()} ${state.phase}",
                checkTypes = checkTypes,
            )
        }
    }

fun <Data, Context : ErrorReportingContext> getIrDumper(): Action<Data, Context> =
    fun(state: ActionState, data: Data, context: Context) {
        if (!state.isDumpNeeded()) return
        val element = findKotlinBackendIr(context, data)
        if (element == null) {
            context.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Cannot dump IR ${state.beforeOrAfter} ${state.phase}: IR not found."
            )
            return
        }
        val dumpContent = dumpIrElement(state, element)
        val dumpDirectory = state.config.dumpToDirectory
        if (dumpDirectory == null) {
            println("\n\n----------------------------------------------")
            println(dumpContent)
            println()
        } else {
            // TODO in JVM BE most of lowerings run per file and "dump" is called per file,
            //  so each run of this function overwrites dump written for the previous one.
            val directoryFile =
                File(dumpDirectory +
                             ((data as? IrModuleFragment)?.let { "/" + it.name.asString().removeSurrounding("<", ">") } ?: ""))
            if (!directoryFile.isDirectory)
                if (!directoryFile.mkdirs())
                    error("Can't create directory for IR dumps at $dumpDirectory")

            // Make dump files in a directory sorted by ID
            val phaseIdFormatted = "%02d".format(state.phaseCount)

            val dumpStrategy = System.getProperty("org.jetbrains.kotlin.compiler.ir.dump.strategy")
            val extPrefix = if (dumpStrategy == "KotlinLike") "kt." else ""

            val fileName = "${phaseIdFormatted}_${state.beforeOrAfter}.${state.phase.name}.${extPrefix}ir"

            File(directoryFile, fileName).writeText(dumpContent)
        }
    }

/**
 * IR dump and verify actions.
 *
 * Types are not checked in the IR during validation. But we may (and probably should) reconsider.
 */
val DEFAULT_IR_ACTIONS: Set<Action<IrElement, CommonBackendContext>> = setOf(getIrDumper(), getIrValidator(checkTypes = false))
