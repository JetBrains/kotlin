/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext

interface IrPluginContext : IrGeneratorContext {
    val languageVersionSettings: LanguageVersionSettings

    @ObsoleteDescriptorBasedAPI
    val moduleDescriptor: ModuleDescriptor

    @ObsoleteDescriptorBasedAPI
    val bindingContext: BindingContext

    @ObsoleteDescriptorBasedAPI
    val symbolTable: ReferenceSymbolTable

    @ObsoleteDescriptorBasedAPI
    val typeTranslator: TypeTranslator

    val platform: TargetPlatform?

    /**
     * Returns a logger instance to post diagnostic messages from plugin
     *
     * @param pluginId the unique plugin ID to make it easy to distinguish in log
     * @return         the logger associated with specified ID
     */
    fun createDiagnosticReporter(pluginId: String): IrMessageLogger

    // The following API is experimental
    fun referenceClass(fqName: FqName): IrClassSymbol?
    fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol?
    fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol>
    fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol>
    fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol>

    // temporary solution to load synthetic top-level declaration
    fun referenceTopLevel(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleDescriptor: ModuleDescriptor): IrSymbol?
}
