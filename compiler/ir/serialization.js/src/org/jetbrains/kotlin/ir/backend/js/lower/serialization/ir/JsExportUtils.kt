/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.ir.util.isExpect
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

internal fun IrAnnotationContainer.isJsExportDeclaration(): Boolean {
    return annotations.hasAnnotation(JsStandardClassIds.Annotations.JsExport.asSingleFqName()) && !isJsExportIgnoreDeclaration()
}

internal fun IrAnnotationContainer.isJsExportIgnoreDeclaration(): Boolean {
    return annotations.hasAnnotation(JsStandardClassIds.Annotations.JsExportIgnore.asSingleFqName())
}

private val IrDeclarationWithName.exportedJsExportName: String
    get() = getAnnotation(JsStandardClassIds.Annotations.JsName.asSingleFqName())?.getSingleConstStringArgument() ?: name.toString()

internal fun IrConstructorCall.getSingleConstStringArgument() =
    (arguments[0] as IrConst).value as String

fun IrModuleFragment.collectJsExportNamesSequence(): Sequence<Triple<IrFile, IrDeclarationWithName, String>> =
    files.asSequence().flatMap { irFile ->
        val isFileJsExported = irFile.annotations.hasAnnotation(
            JsStandardClassIds.Annotations.JsExport.asSingleFqName()
        )

        irFile.declarations.asSequence()
            .filterIsInstance<IrDeclarationWithName>()
            .filter { declaration ->
                if (isFileJsExported) !declaration.isJsExportIgnoreDeclaration()
                else declaration.isJsExportDeclaration()
            }
            .filter { !it.isEffectivelyExternal() && !it.isExpect }
            .map { declaration ->
                Triple(irFile, declaration, declaration.exportedJsExportName)
            }
    }

fun IrModuleFragment.collectJsExportNames(): Map<IrFile, Map<IrDeclarationWithName, String>> {
    val result = mutableMapOf<IrFile, MutableMap<IrDeclarationWithName, String>>()

    collectJsExportNamesSequence().forEach { (file, decl, name) ->
        result.getOrPut(file) { mutableMapOf() }[decl] = name
    }

    return result
}