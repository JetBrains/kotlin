/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
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
    phase in when (beforeOrAfter) {
        BeforeOrAfter.BEFORE -> config.toDumpStateBefore
        BeforeOrAfter.AFTER -> config.toDumpStateAfter
    }

private fun ActionState.isValidationNeeded() =
    phase in when (beforeOrAfter) {
        BeforeOrAfter.BEFORE -> config.toValidateStateBefore
        BeforeOrAfter.AFTER -> config.toValidateStateAfter
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

fun dumpIrElement(actionState: ActionState, data: IrElement, context: Any?): String {
    val beforeOrAfterStr = actionState.beforeOrAfter.name.toLowerCase()

    var dumpText: String = ""
    val elementName: String

    val dumpOnlyFqName = actionState.config.dumpOnlyFqName
    if (dumpOnlyFqName != null) {
        elementName = dumpOnlyFqName
        data.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclaration) {
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

    val title = "-- IR for $elementName $beforeOrAfterStr ${actionState.phase.description}\n"
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

        val directoryFile = File(directoryPath)
        if (!directoryFile.isDirectory)
            if (!directoryFile.mkdirs())
                error("Can't create directory for IR dumps at $directoryPath")

        // Make dump files in a directory sorted by ID
        val phaseIdFormatted = "%02d".format(actionState.phaseCount)

        val fileName = "${phaseIdFormatted}_${actionState.phase.name}.$fileExtension"

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