/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * This class has the same functionality as `FirBuiltinSyntheticFunctionInterfaceProvider` except it also puts generated classes into a list.
 * The list is used later on FIR2IR stage for creating regular IR classes instead of lazy ones.
 * It allows avoiding problems related to lazy classes actualization and fake-overrides building
 * because FIR2IR treats those declarations as declarations in a virtual file despite the fact they are generated ones.
 * The provider is only used for the first common source-set with enabled `stdlibCompilation` mode.
 */
class FirStdlibBuiltinSyntheticFunctionInterfaceProvider private constructor(
    session: FirSession, moduleData: FirModuleData, kotlinScopeProvider: FirKotlinScopeProvider,
) : FirBuiltinSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider) {
    companion object {
        fun initializeIfNeeded(
            session: FirSession,
            moduleData: FirModuleData,
            kotlinScopeProvider: FirKotlinScopeProvider,
        ): FirStdlibBuiltinSyntheticFunctionInterfaceProvider? {
            // Check `dependsOnDependencies` to avoid initializing an extra `FirStdlibBuiltinSyntheticFunctionInterfaceProvider`
            // It's relevant during compiling stdlib with HMPP (Wasm, Native)
            return runIf(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon &&
                        moduleData.dependsOnDependencies.isEmpty()
            ) {
                FirStdlibBuiltinSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider)
            }
        }
    }

    private val generatedClassesList = mutableListOf<FirRegularClass>()
    val generatedClasses: List<FirRegularClass>
        get() = generatedClassesList

    override fun createSyntheticFunctionInterface(classId: ClassId, kind: FunctionTypeKind): FirRegularClassSymbol? {
        return super.createSyntheticFunctionInterface(classId, kind)?.also { generatedClassesList.add(it.fir) }
    }
}