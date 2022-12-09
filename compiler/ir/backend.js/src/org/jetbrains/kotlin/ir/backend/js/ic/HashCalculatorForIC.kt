/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleReferences
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
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
    private val md5 = MessageDigest.getInstance("MD5")

    fun update(data: ByteArray) = md5.update(data)

    fun update(data: Int) = (0..3).forEach { md5.update((data shr (it * 8)).toByte()) }

    fun update(data: String) {
        update(data.length)
        update(data.toByteArray())
    }

    fun update(irElement: IrElement) {
        irElement.accept(
            visitor = DumpIrTreeVisitor(
                out = object : Appendable {
                    override fun append(csq: CharSequence) = this.apply { update(csq.toString().toByteArray()) }
                    override fun append(csq: CharSequence, start: Int, end: Int) = append(csq.subSequence(start, end))
                    override fun append(c: Char) = append(c.toString())
                }
            ), data = ""
        )
    }

    fun updateAnnotationContainer(annotationContainer: IrAnnotationContainer) {
        updateForEach(annotationContainer.annotations, ::update)
    }

    inline fun <T> updateForEach(collection: Collection<T>, f: (T) -> Unit) {
        update(collection.size)
        collection.forEach { f(it) }
    }

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
    val importantSettings = listOf(
        JSConfigurationKeys.GENERATE_DTS,
        JSConfigurationKeys.MODULE_KIND,
        JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION
    )
    updateForEach(importantSettings) { key ->
        update(key.toString())
        update(get(key).toString())
    }

    update(languageVersionSettings.toString())
}.finalize()

internal fun IrSimpleFunction.irSimpleFunctionHashForIC() = HashCalculatorForIC().also {
    it.update(this)
}.finalize()

internal fun IrAnnotationContainer.irAnnotationContainerHashForIC() = HashCalculatorForIC().also {
    it.updateAnnotationContainer(this)
}.finalize()

internal fun IrSymbol.irSymbolHashForIC() = HashCalculatorForIC().also {
    it.update(toString())
    // symbol rendering prints very little information about type parameters
    // TODO may be it make sense to update rendering?
    (owner as? IrTypeParametersContainer)?.let { typeParameters ->
        it.updateForEach(typeParameters.typeParameters) { typeParameter ->
            it.update(typeParameter.symbol.toString())
        }
    }
    (owner as? IrFunction)?.let { irFunction ->
        it.updateForEach(irFunction.valueParameters) { functionParam ->
            // symbol rendering doesn't print default params information
            // it is important to understand if default params were added or removed
            it.update(functionParam.defaultValue?.let { 1 } ?: 0)
        }
    }
    (owner as? IrAnnotationContainer)?.let(it::updateAnnotationContainer)
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
    update(moduleKind.ordinal)

    updateForEach(importedModules) { importedModule ->
        update(importedModule.externalName)
        update(importedModule.internalName.toString())
        update(importedModule.relativeRequirePath ?: "")
    }

    updateForEach(transitiveJsExportFrom) { transitiveExport ->
        update(transitiveExport.internalName.toString())
        update(transitiveExport.externalName)
    }

    updateForEach(exports.keys.sorted()) { tag ->
        update(tag)
        update(exports[tag]!!)
    }

    updateForEach(imports.keys.sorted()) { tag ->
        val import = imports[tag]!!
        update(tag)
        update(import.exportedAs)
        update(import.moduleExporter.toString())
    }
}.finalize()
