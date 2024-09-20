/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
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
    private val javaSymbolProvider: JavaSymbolProvider,
    private val classifierStorage: Fir2IrClassifierStorage,
) : IrExtraActualDeclarationExtractor() {

    companion object {
        fun initializeIfNeeded(platformComponents: Fir2IrComponents): FirDirectJavaActualDeclarationExtractor? {
            val javaSymbolProvider = platformComponents.session.javaSymbolProvider
            if (javaSymbolProvider != null &&
                platformComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
                platformComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.DirectJavaActualization)
            ) {
                return FirDirectJavaActualDeclarationExtractor(javaSymbolProvider, platformComponents.classifierStorage)
            }
            return null
        }
    }

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        val javaActualDeclaration = javaSymbolProvider.getClassLikeSymbolByClassId(expectIrClass.classIdOrFail)
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
