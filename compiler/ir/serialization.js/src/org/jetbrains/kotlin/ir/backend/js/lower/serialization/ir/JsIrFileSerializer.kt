/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile

fun interface JsIrFileMetadataFactory {
    fun createJsIrFileMetadata(irFile: IrFile): JsIrFileMetadata
}

object JsIrFileEmptyMetadataFactory : JsIrFileMetadataFactory {
    override fun createJsIrFileMetadata(irFile: IrFile) = JsIrFileMetadata(emptyList())
}

class JsIrFileSerializer(
    settings: IrSerializationSettings,
    declarationTable: DeclarationTable,
    private val jsIrFileMetadataFactory: JsIrFileMetadataFactory
) : IrFileSerializer(settings, declarationTable) {
    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer) = node.isExportedDeclaration()
    override fun backendSpecificExplicitRootExclusion(node: IrAnnotationContainer) = node.isExportIgnoreDeclaration()
    override fun backendSpecificMetadata(irFile: IrFile) = jsIrFileMetadataFactory.createJsIrFileMetadata(irFile)
}
