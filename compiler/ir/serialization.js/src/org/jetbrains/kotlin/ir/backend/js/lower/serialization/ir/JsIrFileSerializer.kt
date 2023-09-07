/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.IrMessageLogger

fun interface JsIrFileMetadataFactory {
    fun createJsIrFileMetadata(irFile: IrFile): JsIrFileMetadata
}

object JsIrFileEmptyMetadataFactory : JsIrFileMetadataFactory {
    override fun createJsIrFileMetadata(irFile: IrFile) = JsIrFileMetadata(emptyList())
}

class JsIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    compatibilityMode: CompatibilityMode,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>,
    private val jsIrFileMetadataFactory: JsIrFileMetadataFactory
) : IrFileSerializer(
    messageLogger,
    declarationTable,
    compatibilityMode = compatibilityMode,
    languageVersionSettings = languageVersionSettings,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    normalizeAbsolutePaths = normalizeAbsolutePaths,
    sourceBaseDirs = sourceBaseDirs
) {
    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer) = node.isExportedDeclaration()
    override fun backendSpecificExplicitRootExclusion(node: IrAnnotationContainer) = node.isExportIgnoreDeclaration()
    override fun backendSpecificMetadata(irFile: IrFile) = jsIrFileMetadataFactory.createJsIrFileMetadata(irFile)
}
