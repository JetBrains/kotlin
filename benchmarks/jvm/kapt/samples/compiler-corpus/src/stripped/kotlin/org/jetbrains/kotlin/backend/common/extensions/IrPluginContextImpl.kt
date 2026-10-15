/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.scopes.MemberScope

@Suppress("DEPRECATION")
open class IrPluginContextImpl(
    private val module: ModuleDescriptor,
    override val languageVersionSettings: LanguageVersionSettings,
    private val st: ReferenceSymbolTable,
    override val irBuiltIns: IrBuiltIns,
    val linker: IrProvider,
    diagnosticReporter: DiagnosticReporter = DiagnosticsCollectorImpl(),
) : IrPluginContext {
    override val afterK2: Boolean = false

    override val platform: TargetPlatform? = module.platform

    @ObsoleteDescriptorBasedAPI
    override val symbolTable: ReferenceSymbolTable = st

    final override val metadataDeclarationRegistrar: IrGeneratedDeclarationsRegistrar
        get() = DummyIrGeneratedDeclarationsRegistrar

    private fun resolveMemberScope(fqName: FqName): MemberScope? { TODO() }

    private fun <S : IrSymbol> resolveSymbol(fqName: FqName, referencer: (MemberScope) -> S?): S? { TODO() }

    override val diagnosticReporter: IrDiagnosticReporter =
        KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)

    private fun <S : IrSymbol> resolveSymbolCollection(fqName: FqName, referencer: (MemberScope) -> Collection<S>): Collection<S> { TODO() }

    private inner class Finder : DeclarationFinder {
        override fun findClass(classId: ClassId): IrClassSymbol? { TODO() }

        override fun findClassifier(classId: ClassId): IrSymbol? { TODO() }

        override fun findConstructors(classId: ClassId): Collection<IrConstructorSymbol> { TODO() }

        override fun findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> { TODO() }

        override fun findProperties(callableId: CallableId): Collection<IrPropertySymbol> { TODO() }
    }

    override fun finderForBuiltins(): DeclarationFinder { TODO() }

    override fun finderForSource(fromFile: IrFile): DeclarationFinder { TODO() }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun referenceClass(fqName: FqName): IrClassSymbol? { TODO() }

    private fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> { TODO() }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> { TODO() }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> { TODO() }

    @Deprecated("Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead.", level = DeprecationLevel.WARNING)
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun referenceClass(classId: ClassId): IrClassSymbol? { TODO() }

    @Deprecated("Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead.", level = DeprecationLevel.WARNING)
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun referenceClassifier(classId: ClassId): IrSymbol? { TODO() }

    @Deprecated("Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead.", level = DeprecationLevel.WARNING)
    override fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol> { TODO() }

    @Deprecated("Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead.", level = DeprecationLevel.WARNING)
    override fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> { TODO() }

    @Deprecated("Please use `finderForBuiltins()` or `finderForSource(fromFile)` instead.", level = DeprecationLevel.WARNING)
    override fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol> { TODO() }

    override fun recordLookup(declaration: IrDeclarationWithName, fromFile: IrFile) { TODO() }

    private object DummyIrGeneratedDeclarationsRegistrar : IrGeneratedDeclarationsRegistrar() {
        override fun getMetadataVisibleAnnotationsForElement(declaration: IrDeclaration): MutableList<IrAnnotation> = TODO()

        override fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, annotations: List<IrAnnotation>) { TODO() }

        override fun registerFunctionAsMetadataVisible(irFunction: IrSimpleFunction) { TODO() }

        override fun registerConstructorAsMetadataVisible(irConstructor: IrConstructor) { TODO() }

        override fun registerPropertyAsMetadataVisible(irProperty: IrProperty) { TODO() }

        override fun registerClassAsMetadataVisible(irClass: IrClass) { TODO() }

        override fun addCustomMetadataExtension(irDeclaration: IrDeclaration, pluginId: String, data: ByteArray) { TODO() }

        override fun getCustomMetadataExtension(irDeclaration: IrDeclaration, pluginId: String): ByteArray? = TODO()
    }
}
