/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Indicates methods and properties that are not available in backend after K2 compiler release
 *
 * Invocation of such methods in IR plugins if frontend was K2 results in compiler crash.
 * It's still possible to use them in IR plugins with old frontend.
 */
@RequiresOptIn("This API is deprecated. It will be removed after the release of K2 compiler")
annotation class FirIncompatiblePluginAPI(val hint: String = "")

interface IrPluginContext : IrGeneratorContext {
    val languageVersionSettings: LanguageVersionSettings

    /**
     * Indicates that the plugin works after FIR. Effectively it means that all descriptor-based API may contain incorrect and/or incomplete information, and declarations marked with `@FirIncompatibleApi` will throw runtime exceptions.
     */
    val afterK2: Boolean

    @ObsoleteDescriptorBasedAPI
    val moduleDescriptor: ModuleDescriptor

    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    val bindingContext: BindingContext

    val symbolTable: ReferenceSymbolTable

    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    val typeTranslator: TypeTranslator

    val symbols: BuiltinSymbolsBase

    val platform: TargetPlatform?

    /**
     * Returns a logger instance to post diagnostic messages from plugin
     *
     * @param pluginId the unique plugin ID to make it easy to distinguish in log
     * @return         the logger associated with specified ID
     */
    fun createDiagnosticReporter(pluginId: String): IrMessageLogger

    // The following API is experimental
    @FirIncompatiblePluginAPI("Use classId overload instead")
    fun referenceClass(fqName: FqName): IrClassSymbol?
    @FirIncompatiblePluginAPI("Use classId overload instead")
    fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol?
    @FirIncompatiblePluginAPI("Use classId overload instead")
    fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol>
    @FirIncompatiblePluginAPI("Use callableId overload instead")
    fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol>
    @FirIncompatiblePluginAPI("Use callableId overload instead")
    fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol>

    // This one is experimental too
    fun referenceClass(classId: ClassId): IrClassSymbol?
    fun referenceTypeAlias(classId: ClassId): IrTypeAliasSymbol?
    fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol>
    fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>
    fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol>

    // temporary solution to load synthetic top-level declaration
    fun referenceTopLevel(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleDescriptor: ModuleDescriptor): IrSymbol?
}
