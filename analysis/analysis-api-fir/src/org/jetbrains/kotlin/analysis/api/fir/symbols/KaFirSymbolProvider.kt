/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSymbolProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral

internal class KaFirSymbolProvider(
    override val analysisSessionProvider: () -> KaFirSession,
    private val firSymbolProvider: FirSymbolProvider,
) : KaBaseSymbolProvider<KaFirSession>(), KaFirSessionComponent {
    override val KtParameter.symbol: KaVariableSymbol
        get() = withValidityAssertion {
            when {
                isFunctionTypeParameter -> errorWithFirSpecificEntries(
                    "Creating ${KaVariableSymbol::class.simpleName} for function type parameter is not possible. " +
                            "Please see the KDoc of `symbol`",
                    psi = this,
                )

                isLoopParameter || isCatchParameter -> firSymbolBuilder.variableBuilder.buildLocalVariableSymbol(
                    resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession)
                )

                else -> firSymbolBuilder.variableBuilder.buildValueParameterSymbol(
                    resolveToFirSymbolOfType<FirValueParameterSymbol>(firResolveSession)
                )
            }
        }

    override val KtFile.symbol: KaFileSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.buildFileSymbol(
                getOrBuildFirFile(firResolveSession).symbol
            )
        }

    override val KtScript.symbol: KaScriptSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.buildScriptSymbol(
                resolveToFirSymbolOfType<FirScriptSymbol>(firResolveSession)
            )
        }

    override val KtNamedFunction.symbol: KaFunctionSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.functionBuilder.buildFunctionSymbol(
                resolveToFirSymbolOfType<FirFunctionSymbol<*>>(firResolveSession),
            )
        }

    override val KtConstructor<*>.symbol: KaConstructorSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.functionBuilder.buildConstructorSymbol(
                resolveToFirSymbolOfType<FirConstructorSymbol>(firResolveSession)
            )
        }

    override val KtTypeParameter.symbol: KaTypeParameterSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(
                resolveToFirSymbolOfType<FirTypeParameterSymbol>(firResolveSession)
            )
        }

    override val KtTypeAlias.symbol: KaTypeAliasSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(
                resolveToFirSymbolOfType<FirTypeAliasSymbol>(firResolveSession)
            )
        }

    override val KtEnumEntry.symbol: KaEnumEntrySymbol
        get() = withValidityAssertion {
            firSymbolBuilder.buildEnumEntrySymbol(
                resolveToFirSymbolOfType<FirEnumEntrySymbol>(firResolveSession)
            )
        }

    override val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.functionBuilder.buildAnonymousFunctionSymbol(
                resolveToFirSymbolOfType<FirAnonymousFunctionSymbol>(firResolveSession)
            )
        }

    override val KtProperty.symbol: KaVariableSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.variableBuilder.buildVariableSymbol(
                resolveToFirSymbolOfType<FirPropertySymbol>(firResolveSession)
            )
        }

    override val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.classifierBuilder.buildAnonymousObjectSymbol(
                objectDeclaration.resolveToFirSymbolOfType<FirAnonymousObjectSymbol>(firResolveSession)
            )
        }

    override val KtObjectDeclaration.symbol: KaClassSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(
                resolveToFirSymbolOfType<FirClassSymbol<*>>(firResolveSession)
            )
        }

    override val KtClassOrObject.classSymbol: KaClassSymbol?
        get() = withValidityAssertion {
            if (this is KtEnumEntry) {
                return null
            }

            firSymbolBuilder.classifierBuilder.buildClassOrObjectSymbol(
                resolveToFirSymbolOfType<FirClassSymbol<*>>(firResolveSession)
            )
        }

    override val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?
        get() = withValidityAssertion {
            if (this is KtEnumEntry || this.isObjectLiteral()) {
                return null
            }

            firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(
                resolveToFirSymbolOfType<FirRegularClassSymbol>(firResolveSession)
            )
        }

    override val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.functionBuilder.buildPropertyAccessorSymbol(
                resolveToFirSymbolOfType<FirPropertyAccessorSymbol>(firResolveSession)
            )
        }

    override val KtClassInitializer.symbol: KaClassInitializerSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.anonymousInitializerBuilder.buildClassInitializer(
                resolveToFirSymbolOfType<FirAnonymousInitializerSymbol>(firResolveSession)
            )
        }

    override val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.variableBuilder.buildVariableSymbol(
                resolveToFirSymbolOfType<FirVariableSymbol<*>>(firResolveSession)
            )
        }

    override val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
        get() = withValidityAssertion {
            firSymbolBuilder.buildDestructuringDeclarationSymbol(
                resolveToFirSymbolOfType<FirVariableSymbol<*>>(firResolveSession)
            )
        }

    override fun findClass(classId: ClassId): KaClassSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(symbol)
    }

    override fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirTypeAliasSymbol ?: return null
        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(symbol)
    }

    override fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> = withValidityAssertion {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol ->
            firSymbolBuilder.buildSymbol(firSymbol) as KaCallableSymbol
        }
    }

    override fun findPackage(fqName: FqName): KaPackageSymbol? = withValidityAssertion {
        firSymbolBuilder.createPackageSymbolIfOneExists(fqName)
    }

    private val backingRootPackageSymbol by lazy { KaFirPackageSymbol(FqName.ROOT, firResolveSession.project, token) }

    override val rootPackageSymbol: KaPackageSymbol
        get() = withValidityAssertion { backingRootPackageSymbol }
}
