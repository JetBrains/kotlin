/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi

class JsIrLinker(
    currentModule: ModuleDescriptor,
    mangler: KotlinMangler,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable
) : KotlinIrLinker(logger, builtIns, symbolTable, emptyList(), null, mangler),
    DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    override val descriptorReferenceDeserializer =
        JsDescriptorReferenceDeserializer(currentModule, mangler, builtIns)

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId) =
        moduleDescriptor.kotlinLibrary.irDeclaration(uniqId.index, fileIndex)

    override fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int) =
        moduleDescriptor.kotlinLibrary.symbol(symbolIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
        moduleDescriptor.kotlinLibrary.type(typeIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
        moduleDescriptor.kotlinLibrary.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
        moduleDescriptor.kotlinLibrary.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
        moduleDescriptor.kotlinLibrary.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
        moduleDescriptor.kotlinLibrary.fileCount()

    override fun checkAccessibility(declarationDescriptor: DeclarationDescriptor): Boolean {
        require(declarationDescriptor is DeclarationDescriptorWithVisibility)
        return declarationDescriptor.isPublishedApi() || declarationDescriptor.visibility.let { it.isPublicAPI || it == Visibilities.INTERNAL }
    }

    private val ModuleDescriptor.userName get() = kotlinLibrary.libraryFile.absolutePath
}