/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleReferences
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WebConfigurationKeys
import org.jetbrains.kotlin.library.impl.buffer
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.security.MessageDigest

internal fun Hash128Bits.toProtoStream(out: CodedOutputStream) {
    out.writeFixed64NoTag(lowBytes.toLong())
    out.writeFixed64NoTag(highBytes.toLong())
}

internal fun readHash128BitsFromProtoStream(input: CodedInputStream): Hash128Bits {
    val lowBytes = input.readFixed64().toULong()
    val highBytes = input.readFixed64().toULong()
    return Hash128Bits(lowBytes, highBytes)
}

@JvmInline
value class ICHash(val hash: Hash128Bits = Hash128Bits()) {
    fun toProtoStream(out: CodedOutputStream) = hash.toProtoStream(out)

    companion object {
        fun fromProtoStream(input: CodedInputStream) = ICHash(readHash128BitsFromProtoStream(input))
    }
}

private class HashCalculatorForIC {
    private val md5Digest = MessageDigest.getInstance("MD5")

    fun update(data: ByteArray) = md5Digest.update(data)

    fun update(data: Int) = (0..3).forEach { md5Digest.update((data shr (it * 8)).toByte()) }

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

    fun finalize(): ICHash {
        val hashBytes = md5Digest.digest()
        md5Digest.reset()
        return hashBytes.buffer.asLongBuffer().let { longBuffer ->
            ICHash(Hash128Bits(longBuffer[0].toULong(), longBuffer[1].toULong()))
        }
    }
}

internal class ICHasher {
    private val hashCalculator = HashCalculatorForIC()

    fun calculateConfigHash(config: CompilerConfiguration): ICHash {
        val importantSettings = listOf(
            JSConfigurationKeys.GENERATE_DTS,
            JSConfigurationKeys.MODULE_KIND,
            WebConfigurationKeys.PROPERTY_LAZY_INITIALIZATION
        )
        hashCalculator.updateForEach(importantSettings) { key ->
            hashCalculator.update(key.toString())
            hashCalculator.update(config.get(key).toString())
        }

        hashCalculator.update(config.languageVersionSettings.toString())
        return hashCalculator.finalize()
    }

    fun calculateIrFunctionHash(function: IrFunction): ICHash {
        hashCalculator.update(function)
        return hashCalculator.finalize()
    }

    fun calculateIrAnnotationContainerHash(container: IrAnnotationContainer): ICHash {
        hashCalculator.updateAnnotationContainer(container)
        return hashCalculator.finalize()
    }

    fun calculateIrSymbolHash(symbol: IrSymbol): ICHash {
        hashCalculator.update(symbol.toString())
        // symbol rendering prints very little information about type parameters
        // TODO may be it make sense to update rendering?
        (symbol.owner as? IrTypeParametersContainer)?.let { typeParameters ->
            hashCalculator.updateForEach(typeParameters.typeParameters) { typeParameter ->
                hashCalculator.update(typeParameter.symbol.toString())
            }
        }
        (symbol.owner as? IrFunction)?.let { irFunction ->
            hashCalculator.updateForEach(irFunction.valueParameters) { functionParam ->
                // symbol rendering doesn't print default params information
                // it is important to understand if default params were added or removed
                hashCalculator.update(functionParam.defaultValue?.let { 1 } ?: 0)
            }
        }
        (symbol.owner as? IrAnnotationContainer)?.let(hashCalculator::updateAnnotationContainer)
        return hashCalculator.finalize()
    }
}

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
