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
private const val OLD_REFERENCE_API_DEPRECATION_MESSAGE = "Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead."

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
     * Creates a [DeclarationFinder] that can be used to reference declarations which are
     * "builtin" for the particular compiler plugin. It could be either kotlin builtin declarations
     * from stdlib or declarations from the library which is required for plugin to work
     * (like `kotlinx.serialization.Serializable` annotation for kotlinx.serialization plugin).
     *
     * Reference to a declaration will be recorded to the incremental compilation as if this
     *   declaration was referenced from all compiled files.
     */
    fun finderForBuiltins(): DeclarationFinder

    /**
     * Creates a [DeclarationFinder] that can be used to reference declarations from compiled sources.
     * Reference to a declaration will be recorded to the incremental compilation
     *   as reference from [fromFile] file
     */
    fun finderForSource(fromFile: IrFile): DeclarationFinder

    // ------------------------------------ Reference API (IC incompatible) ------------------------------------

    /**
     * Returns a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns its expansion.
     *
     * If you need to access a not-expanded typealias, use [referenceClassifier] instead.
     */
    @Deprecated(level = DeprecationLevel.WARNING, message = OLD_REFERENCE_API_DEPRECATION_MESSAGE)
    fun referenceClass(classId: ClassId): IrClassSymbol?

    /**
     * Returns a class or typealias associated with given [classId].
     */
    @Deprecated(level = DeprecationLevel.WARNING, message = OLD_REFERENCE_API_DEPRECATION_MESSAGE)
    fun referenceClassifier(classId: ClassId): IrSymbol?

    /**
     * Returns constructors of a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns constructors of its expansion.
     */
    @Deprecated(level = DeprecationLevel.WARNING, message = OLD_REFERENCE_API_DEPRECATION_MESSAGE)
    fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol>

    /**
     * Returns functions with given [callableId].
     */
    @Deprecated(level = DeprecationLevel.WARNING, message = OLD_REFERENCE_API_DEPRECATION_MESSAGE)
    fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>

    /**
     * Returns properties with given [callableId].
     */
    @Deprecated(level = DeprecationLevel.WARNING, message = OLD_REFERENCE_API_DEPRECATION_MESSAGE)
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
}

interface DeclarationFinder {
    /**
     * Returns a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns its expansion.
     *
     * If you need to access a not-expanded typealias, use [findClassifier] instead.
     */
    fun findClass(classId: ClassId): IrClassSymbol?

    /**
     * Returns a class or typealias associated with given [classId].
     */
    fun findClassifier(classId: ClassId): IrSymbol?

    /**
     * Returns constructors of a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns constructors of its expansion.
     */
    fun findConstructors(classId: ClassId): Collection<IrConstructorSymbol>

    /**
     * Returns functions with given [callableId].
     */
    fun findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol>

    /**
     * Returns properties with given [callableId].
     */
    fun findProperties(callableId: CallableId): Collection<IrPropertySymbol>
}
