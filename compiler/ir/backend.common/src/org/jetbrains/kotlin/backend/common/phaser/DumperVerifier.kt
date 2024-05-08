/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
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

fun <Data, Context> makeDumpAction(dumper: Action<Data, Context>): Action<Data, Context> =
    { phaseState, data, context ->
        if (phaseState.isDumpNeeded())
            dumper(phaseState, data, context)
    }

fun <Data, Context> makeVerifyAction(verifier: (Context, Data) -> Unit): Action<Data, Context> =
    { phaseState, data, context ->
        if (phaseState.isValidationNeeded())
            verifier(context, data)
    }

fun dumpIrElement(actionState: ActionState, data: IrElement, @Suppress("UNUSED_PARAMETER") context: Any?): String {
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

typealias Dumper<Data, Context> = (ActionState, Data, Context) -> String?

fun <Data, Context> dumpToFile(
    fileExtension: String,
    dumper: Dumper<Data, Context>
): Action<Data, Context> =
    fun(actionState: ActionState, data: Data, context: Context) {
        val directoryPath = actionState.config.dumpToDirectory ?: return
        val dumpContent = dumper(actionState, data, context) ?: return

        // TODO in JVM BE most of lowerings run per file and "dump" is called per file,
        //  so each run of this function overwrites dump written for the previous one.
        val directoryFile =
            File(directoryPath +
                         ((data as? IrModuleFragment)?.let { "/" + it.name.asString().removeSurrounding("<", ">") } ?: ""))
        if (!directoryFile.isDirectory)
            if (!directoryFile.mkdirs())
                error("Can't create directory for IR dumps at $directoryPath")

        // Make dump files in a directory sorted by ID
        val phaseIdFormatted = "%02d".format(actionState.phaseCount)

        val dumpStrategy = System.getProperty("org.jetbrains.kotlin.compiler.ir.dump.strategy")
        val extPrefix = if (dumpStrategy == "KotlinLike") "kt." else ""

        val fileName = "${phaseIdFormatted}_${actionState.beforeOrAfter}.${actionState.phase.name}.$extPrefix$fileExtension"

        File(directoryFile, fileName).writeText(dumpContent)
    }

fun <Data, Context> dumpToStdout(
    dumper: Dumper<Data, Context>
): Action<Data, Context> =
    fun(actionState: ActionState, data: Data, context: Context) {
        if (actionState.config.dumpToDirectory != null) return
        val dumpContent = dumper(actionState, data, context) ?: return
        println("\n\n----------------------------------------------")
        println(dumpContent)
        println()
    }

val defaultDumper = makeDumpAction(dumpToStdout(::dumpIrElement) + dumpToFile("ir", ::dumpIrElement))

fun validationCallback(
    context: CommonBackendContext,
    fragment: IrElement,
    mode: IrVerificationMode = IrVerificationMode.ERROR,
    checkProperties: Boolean = false,
    checkTypes: Boolean = false,
) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = mode == IrVerificationMode.ERROR,
        ensureAllNodesAreDifferent = true,
        checkTypes = checkTypes,
        checkDescriptors = false,
        checkProperties = checkProperties,
    )
    fragment.accept(IrValidator(context, validatorConfig), null)
    fragment.checkDeclarationParents()
}

val validationAction = makeVerifyAction(::validationCallback)

abstract class IrValidationPhase<Context : CommonBackendContext>(val context: Context) : ModuleLoweringPass {

    final override fun lower(irModule: IrModuleFragment) {
        if (context.configuration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE) != IrVerificationMode.NONE) {
            validate(irModule)
        }
    }

    protected open fun validate(irModule: IrModuleFragment) {
        validationCallback(context, irModule)
    }
}

@PhaseDescription(
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering",
)
open class IrValidationBeforeLoweringPhase<Context : CommonBackendContext>(context: Context) : IrValidationPhase<Context>(context)

@PhaseDescription(
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering",
)
open class IrValidationAfterLoweringPhase<Context : CommonBackendContext>(context: Context) : IrValidationPhase<Context>(context)

fun <Context> findBackendContext(context: Context): CommonBackendContext? = when (context) {
    is CommonBackendContext -> context
    is BackendContextHolder<*> -> context.backendContext
    else -> null
}

fun <Context, Data> findKotlinBackendIr(context: Context, data: Data): IrElement? = when {
    data is IrElement -> data
    data is KotlinBackendIrHolder -> data.kotlinIr
    context is KotlinBackendIrHolder -> context.kotlinIr
    else -> null
}

fun <Context : LoggingContext, Data> getIrValidator(checkTypes: Boolean): Action<Data, Context> =
    fun(state: ActionState, data: Data, context: Context) {
        if (!state.isValidationNeeded()) return

        val backendContext: CommonBackendContext? = findBackendContext(context)
        if (backendContext == null) {
            context.messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: insufficient context."
            )
            return
        }
        val element = findKotlinBackendIr(context, data)
        if (element == null) {
            context.messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: IR not found."
            )
            return
        }
        validationCallback(backendContext, element, checkTypes = checkTypes)
    }

fun <Data, Context : LoggingContext> getIrDumper(): Action<Data, Context> =
    fun(state: ActionState, data: Data, context: Context) {
        if (!state.isDumpNeeded()) return
        val backendContext: CommonBackendContext? = findBackendContext(context)
        if (backendContext == null) {
            context.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Cannot dump IR ${state.beforeOrAfter} ${state.phase}: insufficient context."
            )
            return
        }
        val element = findKotlinBackendIr(context, data)
        if (element == null) {
            context.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Cannot dump IR ${state.beforeOrAfter} ${state.phase}: IR not found."
            )
            return
        }
        defaultDumper(state, element, backendContext)
    }
