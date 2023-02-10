/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir.onAirGetNonLocalContainingOrThisDeclarationFor
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FileTowerProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.project.structure.KtCodeFragmentModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider.Companion.getModule
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.DelegatedMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * This workaround over [Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded] to convert cross module FIR to IR.
 * Ideally we should maintain symbol table only, but at current stage we have to fill storages with dependent constructions.
 */
@OptIn(SymbolInternals::class)
private fun createModuleFragmentWithSignaturesIfNeededWorkaround(
    session: FirSession,
    scopeSession: ScopeSession,
    firFiles: List<FirFile>,
    fir2IrConfiguration: Fir2IrConfiguration,
    fir2IrExtensions: Fir2IrExtensions,
    irMangler: KotlinMangler.IrMangler,
    irFactory: IrFactory,
    visibilityConverter: Fir2IrVisibilityConverter,
    specialSymbolProvider: Fir2IrSpecialSymbolProvider,
    irGenerationExtensions: Collection<IrGenerationExtension>,
    kotlinBuiltIns: KotlinBuiltIns,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    initializedIrBuiltIns: IrBuiltInsOverFir?,
): Fir2IrResult {
    val moduleDescriptor = FirModuleDescriptor(session, kotlinBuiltIns)
    val components = Fir2IrComponentsStorage(
        session,
        scopeSession,
        commonMemberStorage.symbolTable,
        irFactory,
        commonMemberStorage.firSignatureComposer,
        fir2IrExtensions,
        fir2IrConfiguration
    )
    val converter = Fir2IrConverter(moduleDescriptor, components)

    components.converter = converter
    components.classifierStorage = Fir2IrClassifierStorage(components, commonMemberStorage)
    components.delegatedMemberGenerator = DelegatedMemberGenerator(components)
    components.declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor, commonMemberStorage)

    components.visibilityConverter = visibilityConverter
    components.typeConverter = Fir2IrTypeConverter(components)
    val irBuiltIns = initializedIrBuiltIns ?: IrBuiltInsOverFir(
        components, fir2IrConfiguration.languageVersionSettings, moduleDescriptor, irMangler,
        true
    )
    components.irBuiltIns = irBuiltIns
    val conversionScope = Fir2IrConversionScope()
    val fir2irVisitor = Fir2IrVisitor(components, conversionScope)
    components.builtIns = Fir2IrBuiltIns(components, specialSymbolProvider)
    components.annotationGenerator = AnnotationGenerator(components)
    components.fakeOverrideGenerator = FakeOverrideGenerator(components, conversionScope)
    components.callGenerator = CallAndReferenceGenerator(components, fir2irVisitor, conversionScope)
    components.irProviders = listOf(FirIrProvider(components))


    val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)

    val allFirFiles = buildList {
        addAll(firFiles)
        addAll(session.createFilesWithGeneratedDeclarations())
    }

    firFiles.first().accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            val symbol =
                (resolvedTypeRef.type as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session) ?: return
            val firClass = symbol.fir as? FirRegularClass ?: return super.visitResolvedTypeRef(resolvedTypeRef)
            if (components.classifierStorage.getCachedIrClass(firClass) != null)
                return
            val sig = IdSignature.CommonSignature(firClass.symbol.classId.asFqNameString(), firClass.name.asString(), 0, 0)
            val irClass =
                components.classifierStorage.registerIrClass(
                    firClass,
                    IrExternalPackageFragmentImpl(
                        IrExternalPackageFragmentSymbolImpl(
                            FirPackageFragmentDescriptor(
                                firClass.symbol.classId.packageFqName,
                                FirModuleDescriptor(session, DefaultBuiltIns.Instance)
                            )
                        ), firClass.symbol.classId.packageFqName
                    ),
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                ).also {
                    it.thisReceiver = buildValueParameter(it) {
                        name = Name.identifier("\$this")
                        type = IrSimpleTypeImpl(it.symbol, false, emptyList(), emptyList())
                    }
                }
            components.classifierStorage.processClassHeader(firClass)
            val irSymbol = irClass.symbol

            commonMemberStorage.symbolTable.declareClass(
                sig,
                { irSymbol }) {
                irClass
            }
            super.visitResolvedTypeRef(resolvedTypeRef)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            val symbol = functionCall.calleeReference.toResolvedCallableSymbol() as FirCallableSymbol<*>
            (symbol.fir.containerSource as? JvmPackagePartSource)?.facadeClassName
            if (symbol.fir is FirConstructor) {
                val firClass = symbol.fir.getContainingClass(session) ?: TODO()
                val irClass = components.classifierStorage.getCachedIrClass(firClass) ?: TODO()
                components.declarationStorage.getCachedIrConstructor(symbol.fir as FirConstructor)
                    ?: components.declarationStorage.createIrConstructor(symbol.fir as FirConstructor, irClass)
            }
            super.visitFunctionCall(functionCall)
        }

    })
    fir2IrExtensions.registerDeclarations(commonMemberStorage.symbolTable)
    converter.runSourcesConversion(
        allFirFiles, irModuleFragment, irGenerationExtensions, fir2irVisitor, fir2IrExtensions,
        false
    )
    return Fir2IrResult(irModuleFragment, components, moduleDescriptor)
}

data class Kt2IrResult(
    val irModuleFragment: IrModuleFragment,
    val symbolTable: SymbolTable,
    val irProviders: List<IrProvider>,
    val jvmBackendExtension: JvmBackendExtension,
    val generatorExtensions: JvmGeneratorExtensions,
    val backendClassResolver: JvmBackendClassResolver,
    val irPluginContext: IrPluginContext,
)

fun compileKt2Ir(
    codeFragment: KtElement,
    dianosticErrorProcessing: (Map.Entry<String?, List<KtDiagnostic>>) -> Unit,
): Kt2IrResult {
    val codeFragmentModule = getModule(codeFragment.project, codeFragment, null) as KtCodeFragmentModule
    val session = codeFragmentModule.getFirResolveSession(codeFragment.project)
    val firFile = codeFragment.getOrBuildFir(session)!! as FirFile

   val (scope, firFiles) = session.useSiteFirSession.runResolution(
        listOf(firFile)
    )
    val diagnosticReporter = SimpleDiagnosticsCollectorWithSuppress()
    session.useSiteFirSession.runCheckers(
        scope,
        listOf(firFile),
        diagnosticReporter
    )
    if (diagnosticReporter.hasErrors) {
        diagnosticReporter.diagnosticsByFilePath.forEach(dianosticErrorProcessing)
    }

    val irGenerationExtension = IrGenerationExtension.getInstances(codeFragment.project)

    val languageVersionSettings = codeFragmentModule.languageVersionSettings
    val compilerConfiguration = CompilerConfiguration().apply {
        this.languageVersionSettings = languageVersionSettings
    }
    val extensions = JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl(), JvmIrMangler)

    val fir2irResult = createModuleFragmentWithSignaturesIfNeededWorkaround(
        session.useSiteFirSession,
        scope, firFiles,
        Fir2IrConfiguration(languageVersionSettings, false, EvaluatedConstTracker.create()),
        extensions,
        JvmIrMangler,
        IrFactoryImpl,
        FirJvmVisibilityConverter,
        Fir2IrJvmSpecialAnnotationSymbolProvider(),
        irGenerationExtension,
        DefaultBuiltIns.Instance,
        Fir2IrCommonMemberStorage(
            signatureComposerForJvmFir2Ir(false),
            FirJvmKotlinMangler()
        ),
        null
    )
    return Kt2IrResult(
        fir2irResult.irModuleFragment,
        fir2irResult.components.symbolTable,
        fir2irResult.components.irProviders,
        FirJvmBackendExtension(fir2irResult.components, null),
        JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl(), JvmIrMangler),
        FirJvmBackendClassResolver(fir2irResult.components),
        fir2irResult.pluginContext
    )
}
