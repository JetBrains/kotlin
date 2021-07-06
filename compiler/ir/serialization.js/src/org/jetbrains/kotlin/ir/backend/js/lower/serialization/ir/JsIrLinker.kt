/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsMapping
import org.jetbrains.kotlin.ir.backend.js.ic.IcModuleDeserializer
import org.jetbrains.kotlin.ir.backend.js.ic.IdSignatureSerializerWithForIC
import org.jetbrains.kotlin.ir.backend.js.ic.SerializedIcData
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.containsErrorCode

class JsIrLinker(
    private val currentModule: ModuleDescriptor?, messageLogger: IrMessageLogger, builtIns: IrBuiltIns, symbolTable: SymbolTable,
    override val functionalInterfaceFactory: IrAbstractFunctionFactory,
    override val translationPluginContext: TranslationPluginContext?,
    private val icData: ICData? = null,
    private val loweredIcData: Map<ModuleDescriptor, SerializedIcData> = emptyMap(),
    private val useGlobalSignatures: Boolean = false,
) : KotlinIrLinker(currentModule, messageLogger, builtIns, symbolTable, emptyList()) {

    override val fakeOverrideBuilder = FakeOverrideBuilder(this, symbolTable, JsManglerIr, builtIns,
                                                           signatureSerializerFactory = { publicSignatureBuilder, table ->
        if (useGlobalSignatures) IdSignatureSerializerWithForIC(publicSignatureBuilder, table) else IdSignatureSerializer(publicSignatureBuilder, table)
    })

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

    private val IrLibrary.libContainsErrorCode: Boolean
        get() = this is KotlinLibrary && this.containsErrorCode

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary?, strategy: DeserializationStrategy): IrModuleDeserializer {
        require(klib != null) { "Expecting kotlin library" }
        loweredIcData[moduleDescriptor]?.let { loweredIcData ->
            return IcModuleDeserializer(
                symbolTable.irFactory as PersistentIrFactory,
                mapping,
                this,
                loweredIcData,
                moduleDescriptor,
                klib,
                strategy,
                containsErrorCode = klib.libContainsErrorCode,
                useGlobalSignatures = useGlobalSignatures
            )
        }
        return JsModuleDeserializer(moduleDescriptor, klib, strategy, klib.libContainsErrorCode)
    }

    val mapping: JsMapping by lazy { JsMapping(symbolTable.irFactory) }

    private inner class JsModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary, strategy: DeserializationStrategy, allowErrorCode: Boolean) :
        BasicIrModuleDeserializer(this, moduleDescriptor, klib, strategy, allowErrorCode, useGlobalSignatures)

    override fun maybeWrapWithBuiltInAndInit(
        moduleDescriptor: ModuleDescriptor,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer {
        return if (isBuiltInModule(moduleDescriptor)) {
            if (useGlobalSignatures) {
                IrIcModuleDeserializerWithBuiltIns(builtIns, functionalInterfaceFactory, moduleDeserializer)
            } else {
                IrModuleDeserializerWithBuiltIns(builtIns, functionalInterfaceFactory, moduleDeserializer)
            }
        } else moduleDeserializer
    }

    override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer {
        val currentModuleDeserializer = super.createCurrentModuleDeserializer(moduleFragment, dependencies)
        icData?.let {
            return CurrentModuleWithICDeserializer(currentModuleDeserializer, symbolTable, builtIns, it.icData) { lib ->
                JsModuleDeserializer(currentModuleDeserializer.moduleDescriptor, lib, currentModuleDeserializer.strategy, it.containsErrorCode)
            }
        }
        return currentModuleDeserializer
    }

    val modules
        get() = deserializersForModules.values
            .map { it.moduleFragment }
            .filter { it.descriptor !== currentModule }


    fun moduleDeserializer(moduleDescriptor: ModuleDescriptor): IrModuleDeserializer {
        return deserializersForModules[moduleDescriptor] ?: error("Deserializer for $moduleDescriptor not found")
    }

    fun loadIcIr(preprocess: (IrModuleFragment) -> Unit) {
        deserializersForModules.values.forEach {
            if (it.moduleDescriptor in loweredIcData) {
                preprocess(it.moduleFragment)
            }
            it.postProcess()
        }
    }

    class JsFePluginContext(
        override val moduleDescriptor: ModuleDescriptor,
        override val symbolTable: ReferenceSymbolTable,
        override val typeTranslator: TypeTranslator,
        override val irBuiltIns: IrBuiltIns,
    ) : TranslationPluginContext
}
