/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.name.JsStandardClassIds

internal fun IrAnnotationContainer.isExportedDeclaration(): Boolean {
    return annotations.hasAnnotation(JsStandardClassIds.Annotations.JsExport.asSingleFqName()) && !isExportIgnoreDeclaration()
}

internal fun IrAnnotationContainer.isExportIgnoreDeclaration(): Boolean {
    return annotations.hasAnnotation(JsStandardClassIds.Annotations.JsExportIgnore.asSingleFqName())
}

private val IrDeclarationWithName.exportedName: String
    get() = getAnnotation(JsStandardClassIds.Annotations.JsName.asSingleFqName())?.getSingleConstStringArgument() ?: name.toString()

@Suppress("UNCHECKED_CAST")
private fun IrConstructorCall.getSingleConstStringArgument() =
    (getValueArgument(0) as IrConst<String>).value

fun IrModuleFragment.collectExportedNames(): Map<IrFile, Map<IrDeclarationWithName, String>> {
    return files.associateWith { irFile ->
        val isFileExported = irFile.annotations.hasAnnotation(JsStandardClassIds.Annotations.JsExport.asSingleFqName())

        val exportedDeclarations = irFile.declarations.asSequence()
            .filterIsInstance<IrDeclarationWithName>()
            .filter { if (isFileExported) !it.isExportIgnoreDeclaration() else it.isExportedDeclaration() }
            .filter { !it.isEffectivelyExternal() && !it.isExpect }
            .map {
                it to it.exportedName
            }.toMap()
        exportedDeclarations
    }
}
