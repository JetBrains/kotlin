/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.analysis.api.symbols.classSymbol as classSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.findClass as findClassEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.findClassLike as findClassLikeEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.findPackage as findPackageEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.findTopLevelCallables as findTopLevelCallablesEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.findTypeAlias as findTypeAliasEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.namedClassSymbol as namedClassSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.rootPackageSymbol as rootPackageSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.symbol as symbolEndpoint

/**
 * Routes the legacy [KaSymbolProvider] surface through the new public `context(session: KaSession)` symbol endpoints, which in turn reach
 * the [KaInternalsSymbolProvider][org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolProvider] proxy.
 */
internal class KaSymbolProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaSymbolProvider {
    override val KtDeclaration.symbol: KaDeclarationSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtParameter.symbol: KaVariableSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtNamedFunction.symbol: KaFunctionSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtConstructor<*>.symbol: KaConstructorSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtTypeParameter.symbol: KaTypeParameterSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtTypeAlias.symbol: KaTypeAliasSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtEnumEntry.symbol: KaEnumEntrySymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtProperty.symbol: KaVariableSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtBackingField.symbol: KaBackingFieldSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtObjectDeclaration.symbol: KaClassSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtClassOrObject.classSymbol: KaClassSymbol?
        get() = context(analysisSession) { classSymbolEndpoint }

    override val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?
        get() = context(analysisSession) { namedClassSymbolEndpoint }

    override val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtClassInitializer.symbol: KaClassInitializerSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtFile.symbol: KaFileSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override val KtScript.symbol: KaScriptSymbol
        get() = context(analysisSession) { symbolEndpoint }

    @KaExperimentalApi
    override val KtContextReceiver.symbol: KaContextParameterSymbol
        get() = context(analysisSession) { symbolEndpoint }

    override fun findPackage(fqName: FqName): KaPackageSymbol? =
        context(analysisSession) { findPackageEndpoint(fqName) }

    override fun findClass(classId: ClassId): KaClassSymbol? =
        context(analysisSession) { findClassEndpoint(classId) }

    override fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? =
        context(analysisSession) { findTypeAliasEndpoint(classId) }

    override fun findClassLike(classId: ClassId): KaClassLikeSymbol? =
        context(analysisSession) { findClassLikeEndpoint(classId) }

    override fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> =
        context(analysisSession) { findTopLevelCallablesEndpoint(packageFqName, name) }

    override val rootPackageSymbol: KaPackageSymbol
        get() = context(analysisSession) { rootPackageSymbolEndpoint }
}
