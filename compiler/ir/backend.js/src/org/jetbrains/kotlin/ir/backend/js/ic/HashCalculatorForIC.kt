/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleReferences
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File
import java.security.MessageDigest

@JvmInline
value class ICHash(private val value: ULong = 0UL) {
    fun combineWith(other: ICHash) = ICHash(value xor (other.value + 0x9e3779b97f4a7c15UL + (value shl 12) + (value shr 4)))

    override fun toString() = value.toString(16)

    fun toProtoStream(out: CodedOutputStream) = out.writeFixed64NoTag(value.toLong())

    companion object {
        fun fromProtoStream(input: CodedInputStream) = ICHash(input.readFixed64().toULong())
    }
}

private class HashCalculatorForIC {
    private companion object {
        private val md5 = MessageDigest.getInstance("MD5")
    }

    init {
        md5.reset()
    }

    fun update(data: ByteArray) = md5.update(data)
    fun update(data: String) = md5.update(data.toByteArray())

    private fun bytesToULong(d: ByteArray, offset: Int): ULong {
        var hash = 0UL
        repeat(8) {
            hash = hash or ((d[it + offset].toULong() and 0xFFUL) shl (it * 8))
        }
        return hash
    }

    fun finalize(): ICHash {
        val d = md5.digest()
        return ICHash(bytesToULong(d, 0)).combineWith(ICHash(bytesToULong(d, 8)))
    }
}

internal fun File.fileHashForIC(): ICHash {
    val md5 = HashCalculatorForIC()
    fun File.process(prefix: String = "") {
        if (isDirectory) {
            this.listFiles()!!.sortedBy { it.name }.forEach {
                md5.update(prefix + it.name)
                it.process(prefix + it.name + "/")
            }
        } else {
            md5.update(readBytes())
        }
    }
    this.process()
    return md5.finalize()
}

internal fun CompilerConfiguration.configHashForIC() = HashCalculatorForIC().apply {
    val importantBooleanSettingKeys = listOf(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
    for (key in importantBooleanSettingKeys) {
        update(key.toString())
        update(getBoolean(key).toString())
    }
}.finalize()

internal fun IrElement.irElementHashForIC() = HashCalculatorForIC().also {
    accept(
        visitor = DumpIrTreeVisitor(
            out = object : Appendable {
                override fun append(csq: CharSequence) = this.apply { it.update(csq.toString()) }
                override fun append(csq: CharSequence, start: Int, end: Int) = append(csq.subSequence(start, end))
                override fun append(c: Char) = this.apply { it.update(c.toString()) }
            }
        ), data = ""
    )
}.finalize()

internal fun String.stringHashForIC() = HashCalculatorForIC().also { it.update(this) }.finalize()

internal fun KotlinLibrary.fingerprint(fileIndex: Int) = HashCalculatorForIC().apply {
    update(types(fileIndex))
    update(signatures(fileIndex))
    update(strings(fileIndex))
    update(declarations(fileIndex))
    update(bodies(fileIndex))
}.finalize()

internal fun CrossModuleReferences.crossModuleReferencesHashForIC() = HashCalculatorForIC().apply {
    for (importedModule in importedModules) {
        update(importedModule.externalName)
        update(importedModule.internalName.toString())
    }
    for (exportFrom in transitiveJsExportFrom) {
        update(exportFrom.toString())
    }
    for (tag in exports.keys.sorted()) {
        update(tag)
        update(exports[tag]!!)
    }
    for (tag in imports.keys.sorted()) {
        val import = imports[tag]!!
        update(tag)
        update(import.exportedAs)
        update(import.moduleExporter.toString())
    }
}.finalize()
