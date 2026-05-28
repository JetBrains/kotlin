/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.getJavaClassLikeSymbolByClassId
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.name.CallableId

class FirDirectJavaActualDeclarationExtractor private constructor(
    private val session: FirSession,
    private val classifierStorage: Fir2IrClassifierStorage,
) : IrExtraActualDeclarationExtractor() {

    companion object {
        fun initializeIfNeeded(platformComponents: Fir2IrComponents): FirDirectJavaActualDeclarationExtractor? {
            val session = platformComponents.session
            // `javaSymbolProvider` is the historical "is this a JVM session" tell. It survives Stage 2 — only the binary
            // half of its lookups moves into `JvmClassFileBasedSymbolProvider` (see
            // compiler/java-direct/implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md §6.2/§6.3) — so we keep the gate here.
            if (session.javaSymbolProvider != null &&
                session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
                session.languageVersionSettings.supportsFeature(LanguageFeature.DirectJavaActualization)
            ) {
                return FirDirectJavaActualDeclarationExtractor(session, platformComponents.classifierStorage)
            }
            return null
        }
    }

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        // Stage 2 §6.1 indirect-caller audit: go through the Java-targeted lookup helper instead of
        // `JavaSymbolProvider` directly. The strict `Java.Source` filter is kept (only Java source-class actualizations
        // are valid here — binary Java classes are not actualization candidates).
        val javaActualDeclaration = session.getJavaClassLikeSymbolByClassId(expectIrClass.classIdOrFail)
            ?.takeIf { it.origin is FirDeclarationOrigin.Java.Source }
        if (javaActualDeclaration != null) {
            return classifierStorage.getIrClassSymbol(javaActualDeclaration)
        }
        return null
    }

    override fun extract(
        expectTopLevelCallables: List<IrDeclarationWithName>,
        expectCallableId: CallableId,
    ): List<IrSymbol> = emptyList()
}
