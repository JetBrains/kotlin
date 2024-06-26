/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleReferences
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.impl.buffer
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.serialization.js.ModuleKind
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

private class HashCalculatorForIC(private val checkForClassStructuralChanges: Boolean = false) {
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

    fun updateProperty(irProperty: IrProperty) {
        if (irProperty.isConst) {
            irProperty.backingField?.initializer?.let(::update)
        }
    }

    fun updateSymbol(symbol: IrSymbol) {
        update(symbol.toString())

        if (checkForClassStructuralChanges) {
            (symbol.owner as? IrClass)?.let { irClass ->
                irClass.declarations.forEach {
                    updateSymbol(it.symbol)
                }
            }
        }

        (symbol.owner as? IrClass)?.takeIf { it.isInterface }?.let { irInterface ->
            // Adding or removing a method or property with a default implementation to an interface
            // should invalidate all children: we must regenerate JS code for them
            val openDeclarationSymbols = buildList {
                for (decl in irInterface.declarations) {
                    if (decl is IrOverridableMember && decl.modality == Modality.OPEN) {
                        add(decl.symbol)
                    }
                    if (decl is IrProperty) {
                        decl.getter?.takeIf { it.modality == Modality.OPEN }?.symbol?.let(::add)
                        decl.setter?.takeIf { it.modality == Modality.OPEN }?.symbol?.let(::add)
                    }
                }
            }
            updateForEach(openDeclarationSymbols, ::updateSymbol)
        }

        // symbol rendering prints very little information about type parameters
        // TODO may be it make sense to update rendering?
        (symbol.owner as? IrTypeParametersContainer)?.let { typeParameters ->
            updateForEach(typeParameters.typeParameters) { typeParameter ->
                update(typeParameter.symbol.toString())
            }
        }
        (symbol.owner as? IrFunction)?.let { irFunction ->
            updateForEach(irFunction.valueParameters) { functionParam ->
                // symbol rendering doesn't print default params information
                // it is important to understand if default params were added or removed
                update(functionParam.defaultValue?.let { 1 } ?: 0)
            }
        }
        (symbol.owner as? IrSimpleFunction)?.let { irSimpleFunction ->
            irSimpleFunction.correspondingPropertySymbol?.owner?.let(::updateProperty)
        }
        (symbol.owner as? IrAnnotationContainer)?.let(::updateAnnotationContainer)
        (symbol.owner as? IrProperty)?.let(::updateProperty)
    }

    inline fun <T> updateForEach(collection: Collection<T>, f: (T) -> Unit) {
        update(collection.size)
        collection.forEach { f(it) }
    }

    fun <T> updateConfigKeys(config: CompilerConfiguration, keys: List<CompilerConfigurationKey<out T>>, valueUpdater: (T) -> Unit) {
        updateForEach(keys) { key ->
            update(key.toString())
            val value = config.get(key)
            if (value == null) {
                md5Digest.update(0)
            } else {
                md5Digest.update(1)
                valueUpdater(value)
            }
        }
    }

    fun finalizeAndGetHash(): ICHash {
        val hashBytes = md5Digest.digest()
        md5Digest.reset()
        return hashBytes.buffer.asLongBuffer().let { longBuffer ->
            ICHash(Hash128Bits(longBuffer[0].toULong(), longBuffer[1].toULong()))
        }
    }
}

internal class ICHasher(checkForClassStructuralChanges: Boolean = false) {
    private val hashCalculator = HashCalculatorForIC(checkForClassStructuralChanges)

    fun calculateConfigHash(config: CompilerConfiguration): ICHash {
        hashCalculator.update(KotlinCompilerVersion.VERSION)

        val booleanKeys = listOf(
            JSConfigurationKeys.SOURCE_MAP,
            JSConfigurationKeys.META_INFO,
            JSConfigurationKeys.DEVELOPER_MODE,
            JSConfigurationKeys.USE_ES6_CLASSES,
            JSConfigurationKeys.GENERATE_POLYFILLS,
            JSConfigurationKeys.GENERATE_DTS,
            JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION,
            JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS,
            JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT,
            JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR,
            JSConfigurationKeys.OPTIMIZE_GENERATED_JS,
        )
        hashCalculator.updateConfigKeys(config, booleanKeys) { value: Boolean ->
            hashCalculator.update(if (value) 1 else 0)
        }

        val enumKeys = listOf(
            JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES,
            JSConfigurationKeys.SOURCEMAP_NAMES_POLICY,
            JSConfigurationKeys.MODULE_KIND,
        )
        hashCalculator.updateConfigKeys(config, enumKeys) { value: Enum<*> ->
            hashCalculator.update(value.ordinal)
        }

        hashCalculator.updateConfigKeys(
            config,
            listOf(
                JSConfigurationKeys.SOURCE_MAP_PREFIX,
                JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS
            )
        ) { value: String ->
            hashCalculator.update(value)
        }

        hashCalculator.updateConfigKeys(config, listOf(PartialLinkageConfig.KEY)) { value: PartialLinkageConfig ->
            hashCalculator.update(value.mode.ordinal)
            hashCalculator.update(value.logLevel.ordinal)
        }

        hashCalculator.update(config.languageVersionSettings.toString())
        return hashCalculator.finalizeAndGetHash()
    }

    fun calculateIrFunctionHash(function: IrFunction): ICHash {
        hashCalculator.update(function)
        return hashCalculator.finalizeAndGetHash()
    }

    fun calculateIrAnnotationContainerHash(container: IrAnnotationContainer): ICHash {
        hashCalculator.updateAnnotationContainer(container)
        return hashCalculator.finalizeAndGetHash()
    }

    fun calculateIrSymbolHash(symbol: IrSymbol): ICHash {
        hashCalculator.updateSymbol(symbol)
        return hashCalculator.finalizeAndGetHash()
    }
}

internal fun CrossModuleReferences.crossModuleReferencesHashForIC() = HashCalculatorForIC().apply {
    update(moduleKind.ordinal)

    updateForEach(importedModules) { importedModule ->
        update(importedModule.externalName)
        update(importedModule.internalName.toString())
        update(importedModule.relativeRequirePath ?: "")
    }

    updateForEach(transitiveExportFrom) { transitiveExport ->
        update(transitiveExport.internalName.toString())
        update(transitiveExport.externalName)
    }

    updateForEach(exports.keys.sorted()) { tag ->
        update(tag)
        update(exports[tag]!!)
    }

    updateForEach(importsWithEffect.sortedBy { it.moduleExporter.externalName }) { import ->
        update(import.moduleExporter.externalName)
    }

    updateForEach(imports.keys.sorted()) { tag ->
        val import = imports[tag]!!
        update(tag)
        update(import.exportedAs)

        if (moduleKind == ModuleKind.ES) {
            update(import.moduleExporter.internalName.toString())
            update(import.moduleExporter.externalName)
            update(import.moduleExporter.relativeRequirePath ?: "")
        } else {
            update(import.moduleExporter.internalName.toString())
        }
    }
}.finalizeAndGetHash()