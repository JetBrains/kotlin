/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclarationOfType
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KtFirSymbolProvider(
    override val analysisSession: KtAnalysisSession,
    firSymbolProvider: FirSymbolProvider,
    private val resolveState: FirModuleResolveState,
    private val firSymbolBuilder: KtSymbolByFirBuilder,
    override val token: ValidityToken,
) : KtSymbolProvider(), ValidityTokenOwner {
    private val firSymbolProvider by weakRef(firSymbolProvider)

    override fun getParameterSymbol(psi: KtParameter): KtParameterSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirValueParameter, KtParameterSymbol>(resolveState) {
            firSymbolBuilder.buildParameterSymbol(it)
        }
    }

    override fun getFunctionSymbol(psi: KtNamedFunction): KtFunctionSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirSimpleFunction, KtFunctionSymbol>(resolveState) {
            firSymbolBuilder.buildFunctionSymbol(it)
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirConstructor, KtConstructorSymbol>(resolveState) {
            firSymbolBuilder.buildConstructorSymbol(it)
        }
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirTypeParameter, KtTypeParameterSymbol>(resolveState) {
            firSymbolBuilder.buildTypeParameterSymbol(it)
        }
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirTypeAlias, KtTypeAliasSymbol>(resolveState) {
            firSymbolBuilder.buildTypeAliasSymbol(it)
        }
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirEnumEntry, KtEnumEntrySymbol>(resolveState) {
            firSymbolBuilder.buildEnumEntrySymbol(it)
        }
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirSimpleFunction, KtFunctionSymbol>(resolveState) {
            firSymbolBuilder.buildFunctionSymbol(it)
        }
        firSymbolBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getAnonymousFunctionSymbol(psi: KtLambdaExpression): KtAnonymousFunctionSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirAnonymousFunction, KtAnonymousFunctionSymbol>(resolveState) {
            firSymbolBuilder.buildAnonymousFunctionSymbol(it)
        }
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirProperty, KtVariableSymbol>(resolveState) {
            firSymbolBuilder.buildVariableSymbol(it)
        }
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol = withValidityAssertion {
        psi.objectDeclaration.withFirDeclarationOfType<FirAnonymousObject, KtAnonymousObjectSymbol>(resolveState) {
            firSymbolBuilder.buildAnonymousObjectSymbol(it)
        }
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        check(psi !is KtObjectDeclaration || psi.parent !is KtObjectLiteralExpression)
        psi.withFirDeclarationOfType<FirRegularClass, KtClassOrObjectSymbol>(resolveState) {
            firSymbolBuilder.buildClassSymbol(it)
        }
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirPropertyAccessor, KtPropertyAccessorSymbol>(resolveState) {
            firSymbolBuilder.buildPropertyAccessorSymbol(it)
        }
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return null
        firSymbolBuilder.buildClassSymbol(symbol.fir)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol -> firSymbolBuilder.buildSymbol(firSymbol.fir) }
    }
}