/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSymbolProvider(
    override val analysisSession: KaFirSession,
    private val firSymbolProvider: FirSymbolProvider,
) : KaSymbolProvider(), KaFirSessionComponent {

    override fun getParameterSymbol(psi: KtParameter): KaVariableLikeSymbol {
        return when {
            psi.isFunctionTypeParameter -> errorWithFirSpecificEntries(
                "Creating KtValueParameterSymbol for function type parameter is not possible. Please see the KDoc of getParameterSymbol",
                psi = psi,
            )

            psi.isLoopParameter || psi.isCatchParameter -> {
                firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(
                    psi.resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession)
                )
            }

            else -> {
                firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(
                    psi.resolveToFirSymbolOfType<FirValueParameterSymbol>(firResolveSession)
                )
            }
        }
    }

    override fun getFileSymbol(psi: KtFile): KaFileSymbol {
        return firSymbolBuilder.buildFileSymbol(psi.getOrBuildFirFile(firResolveSession).symbol)
    }

    override fun getScriptSymbol(psi: KtScript): KaScriptSymbol {
        return firSymbolBuilder.buildScriptSymbol(psi.resolveToFirSymbolOfType<FirScriptSymbol>(firResolveSession))
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KaFunctionLikeSymbol {
        return when (val firSymbol = psi.resolveToFirSymbolOfType<FirFunctionSymbol<*>>(firResolveSession)) {
            is FirNamedFunctionSymbol -> {
                if (firSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                    firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(firSymbol)
                } else {
                    firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firSymbol)
                }
            }

            is FirAnonymousFunctionSymbol -> firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(firSymbol)
            else -> errorWithAttachment("Unexpected ${firSymbol::class}") {
                withFirSymbolEntry("firSymbol", firSymbol)
                withPsiEntry("function", psi, analysisSession::getModule)
            }
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KaConstructorSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildConstructorSymbol(
            psi.resolveToFirSymbolOfType<FirConstructorSymbol>(firResolveSession)
        )
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KaTypeParameterSymbol {
        return firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(
            psi.resolveToFirSymbolOfType<FirTypeParameterSymbol>(firResolveSession)
        )
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KaTypeAliasSymbol {
        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(psi.resolveToFirSymbolOfType<FirTypeAliasSymbol>(firResolveSession))
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KaEnumEntrySymbol {
        return firSymbolBuilder.buildEnumEntrySymbol(psi.resolveToFirSymbolOfType<FirEnumEntrySymbol>(firResolveSession))
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KaAnonymousFunctionSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(
            psi.resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
        )
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KaAnonymousFunctionSymbol {
        return firSymbolBuilder.functionLikeBuilder.buildAnonymousFunctionSymbol(
            psi.resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
        )
    }

    override fun getVariableSymbol(psi: KtProperty): KaVariableSymbol {
        return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(psi.resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession))
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KaAnonymousObjectSymbol {
        return firSymbolBuilder.classifierBuilder.buildAnonymousObjectSymbol(
            psi.objectDeclaration.resolveToFirSymbolOfType<FirAnonymousObjectSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KaClassOrObjectSymbol? {
        if (psi is KtEnumEntry) {
            return null
        }

        val firSymbol = psi.resolveToFirClassLikeSymbol()
        return firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(firSymbol)
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KaNamedClassOrObjectSymbol? {
        if (psi is KtEnumEntry || psi.isObjectLiteral()) {
            return null
        }

        val firSymbol = psi.resolveToFirClassLikeSymbol() as FirRegularClassSymbol
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(firSymbol)
    }

    private fun KtClassOrObject.resolveToFirClassLikeSymbol(): FirClassSymbol<*> {
        return when (val firClassLike = resolveToFirSymbolOfType<FirClassLikeSymbol<*>>(firResolveSession)) {
            is FirTypeAliasSymbol -> firClassLike.fullyExpandedClass(analysisSession.useSiteSession)
                ?: errorWithAttachment("${firClassLike.fir::class} should be expanded to the expected type alias") {
                    val errorElement = this@resolveToFirClassLikeSymbol
                    withFirSymbolEntry("firClassLikeSymbol", firClassLike)
                    withPsiEntry("ktClassOrObject", errorElement, analysisSession::getModule)
                }
            is FirAnonymousObjectSymbol -> firClassLike
            is FirRegularClassSymbol -> firClassLike
        }
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KaPropertyAccessorSymbol {
        return firSymbolBuilder.callableBuilder.buildPropertyAccessorSymbol(
            psi.resolveToFirSymbolOfType<FirPropertyAccessorSymbol>(firResolveSession)
        )
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KaClassInitializerSymbol {
        return firSymbolBuilder.anonymousInitializerBuilder.buildClassInitializer(
            psi.resolveToFirSymbolOfType<FirAnonymousInitializerSymbol>(firResolveSession)
        )
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KaClassOrObjectSymbol? {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(symbol)
    }

    override fun getTypeAliasByClassId(classId: ClassId): KaTypeAliasSymbol? {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirTypeAliasSymbol ?: return null
        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(symbol)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol ->
            firSymbolBuilder.buildSymbol(firSymbol) as KaCallableSymbol
        }
    }

    override fun getPackageSymbolIfPackageExists(packageFqName: FqName): KaPackageSymbol? {
        return firSymbolBuilder.createPackageSymbolIfOneExists(packageFqName)
    }

    override val ROOT_PACKAGE_SYMBOL: KaPackageSymbol = KaFirPackageSymbol(FqName.ROOT, firResolveSession.project, token)

    override fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KaVariableSymbol {
        return when (val firSymbol = psi.resolveToFirSymbol(firResolveSession)) {
            is FirPropertySymbol -> firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firSymbol)
            is FirErrorPropertySymbol -> firSymbolBuilder.variableLikeBuilder.buildErrorVariableSymbol(firSymbol)
            else -> throwUnexpectedFirElementError(
                firSymbol,
                psi,
                FirPropertySymbol::class,
                FirErrorPropertySymbol::class,
                FirValueParameterSymbol::class,
            )
        }
    }

    override fun getDestructuringDeclarationSymbol(psi: KtDestructuringDeclaration): KaDestructuringDeclarationSymbol {
        val firSymbol = psi.resolveToFirSymbolOfType<FirVariableSymbol<*>>(firResolveSession)
        return firSymbolBuilder.buildDestructuringDeclarationSymbol(firSymbol)
    }
}
