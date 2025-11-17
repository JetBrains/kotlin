/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.recordFqNameLookup
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class Fir2IrPluginContext(
    private val c: Fir2IrComponents,
    override val irBuiltIns: IrBuiltIns,
    @property:ObsoleteDescriptorBasedAPI override val moduleDescriptor: ModuleDescriptor,
    @property:ObsoleteDescriptorBasedAPI override val symbolTable: ReferenceSymbolTable,
    @property:Deprecated(
        "Consider using diagnosticReporter instead. See https://youtrack.jetbrains.com/issue/KT-78277 for more details",
        level = DeprecationLevel.WARNING
    )
    override val messageCollector: MessageCollector,
    diagnosticReporter: DiagnosticReporter,
) : IrPluginContext {
    companion object {
        private const val ERROR_MESSAGE = "This API is not supported for K2"
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    override val bindingContext: BindingContext
        get() = error(ERROR_MESSAGE)

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @ObsoleteDescriptorBasedAPI
    @FirIncompatiblePluginAPI
    override val typeTranslator: TypeTranslator
        get() = error(ERROR_MESSAGE)

    override val afterK2: Boolean = true

    override val languageVersionSettings: LanguageVersionSettings
        get() = c.session.languageVersionSettings

    override val platform: TargetPlatform
        get() = c.session.moduleData.platform

    private val symbolProvider: FirSymbolProvider
        get() = c.session.symbolProvider

    override val metadataDeclarationRegistrar: Fir2IrIrGeneratedDeclarationsRegistrar
        get() = c.annotationsFromPluginRegistrar

    private val lookupsWithoutSpecificFile = mutableSetOf<FqName>()

    @IrPluginContext.LookupWithoutUseSiteFile
    override fun referenceClass(classId: ClassId): IrClassSymbol? {
        return referenceClassImpl(classId, fromFile = null)
    }

    override fun referenceClass(classId: ClassId, fromFile: IrFile): IrClassSymbol? {
        return referenceClassImpl(classId, fromFile)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun referenceClassImpl(classId: ClassId, fromFile: IrFile?): IrClassSymbol? {
        var classIdToSearch = classId
        while (true) {
            recordLookup(classIdToSearch.asSingleFqName(), fromFile)
            val firSymbol = symbolProvider.getClassLikeSymbolByClassId(classIdToSearch) ?: return null
            when (firSymbol) {
                is FirRegularClassSymbol -> return c.classifierStorage.getIrClassSymbol(firSymbol)
                is FirTypeAliasSymbol -> {
                    classIdToSearch = firSymbol.resolvedExpandedTypeRef.coneType.classId ?: return null
                }
                is FirAnonymousObjectSymbol -> shouldNotBeCalled()
            }
        }
    }

    @IrPluginContext.LookupWithoutUseSiteFile
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun referenceClassifier(classId: ClassId): IrSymbol? {
        return referenceClassifierImpl(classId, fromFile = null)
    }

    override fun referenceClassifier(classId: ClassId, fromFile: IrFile): IrSymbol? {
        return referenceClassifierImpl(classId, fromFile)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun referenceClassifierImpl(classId: ClassId, fromFile: IrFile?): IrSymbol? {
        recordLookup(classId.asSingleFqName(), fromFile)
        val firSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return when (firSymbol) {
            is FirRegularClassSymbol -> c.classifierStorage.getIrClassSymbol(firSymbol)
            is FirTypeAliasSymbol -> c.classifierStorage.getIrTypeAliasSymbol(firSymbol)
            is FirAnonymousObjectSymbol -> shouldNotBeCalled()
        }
    }

    @IrPluginContext.LookupWithoutUseSiteFile
    override fun referenceConstructors(classId: ClassId): Collection<IrConstructorSymbol> {
        return referenceConstructorsImpl(classId, fromFile = null)
    }

    override fun referenceConstructors(classId: ClassId, fromFile: IrFile): Collection<IrConstructorSymbol> {
        return referenceConstructorsImpl(classId, fromFile)
    }

    @IrPluginContext.LookupWithoutUseSiteFile
    private fun referenceConstructorsImpl(classId: ClassId, fromFile: IrFile?): Collection<IrConstructorSymbol> {
        recordLookup(classId.asSingleFqName(), fromFile)
        return referenceCallableSymbols(
            classId,
            getCallablesFromScope = { getDeclaredConstructors() },
            getCallablesFromProvider = { shouldNotBeCalled() },
            Fir2IrDeclarationStorage::getIrConstructorSymbol
        )
    }

    @IrPluginContext.LookupWithoutUseSiteFile
    override fun referenceFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> {
        return referenceFunctionsImpl(callableId, fromFile = null)
    }

    override fun referenceFunctions(callableId: CallableId, fromFile: IrFile): Collection<IrSimpleFunctionSymbol> {
        return referenceFunctionsImpl(callableId, fromFile)
    }

    private fun referenceFunctionsImpl(callableId: CallableId, fromFile: IrFile?): Collection<IrSimpleFunctionSymbol> {
        recordLookup(callableId.asSingleFqName(), fromFile)
        return referenceCallableSymbols(
            callableId.classId,
            getCallablesFromScope = { getFunctions(callableId.callableName) },
            getCallablesFromProvider = { getTopLevelFunctionSymbols(callableId.packageName, callableId.callableName) },
            Fir2IrDeclarationStorage::getIrFunctionSymbol
        )
    }

    @IrPluginContext.LookupWithoutUseSiteFile
    override fun referenceProperties(callableId: CallableId): Collection<IrPropertySymbol> {
        return referencePropertiesImpl(callableId, fromFile = null)
    }

    override fun referenceProperties(callableId: CallableId, fromFile: IrFile): Collection<IrPropertySymbol> {
        return referencePropertiesImpl(callableId, fromFile)
    }

    private fun referencePropertiesImpl(callableId: CallableId, fromFile: IrFile?): Collection<IrPropertySymbol> {
        recordLookup(callableId.asSingleFqName(), fromFile)
        return referenceCallableSymbols(
            callableId.classId,
            getCallablesFromScope = { getProperties(callableId.callableName).filterIsInstance<FirPropertySymbol>() },
            getCallablesFromProvider = { getTopLevelPropertySymbols(callableId.packageName, callableId.callableName) },
            Fir2IrDeclarationStorage::getIrPropertySymbol
        )
    }

    private inline fun <F : FirCallableSymbol<*>, S : IrSymbol, reified R : S> referenceCallableSymbols(
        classId: ClassId?,
        getCallablesFromScope: FirTypeScope.() -> Collection<F>,
        getCallablesFromProvider: FirSymbolProvider.() -> Collection<F>,
        irExtractor: Fir2IrDeclarationStorage.(F) -> S?,
    ): Collection<R> {
        val callables = if (classId != null) {
            val expandedClass = symbolProvider.getClassLikeSymbolByClassId(classId)
                ?.fullyExpandedClass(c.session)
                ?: return emptyList()
            with(c) { expandedClass.unsubstitutedScope().getCallablesFromScope() }
        } else {
            symbolProvider.getCallablesFromProvider()
        }

        return callables.mapNotNull { c.declarationStorage.irExtractor(it) }.filterIsInstance<R>()
    }

    override fun recordLookup(declaration: IrDeclarationWithName, fromFile: IrFile) {
        recordLookup(declaration.fqNameWhenAvailable, fromFile)
    }

    private fun recordLookup(fqName: FqName?, fromFile: IrFile?) {
        if (fqName == null) return
        if (fromFile == null) {
            lookupsWithoutSpecificFile += fqName
        } else {
            val lookupTracker = c.session.lookupTracker ?: return
            lookupTracker.recordFqNameLookup(fqName, source = null, fileSource = fromFile.fileSource)
        }
    }

    fun recordLookupsWithoutSpecificFile(moduleFragment: IrModuleFragment) {
        val lookupTracker = c.session.lookupTracker ?: return
        for (file in moduleFragment.files) {
            val fileSource = file.fileSource ?: continue
            for (fqName in lookupsWithoutSpecificFile) {
                lookupTracker.recordFqNameLookup(fqName, source = null, fileSource = fileSource)
            }
        }
    }

    private val IrFile.fileSource: KtSourceElement?
        get() = (metadata as? FirMetadataSource.File)?.fir?.source

    override val diagnosticReporter: IrDiagnosticReporter =
        KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceClass(fqName: FqName): IrClassSymbol {
        error(ERROR_MESSAGE)
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol {
        error(ERROR_MESSAGE)
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
        error(ERROR_MESSAGE)
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
        error(ERROR_MESSAGE)
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
        error(ERROR_MESSAGE)
    }

    @Deprecated("This API is deprecated. It will be removed after the 2.3 release", level = DeprecationLevel.WARNING)
    @FirIncompatiblePluginAPI
    override fun referenceTopLevel(
        signature: IdSignature,
        kind: IrDeserializer.TopLevelSymbolKind,
        moduleDescriptor: ModuleDescriptor
    ): IrSymbol {
        error(ERROR_MESSAGE)
    }
}
