/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.components.KtFirAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirSymbolEntry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry

internal class KtFirSymbolProvider(
    override val analysisSession: KtFirAnalysisSession,
    private val firSymbolProvider: FirSymbolProvider,
) : KtSymbolProvider(), KtFirAnalysisSessionComponent {

    override fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol {
        return when {
            psi.isFunctionTypeParameter -> errorWithFirSpecificEntries(
                "Creating KtValueParameterSymbol for function type parameter is not possible. Please see the KDoc of getParameterSymbol",
                psi = psi,
            )

            psi.isLoopParameter -> {
                firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(
                    psi.resolveToFirSymbolOfType<FirPropertySymbol>(
                        firResolveSession
                    )
                )
            }

            else -> {
                firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(
                    psi.resolveToFirSymbolOfType<FirValueParameterSymbol>(firResolveSession)
                )
            }
        }
    }

    override fun getFileSymbol(psi: KtFile): KtFileSymbol {
        return firSymbolBuilder.buildFileSymbol(psi.getOrBuildFirFile(firResolveSession).symbol)
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol {
        return when (val firSymbol = psi.resolveToFirSymbolOfType<FirFunctionSymbol<*>>(firResolveSession)) {
            is FirNamedFunctionSymbol -> {
                if (firSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                    firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(firSymbol)
                } else {
                    firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firSymbol)
                }
            }

            is FirAnonymousFunctionSymbol -> firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(firSymbol)
            else -> buildErrorWithAttachment("Unexpected ${firSymbol::class}") {
                withFirSymbolEntry("firSymbol", firSymbol)
                withPsiEntry("function", psi)
            }
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildConstructorSymbol(
            psi.resolveToFirSymbolOfType<FirConstructorSymbol>(
                firResolveSession
            )
        )
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol {
        return firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(
            psi.resolveToFirSymbolOfType<FirTypeParameterSymbol>(
                firResolveSession
            )
        )
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol {
        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(psi.resolveToFirSymbolOfType<FirTypeAliasSymbol>(firResolveSession))
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol {
        return firSymbolBuilder.buildEnumEntrySymbol(psi.resolveToFirSymbolOfType<FirEnumEntrySymbol>(firResolveSession))
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(
            psi.resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
        )
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(
            psi.resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
        )
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol {
        return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(psi.resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession))
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol {
        return firSymbolBuilder.classifierBuilder.buildAnonymousObjectSymbol(
            psi.objectDeclaration.resolveToFirSymbolOfType<FirAnonymousObjectSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol {
        val firClass = psi.resolveToFirClassLikeSymbol()
        return firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(firClass)
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol? {
        require(psi !is KtObjectDeclaration || psi.parent !is KtObjectLiteralExpression)
        // A KtClassOrObject may also map to an FirEnumEntry. Hence, we need to return null in this case.
        if (psi is KtEnumEntry) return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(psi.resolveToFirClassLikeSymbol() as FirRegularClassSymbol)
    }

    private fun KtClassOrObject.resolveToFirClassLikeSymbol(): FirClassSymbol<*> {
        return when (val firClassLike = resolveToFirSymbolOfType<FirClassLikeSymbol<*>>(firResolveSession)) {
            is FirTypeAliasSymbol -> firClassLike.fullyExpandedClass(firResolveSession.useSiteFirSession)
                ?: buildErrorWithAttachment("${firClassLike.fir::class} should be expanded to the expected type alias") {
                    withFirSymbolEntry("firClassLikeSymbol", firClassLike)
                    withPsiEntry("ktClassOrObject", this@resolveToFirClassLikeSymbol)
                }
            is FirAnonymousObjectSymbol -> firClassLike
            is FirRegularClassSymbol -> firClassLike
        }
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol {
        return firSymbolBuilder.callableBuilder.buildPropertyAccessorSymbol(
            psi.resolveToFirSymbolOfType<FirPropertyAccessorSymbol>(firResolveSession)
        )

    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol {
        return firSymbolBuilder.anonymousInitializerBuilder.buildClassInitializer(
            psi.resolveToFirSymbolOfType<FirAnonymousInitializerSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(symbol)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol -> firSymbolBuilder.buildSymbol(firSymbol) }
    }

    override fun getPackageSymbolIfPackageExists(packageFqName: FqName): KtPackageSymbol? {
        return firSymbolBuilder.createPackageSymbolIfOneExists(packageFqName)
    }

    override val ROOT_PACKAGE_SYMBOL: KtPackageSymbol = KtFirPackageSymbol(FqName.ROOT, firResolveSession.project, token)

    override fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KtLocalVariableSymbol {
        return firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(
            psi.resolveToFirSymbolOfType<FirPropertySymbol>(
                firResolveSession
            )
        )
    }
}
