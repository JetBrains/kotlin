/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KtFirSymbolProvider(
    override val analysisSession: KtAnalysisSession,
    firSymbolProvider: FirSymbolProvider,
    private val firResolveSession: LLFirResolveSession,
    private val firSymbolBuilder: KtSymbolByFirBuilder,
    override val token: KtLifetimeToken,
) : KtSymbolProvider(), KtLifetimeOwner {
    private val firSymbolProvider by weakRef(firSymbolProvider)

    override fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol = withValidityAssertion {
        when {
            psi.isFunctionTypeParameter -> error(
                "Creating KtValueParameterSymbol for function type parameter is not possible. Please see the KDoc of getParameterSymbol"
            )

            psi.isLoopParameter -> {
                firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(psi.resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession))
            }

            else -> {
                firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(
                    psi.resolveToFirSymbolOfType<FirValueParameterSymbol>(firResolveSession)
                )
            }
        }
    }

    override fun getFileSymbol(psi: KtFile): KtFileSymbol = withValidityAssertion {
        firSymbolBuilder.buildFileSymbol(psi.getOrBuildFirFile(firResolveSession).symbol)
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol = withValidityAssertion {
        when (val firSymbol = psi.resolveToFirSymbolOfType<FirFunctionSymbol<*>>(firResolveSession)) {
            is FirNamedFunctionSymbol -> {
                if (firSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                    firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(firSymbol)
                } else {
                    firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firSymbol)
                }
            }
            is FirAnonymousFunctionSymbol -> firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(firSymbol)
            else -> error("Unexpected ${firSymbol.fir.renderWithType()}")
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {

        firSymbolBuilder.functionLikeBuilder.buildConstructorSymbol(psi.resolveToFirSymbolOfType<FirConstructorSymbol>(firResolveSession))

    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(psi.resolveToFirSymbolOfType<FirTypeParameterSymbol>(firResolveSession))
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(psi.resolveToFirSymbolOfType<FirTypeAliasSymbol>(firResolveSession))
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        firSymbolBuilder.buildEnumEntrySymbol(psi.resolveToFirSymbolOfType<FirEnumEntrySymbol>(firResolveSession))
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(firResolveSession))
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol = withValidityAssertion {
        firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(
            psi.resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
        )
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(psi.resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession))
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol = withValidityAssertion {
        firSymbolBuilder.classifierBuilder.buildAnonymousObjectSymbol(
            psi.objectDeclaration.resolveToFirSymbolOfType<FirAnonymousObjectSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(psi.resolveToFirSymbolOfType<FirClassSymbol<*>>(firResolveSession))
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol? = withValidityAssertion {
        require(psi !is KtObjectDeclaration || psi.parent !is KtObjectLiteralExpression)
        // A KtClassOrObject may also map to an FirEnumEntry. Hence, we need to return null in this case.
        if (psi is KtEnumEntry) return null
        firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(psi.resolveToFirSymbolOfType<FirRegularClassSymbol>(firResolveSession))
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        firSymbolBuilder.callableBuilder.buildPropertyAccessorSymbol(psi.resolveToFirSymbolOfType<FirPropertyAccessorSymbol>(firResolveSession))
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol = withValidityAssertion {
        firSymbolBuilder.anonymousInitializerBuilder.buildClassInitializer(
            psi.resolveToFirSymbolOfType<FirAnonymousInitializerSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(symbol)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol -> firSymbolBuilder.buildSymbol(firSymbol) }
    }

    override val ROOT_PACKAGE_SYMBOL: KtPackageSymbol = KtFirPackageSymbol(FqName.ROOT, firResolveSession.project, token)
}
