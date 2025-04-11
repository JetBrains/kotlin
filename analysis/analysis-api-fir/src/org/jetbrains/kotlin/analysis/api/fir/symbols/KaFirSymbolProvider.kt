/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSymbolProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirSymbolProvider(
    override val analysisSessionProvider: () -> KaFirSession,
    private val firSymbolProvider: FirSymbolProvider,
) : KaBaseSymbolProvider<KaFirSession>(), KaFirSessionComponent {
    override val KtParameter.symbol: KaVariableSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            when {
                isFunctionTypeParameter -> errorWithFirSpecificEntries(
                    "Creating ${KaVariableSymbol::class.simpleName} for function type parameter is not possible. " +
                            "Please see the KDoc of `symbol`",
                    psi = this,
                )

                isLoopParameter || isCatchParameter -> KaFirLocalVariableSymbol(this, analysisSession)
                isContextParameter -> KaFirContextParameterSymbol(this, analysisSession)
                else -> KaFirValueParameterSymbol(this, analysisSession)
            }
        }

    override val KtFile.symbol: KaFileSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirFileSymbol(this, analysisSession)
        }

    override val KtScript.symbol: KaScriptSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirScriptSymbol(this, analysisSession)
        }

    override val KtNamedFunction.symbol: KaFunctionSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            if (isAnonymous) {
                KaFirAnonymousFunctionSymbol(this, analysisSession)
            } else {
                KaFirNamedFunctionSymbol(this, analysisSession)
            }
        }

    override val KtConstructor<*>.symbol: KaConstructorSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirConstructorSymbol(this, analysisSession)
        }

    override val KtTypeParameter.symbol: KaTypeParameterSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirTypeParameterSymbol(this, analysisSession)
        }

    override val KtTypeAlias.symbol: KaTypeAliasSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirTypeAliasSymbol(this, analysisSession)
        }

    override val KtEnumEntry.symbol: KaEnumEntrySymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirEnumEntrySymbol(this, analysisSession)
        }

    override val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirAnonymousFunctionSymbol(this, analysisSession)
        }

    override val KtProperty.symbol: KaVariableSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            if (isLocal) {
                KaFirLocalVariableSymbol(this, analysisSession)
            } else {
                KaFirKotlinPropertySymbol.create(this, analysisSession)
            }
        }

    override val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirAnonymousObjectSymbol(objectDeclaration, analysisSession)
        }

    override val KtObjectDeclaration.symbol: KaClassSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            if (isObjectLiteral()) {
                KaFirAnonymousObjectSymbol(this, analysisSession)
            } else {
                KaFirNamedClassSymbol(this, analysisSession)
            }
        }

    override val KtClassOrObject.classSymbol: KaClassSymbol?
        get() = createPsiBasedSymbolWithValidityAssertion {
            when (this) {
                is KtEnumEntry -> null
                is KtObjectDeclaration -> symbol
                else -> KaFirNamedClassSymbol(this, analysisSession)
            }
        }

    override val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?
        get() = createPsiBasedSymbolWithValidityAssertion {
            if (this is KtEnumEntry || this.isObjectLiteral()) {
                return null
            }

            KaFirNamedClassSymbol(this, analysisSession)
        }

    override val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            if (isGetter) {
                KaFirPropertyGetterSymbol.create(this, analysisSession)
            } else {
                KaFirPropertySetterSymbol.create(this, analysisSession)
            }
        }

    override val KtClassInitializer.symbol: KaClassInitializerSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirClassInitializerSymbol(this, analysisSession)
        }

    override val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            when (val parent = parent) {
                is KtDestructuringDeclaration -> {
                    if (parent.parent?.parent is KtScript) {
                        KaFirKotlinPropertySymbol.create(this, analysisSession)
                    } else {
                        KaFirLocalVariableSymbol(this, analysisSession)
                    }
                }

                is PsiErrorElement -> {
                    val destructuringDeclaration = parent.parent as KtDestructuringDeclaration
                    KaFirErrorVariableSymbol(destructuringDeclaration, analysisSession)
                }

                else -> errorWithFirSpecificEntries("Unexpected type of parent", psi = this) {
                    withPsiEntry("parent", parent)
                }
            }
        }

    override val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirDestructuringDeclarationSymbol(this, analysisSession)
        }

    override val KtContextReceiver.symbol: KaContextParameterSymbol
        get() = createPsiBasedSymbolWithValidityAssertion {
            KaFirContextReceiverBasedContextParameterSymbol(this, analysisSession)
        }

    override fun findClass(classId: ClassId): KaClassSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        firSymbolBuilder.classifierBuilder.buildNamedClassSymbol(symbol)
    }

    override fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirTypeAliasSymbol ?: return null
        firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(symbol)
    }

    override fun findClassLike(classId: ClassId): KaClassLikeSymbol? {
        val symbol = firSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(symbol)
    }

    override fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> = withValidityAssertion {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        firs.asSequence().map { firSymbol ->
            firSymbolBuilder.buildSymbol(firSymbol) as KaCallableSymbol
        }
    }

    override fun findPackage(fqName: FqName): KaPackageSymbol? = withValidityAssertion {
        firSymbolBuilder.createPackageSymbolIfOneExists(fqName)
    }

    private val backingRootPackageSymbol by lazy { KaFirPackageSymbol(FqName.ROOT, resolutionFacade.project, token) }

    override val rootPackageSymbol: KaPackageSymbol
        get() = withValidityAssertion { backingRootPackageSymbol }
}
