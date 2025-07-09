/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

abstract class IrLibraryFile {
    abstract fun declaration(index: Int): ProtoDeclaration
    abstract fun inlineDeclaration(index: Int): ProtoDeclaration
    abstract fun type(index: Int): ProtoType
    abstract fun signature(index: Int): ProtoIdSignature
    abstract fun string(index: Int): String
    abstract fun expressionBody(index: Int): ProtoExpression
    abstract fun statementBody(index: Int): ProtoStatement
    abstract fun debugInfo(index: Int): String?
    abstract fun fileEntry(index: Int): ProtoFileEntry?
}

abstract class IrLibraryBytesSource {
    abstract fun irDeclaration(index: Int): ByteArray
    abstract fun irInlineDeclaration(index: Int): ByteArray
    abstract fun type(index: Int): ByteArray
    abstract fun signature(index: Int): ByteArray
    abstract fun string(index: Int): ByteArray
    abstract fun body(index: Int): ByteArray
    abstract fun debugInfo(index: Int): ByteArray?
    abstract fun fileEntry(index: Int): ByteArray?
}

class IrKlibBytesSource(private val klib: IrLibrary, private val fileIndex: Int) : IrLibraryBytesSource() {
    override fun irDeclaration(index: Int): ByteArray = klib.irDeclaration(index, fileIndex)
    override fun irInlineDeclaration(index: Int): ByteArray = klib.irInlineDeclaration(index, fileIndex)
    override fun type(index: Int): ByteArray = klib.type(index, fileIndex)
    override fun signature(index: Int): ByteArray = klib.signature(index, fileIndex)
    override fun string(index: Int): ByteArray = klib.string(index, fileIndex)
    override fun body(index: Int): ByteArray = klib.body(index, fileIndex)
    override fun debugInfo(index: Int): ByteArray? = klib.debugInfo(index, fileIndex)
    override fun fileEntry(index: Int): ByteArray? = klib.fileEntry(index, fileIndex)
}

fun IrLibraryFile.deserializeFqName(fqn: List<Int>): String =
    fqn.joinToString(".", transform = ::string)


class IrLibraryFileFromBytes(private val bytesSource: IrLibraryBytesSource) : IrLibraryFile() {

    override fun declaration(index: Int): ProtoDeclaration =
        ProtoDeclaration.parseFrom(bytesSource.irDeclaration(index).codedInputStream, extensionRegistryLite)

    override fun inlineDeclaration(index: Int): ProtoDeclaration =
        ProtoDeclaration.parseFrom(bytesSource.irInlineDeclaration(index).codedInputStream, extensionRegistryLite)

    override fun type(index: Int): ProtoType = ProtoType.parseFrom(bytesSource.type(index).codedInputStream, extensionRegistryLite)

    override fun signature(index: Int): ProtoIdSignature =
        ProtoIdSignature.parseFrom(bytesSource.signature(index).codedInputStream, extensionRegistryLite)

    override fun string(index: Int): String = WobblyTF8.decode(bytesSource.string(index))

    override fun expressionBody(index: Int): ProtoExpression =
        ProtoExpression.parseFrom(bytesSource.body(index).codedInputStream, extensionRegistryLite)

    override fun statementBody(index: Int): ProtoStatement =
        ProtoStatement.parseFrom(bytesSource.body(index).codedInputStream, extensionRegistryLite)

    override fun debugInfo(index: Int): String? = bytesSource.debugInfo(index)?.let { WobblyTF8.decode(it) }
    override fun fileEntry(index: Int): ProtoFileEntry? = bytesSource.fileEntry(index)?.let {
        ProtoFileEntry.parseFrom(it, extensionRegistryLite)
    }

    companion object {
        val extensionRegistryLite: ExtensionRegistryLite = ExtensionRegistryLite.newInstance()
    }
}

fun IrLibraryFile.fileEntry(protoFile: ProtoFile): FileEntry =
    if (protoFile.hasFileEntryId())
        fileEntry(protoFile.fileEntryId) ?: error("Invalid KLib: cannot read file entry by its index")
    else {
        require(protoFile.hasFileEntry()) { "Invalid KLib: either fileEntry or fileEntryId must be present" }
        protoFile.fileEntry
    }

fun IrLibrary.fileEntry(protoFile: ProtoFile, fileIndex: Int): FileEntry =
    if (protoFile.hasFileEntryId() && hasFileEntriesTable) {
        val fileEntry = fileEntry(protoFile.fileEntryId, fileIndex) ?: error("Invalid KLib: cannot read file entry by its index")
        ProtoFileEntry.parseFrom(fileEntry)
    } else {
        require(protoFile.hasFileEntry()) {
            "Invalid KLib: either fileEntry or valid fileEntryId must be present. Valid fileEntryId is a valid index in existing file entries table"
        }
        protoFile.fileEntry
    }
