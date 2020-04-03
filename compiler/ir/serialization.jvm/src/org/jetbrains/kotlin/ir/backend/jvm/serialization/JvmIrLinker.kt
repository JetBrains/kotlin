/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable

class JvmIrLinker(logger: LoggingContext, builtIns: IrBuiltIns, symbolTable: SymbolTable) :
    KotlinIrLinker(logger, builtIns, symbolTable, emptyList(), null) {

    override fun handleNoModuleDeserializerFound(idSignature: IdSignature): DeserializationState<*> {
        // TODO: Implement special java-module deserializer instead of this hack
        return globalDeserializationState // !!!!!! Wrong, as external references will all have UniqId.NONE
    }

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, idSigIndex: Int) =
        moduleDescriptor.kotlinLibrary.irDeclaration(idSigIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
        moduleDescriptor.kotlinLibrary.type(typeIndex, fileIndex)

    override fun readSignature(moduleDescriptor: ModuleDescriptor, fileIndex: Int, signatureIndex: Int) =
        moduleDescriptor.kotlinLibrary.signature(signatureIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
        moduleDescriptor.kotlinLibrary.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
        moduleDescriptor.kotlinLibrary.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
        moduleDescriptor.kotlinLibrary.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
        moduleDescriptor.kotlinLibrary.fileCount()

    override fun resolveModuleDeserializer(moduleDescriptor: ModuleDescriptor): IrModuleDeserializer? {
        return deserializersForModules[moduleDescriptor]
    }

    // TODO: implement special Java deserializer
    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy): IrModuleDeserializer =
        JvmModuleDeserializer(moduleDescriptor, strategy)

    private inner class JvmModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy) :
        KotlinIrLinker.IrModuleDeserializer(moduleDescriptor, strategy)
}