/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.name.FqName

class JsIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    compatibilityMode: CompatibilityMode,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>
) : IrFileSerializer(
    messageLogger,
    declarationTable,
    compatibilityMode = compatibilityMode,
    languageVersionSettings = languageVersionSettings,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    normalizeAbsolutePaths = normalizeAbsolutePaths,
    sourceBaseDirs = sourceBaseDirs
) {
    companion object {
        private val JS_NAME_FQN = FqName("kotlin.js.JsName")
        private val JS_EXPORT_FQN = FqName("kotlin.js.JsExport")
        private val JS_EXPORT_IGNORE_FQN = FqName("kotlin.js.JsExport.Ignore")
    }

    private fun IrAnnotationContainer.isExportedDeclaration(): Boolean {
        return annotations.hasAnnotation(JS_EXPORT_FQN) && !isExportIgnoreDeclaration()
    }

    private fun IrAnnotationContainer.isExportIgnoreDeclaration(): Boolean {
        return annotations.hasAnnotation(JS_EXPORT_IGNORE_FQN)
    }

    private val IrDeclarationWithName.exportedName: String
        get() = getAnnotation(JS_NAME_FQN)?.getSingleConstStringArgument() ?: name.toString()

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer) = node.isExportedDeclaration()
    override fun backendSpecificExplicitRootExclusion(node: IrAnnotationContainer) = node.isExportIgnoreDeclaration()
    override fun backendSpecificMetadata(irFile: IrFile): FileBackendSpecificMetadata {
        val isFileExported = irFile.annotations.hasAnnotation(JS_EXPORT_FQN)

        val exportedNames = irFile.declarations.asSequence()
            .filterIsInstance<IrDeclarationWithName>()
            .filter { if (isFileExported) !it.isExportIgnoreDeclaration() else it.isExportedDeclaration() }
            .filter { !it.isEffectivelyExternal() && !it.isExpect }
            .map { it.exportedName }
            .toList()

        return JsIrFileMetadata(exportedNames)
    }

    @Suppress("UNCHECKED_CAST")
    private fun IrConstructorCall.getSingleConstStringArgument() =
        (getValueArgument(0) as IrConst<String>).value
}
