/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
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

private const val K1_DEPRECATION_MESSAGE = "This API is deprecated. It will be removed after the 2.3 release"

interface IrPluginContext : IrGeneratorContext {
    val languageVersionSettings: LanguageVersionSettings

    /**
     * Indicates that the plugin works after FIR. Effectively it means that all descriptor-based API may contain incorrect and/or incomplete information, and declarations marked with `@FirIncompatibleApi` will throw runtime exceptions.
     */
    val afterK2: Boolean

    @Deprecated("This API is deprecated. Use `irBuiltIns` instead.", level = DeprecationLevel.ERROR)
    val symbols: Symbols

    val platform: TargetPlatform?

    /**
     * Returns a diagnostic reporter instance to report IR diagnostics from plugin
     */
    val diagnosticReporter: IrDiagnosticReporter

    /**
     * Use this service to:
     * - add annotations to declarations if those annotations should be saved into metadata
     * - register that declaration generated at IR stage will appear in compiled metadata
     * This service properly works only in K2 compiler
     */
    val metadataDeclarationRegistrar: IrGeneratedDeclarationsRegistrar

    fun referenceClass(classId: ClassId): IrClassSymbol?
    fun referenceTypeAlias(classId: ClassId): IrTypeAliasSymbol?
    fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol>
    fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>
    fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol>

    // ------------------------------------ IC API ------------------------------------

    /**
     * Records information that [declaration] was referenced during modification of file [fromFile].
     * This information later will be used by the Incremental compilation to correctly invalidate
     * compiled files on source changes.
     */
    fun recordLookup(declaration: IrDeclarationWithName, fromFile: IrFile)

    // ------------------------------------ Deprecated API ------------------------------------

    @Deprecated("Use diagnosticReporter instead", level = DeprecationLevel.ERROR)
    fun createDiagnosticReporter(pluginId: String): MessageCollector

    /**
     * Returns a message collector instance to report generic diagnostic messages from plugin
     */
    @Deprecated(
        "Consider using diagnosticReporter instead. See https://youtrack.jetbrains.com/issue/KT-78277 for more details",
        level = DeprecationLevel.WARNING
    )
    val messageCollector: MessageCollector

    @ObsoleteDescriptorBasedAPI
    val symbolTable: ReferenceSymbolTable

    @ObsoleteDescriptorBasedAPI
    val moduleDescriptor: ModuleDescriptor

    // ------------------------------------ K2-incompatible API ------------------------------------

    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    val bindingContext: BindingContext

    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    val typeTranslator: TypeTranslator

    // The following API is experimental
    @FirIncompatiblePluginAPI("Use classId overload instead")
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceClass(fqName: FqName): IrClassSymbol?

    @FirIncompatiblePluginAPI("Use classId overload instead")
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol?

    @FirIncompatiblePluginAPI("Use classId overload instead")
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol>

    @FirIncompatiblePluginAPI("Use callableId overload instead")
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol>

    @FirIncompatiblePluginAPI("Use callableId overload instead")
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol>

    // temporary solution to load synthetic top-level declaration
    @FirIncompatiblePluginAPI
    @Deprecated(K1_DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun referenceTopLevel(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleDescriptor: ModuleDescriptor): IrSymbol?
}
