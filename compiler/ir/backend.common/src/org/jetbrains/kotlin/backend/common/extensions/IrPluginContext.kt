/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
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

    // ------------------------------------ Reference API (IC compatible) ------------------------------------

    /**
     * Returns a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns its expansion.
     *
     * If you need to access a not-expanded typealias, use [referenceClassifier] instead.
     *
     * @param fromFile the file from which the reference is made.
     *   This information is needed for proper reference collecting for incremental compilation.
     */
    fun referenceClass(classId: ClassId, fromFile: IrFile): IrClassSymbol?

    /**
     * Returns a class or typealias associated with given [classId].
     *
     * @param fromFile the file from which the reference is made.
     *   This information is needed for proper reference collecting for incremental compilation.
     */
    fun referenceClassifier(classId: ClassId, fromFile: IrFile): IrSymbol?

    /**
     * Returns constructors of a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns constructors of its expansion.
     *
     * @param fromFile the file from which the reference is made.
     *   This information is needed for proper reference collecting for incremental compilation.
     */
    fun referenceConstructors(classId: ClassId, fromFile: IrFile): Collection<IrConstructorSymbol>

    /**
     * Returns functions with given [callableId].
     *
     * @param fromFile the file from which the reference is made.
     *   This information is needed for proper reference collecting for incremental compilation.
     */
    fun referenceFunctions(callableId: CallableId, fromFile: IrFile): Collection<IrSimpleFunctionSymbol>

    /**
     * Returns properties with given [callableId].
     *
     * @param fromFile the file from which the reference is made.
     *   This information is needed for proper reference collecting for incremental compilation.
     */
    fun referenceProperties(callableId: CallableId, fromFile: IrFile): Collection<IrPropertySymbol>

    // ------------------------------------ Reference API (IC incompatible) ------------------------------------

    /**
     * Returns a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns its expansion.
     *
     * If you need to access a not-expanded typealias, use [referenceClassifier] instead.
     */
    @LookupWithoutUseSiteFile
    fun referenceClass(classId: ClassId): IrClassSymbol?

    /**
     * Returns a class or typealias associated with given [classId].
     */
    @LookupWithoutUseSiteFile
    fun referenceClassifier(classId: ClassId): IrSymbol?

    /**
     * Returns constructors of a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns constructors of its expansion.
     */
    @LookupWithoutUseSiteFile
    fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol>

    /**
     * Returns functions with given [callableId].
     */
    @LookupWithoutUseSiteFile
    fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>

    /**
     * Returns properties with given [callableId].
     */
    @LookupWithoutUseSiteFile
    fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol>

    // ------------------------------------ IC API ------------------------------------

    /**
     * Records information that [declaration] was referenced during modification of file [fromFile].
     * This information later will be used by the Incremental compilation to correctly invalidate
     * compiled files on source changes.
     */
    fun recordLookup(declaration: IrDeclarationWithName, fromFile: IrFile)

    // ------------------------------------ Deprecated API ------------------------------------

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

    // ------------------------------------ Opt-ins ------------------------------------

    /**
     * Marks declaration reference API, which automatically record lookups for incremental compilation for all the files.
     *
     * The acceptable use-case for them is to reference some builtin Kotlin declarations or statically known declarations
     * from libraries that are required for the plugin to work.
     */
    // TODO: uncomment when official plugins are migrated (KT-82341)
    // @RequiresOptIn("This API doesn't automatically records lookups for incremental compilation. Please use it with caution.")
    annotation class LookupWithoutUseSiteFile
}
