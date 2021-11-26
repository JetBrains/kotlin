/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDeclarationOfType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassInitializerSymbol
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

    override fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol = withValidityAssertion {
        when {
            psi.isFunctionTypeParameter -> error(
                "Creating KtValueParameterSymbol for function type parameter is not possible. Please see the KDoc of getParameterSymbol"
            )

            psi.isLoopParameter -> psi.withFirDeclarationOfType<FirProperty, KtLocalVariableSymbol>(resolveState) {
                firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(it)
            }

            else -> psi.withFirDeclarationOfType<FirValueParameter, KtValueParameterSymbol>(resolveState) {
                firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(it)
            }
        }
    }

    override fun getFileSymbol(psi: KtFile): KtFileSymbol = withValidityAssertion {
        firSymbolBuilder.buildFileSymbol(psi.getOrBuildFirFile(resolveState))
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirFunction, KtFunctionLikeSymbol>(resolveState) { fir ->
            when (fir) {
                is FirSimpleFunction -> {
                    if (fir.origin == FirDeclarationOrigin.SamConstructor) {
                        firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(fir)
                    } else {
                        firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(fir)
                    }
                }
                is FirAnonymousFunction -> firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(fir)
                else -> error("Unexpected ${fir.renderWithType()}")
            }
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirConstructor, KtConstructorSymbol>(resolveState) {
            firSymbolBuilder.functionLikeBuilder.buildConstructorSymbol(it)
        }
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirTypeParameter, KtTypeParameterSymbol>(resolveState) {
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(it)
        }
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirTypeAlias, KtTypeAliasSymbol>(resolveState) {
            firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(it)
        }
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirEnumEntry, KtEnumEntrySymbol>(resolveState) {
            firSymbolBuilder.buildEnumEntrySymbol(it)
        }
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirSimpleFunction, KtFunctionSymbol>(resolveState) {
            firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(it)
        }
        firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirAnonymousFunction, KtAnonymousFunctionSymbol>(resolveState) {
            firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(it)
        }
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirProperty, KtVariableSymbol>(resolveState) {
            firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(it)
        }
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol = withValidityAssertion {
        psi.objectDeclaration.withFirDeclarationOfType<FirAnonymousObject, KtAnonymousObjectSymbol>(resolveState) {
            firSymbolBuilder.classifierBuilder.buildAnonymousObjectSymbol(it)
        }
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirClass, KtClassOrObjectSymbol>(resolveState) {
            firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(it)
        }
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol? = withValidityAssertion {
        require(psi !is KtObjectDeclaration || psi.parent !is KtObjectLiteralExpression)
        // A KtClassOrObject may also map to an FirEnumEntry. Hence, we need to return null in this case.
        if (psi is KtEnumEntry) return null
        psi.withFirDeclarationOfType<FirRegularClass, KtNamedClassOrObjectSymbol>(resolveState) {
            firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(it)
        }
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirPropertyAccessor, KtPropertyAccessorSymbol>(resolveState) {
            firSymbolBuilder.callableBuilder.buildPropertyAccessorSymbol(it)
        }
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol = withValidityAssertion {
        psi.withFirDeclarationOfType<FirAnonymousInitializer, KtClassInitializerSymbol>(resolveState) {
            firSymbolBuilder.anonymousInitializerBuilder.buildClassInitializer(it)
        }
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(symbol.fir)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol -> firSymbolBuilder.buildSymbol(firSymbol.fir) }
    }

    override val ROOT_PACKAGE_SYMBOL: KtPackageSymbol = KtFirPackageSymbol(FqName.ROOT, resolveState.project, token)
}
