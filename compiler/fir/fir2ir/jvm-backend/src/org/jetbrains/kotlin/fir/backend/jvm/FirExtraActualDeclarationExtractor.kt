/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrDeclarationStorage
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.FirJvmActualizingBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.isJvm

private val actualizeByJvmBuiltinProviderFqName: FqName = StandardClassIds.Annotations.ActualizeByJvmBuiltinProvider.asSingleFqName()
private val implicitlyActualizedByJvmDeclaration: FqName = StandardClassIds.Annotations.ImplicitlyActualizedByJvmDeclaration.asSingleFqName()

/*
 * - Extract actual top-level declarations from the builtin symbol provider for expect declarations marked with
 *   `ActualizeByJvmBuiltinProvider` annotation
 * - Extract actual top-level declarations from the builtin symbol provider for expect declarations marked with
 *   `ImplicitlyActualizedByJvmDeclaration` annotation
 */
class FirExtraActualDeclarationExtractor private constructor(
    private val session: FirSession,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val declarationStorage: Fir2IrDeclarationStorage,
) : IrExtraActualDeclarationExtractor() {
    private val builtinProvider: FirBuiltinSymbolProvider? =
        when (session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            true -> (session.symbolProvider as FirCachingCompositeSymbolProvider)
                .providers.filterIsInstance<FirJvmActualizingBuiltinSymbolProvider>().single().builtinSymbolProvider
            false -> null
        }

    init {
        check(session.moduleData.platform.isJvm()) { "${this::class.simpleName} is only relevant for JVM" }
    }

    constructor(platformComponents: Fir2IrComponents) : this(
        platformComponents.session,
        platformComponents.classifierStorage,
        platformComponents.declarationStorage
    )

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        if (expectIrClass.hasActualizeByJvmBuiltinProviderFqNameAnnotation()) {
            val regularClassSymbol = builtinProvider?.getRegularClassSymbolByClassId(expectIrClass.classIdOrFail) ?: return null
            return classifierStorage.getIrClassSymbol(regularClassSymbol)
        }
        if (expectIrClass.annotations.any { it.isAnnotation(implicitlyActualizedByJvmDeclaration) }) {
            val actualDeclaration = session.symbolProvider.getRegularClassSymbolByClassId(expectIrClass.classIdOrFail)
                ?.takeIf { !it.isExpect }
                ?: return null
            return classifierStorage.getIrClassSymbol(actualDeclaration)
        }
        return null
    }

    private fun IrClass.hasActualizeByJvmBuiltinProviderFqNameAnnotation(): Boolean {
        if (annotations.any { it.isAnnotation(actualizeByJvmBuiltinProviderFqName) }) return true
        return parentClassOrNull?.hasActualizeByJvmBuiltinProviderFqNameAnnotation() == true
    }

    override fun extract(expectTopLevelCallables: List<IrDeclarationWithName>, expectCallableId: CallableId): List<IrSymbol> {
        require(expectTopLevelCallables.all { it.isTopLevel })

        if (expectTopLevelCallables.none { expectCallable ->
                expectCallable.annotations.any { it.isAnnotation(actualizeByJvmBuiltinProviderFqName) }
            }
        ) {
            return emptyList()
        }

        return builtinProvider?.getTopLevelCallableSymbols(expectCallableId.packageName, expectCallableId.callableName).orEmpty()
            .mapNotNull {
                when (it) {
                    is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(it)
                    is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(it)
                    else -> null
                }
            }
    }
}
