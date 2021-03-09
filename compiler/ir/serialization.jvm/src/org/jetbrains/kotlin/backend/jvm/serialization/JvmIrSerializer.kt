/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ByteString

class JvmIrSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    externallyVisibleOnly: Boolean = true,
    skipExpects: Boolean = false,
) : IrFileSerializer(messageLogger, declarationTable, expectDescriptorToSymbol, externallyVisibleOnly, skipExpects) {

    // Usage protocol: construct an instance, call only one of `serializeIrFile()` and `serializeTopLevelClass()` only once.

    fun serializeJvmIrFile(irFile: IrFile): JvmIr.JvmIrFile {
        val proto = JvmIr.JvmIrFile.newBuilder()

        irFile.declarations.filter { it !is IrClass }.forEach { declaration ->
            proto.addDeclaration(serializeDeclaration(declaration))
        }
        proto.addAllAnnotation(serializeAnnotations(irFile.annotations))

        proto.auxTables = serializeAuxTables()

        return proto.build()
    }

    fun serializeTopLevelClass(irClass: IrClass): JvmIr.JvmIrClass {
        val proto = JvmIr.JvmIrClass.newBuilder()
        proto.irClass = serializeIrClass(irClass)
        proto.auxTables = serializeAuxTables()
        return proto.build()
    }

    private fun serializeAuxTables(): JvmIr.AuxTables {
        val proto = JvmIr.AuxTables.newBuilder()
        protoTypeArray.forEach { proto.addType(it.toByteString()) }
        protoIdSignatureArray.forEach { proto.addSignature(it.toByteString()) }
        protoStringArray.forEach { proto.addString(ByteString.copyFromUtf8(it)) }
        protoBodyArray.forEach { proto.addBody(ByteString.copyFrom(it.toByteArray())) }
        return proto.build()
    }
}