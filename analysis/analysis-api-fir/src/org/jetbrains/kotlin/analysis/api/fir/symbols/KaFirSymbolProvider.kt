/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
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
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirSymbolProvider(
    override val analysisSessionProvider: () -> KaFirSession,
    private val firSymbolProvider: FirSymbolProvider,
) : KaBaseSymbolProvider<KaFirSession>(), KaFirSessionComponent {
    override fun symbol(parameter: KtParameter): KaVariableSymbol = withPsiValidityAssertion(parameter) {
        when {
            parameter.isFunctionTypeParameter -> errorWithFirSpecificEntries(
                "Creating ${KaVariableSymbol::class.simpleName} for function type parameter is not possible. " +
                        "Please see the KDoc of `symbol`",
                psi = parameter,
            )

            parameter.isLoopParameter || parameter.isCatchParameter -> KaFirLocalVariableSymbol(parameter, analysisSession)
            parameter.isContextParameter -> KaFirContextParameterSymbol(parameter, analysisSession)
            else -> KaFirValueParameterSymbol(parameter, analysisSession)
        }
    }

    override fun symbol(file: KtFile): KaFileSymbol = withPsiValidityAssertion(file) {
        KaFirFileSymbol(file, analysisSession)
    }

    override fun symbol(script: KtScript): KaScriptSymbol = withPsiValidityAssertion(script) {
        KaFirScriptSymbol(script, analysisSession)
    }

    override fun symbol(namedFunction: KtNamedFunction): KaFunctionSymbol = withPsiValidityAssertion(namedFunction) {
        if (namedFunction.isAnonymous) {
            KaFirAnonymousFunctionSymbol(namedFunction, analysisSession)
        } else {
            KaFirNamedFunctionSymbol(namedFunction, analysisSession)
        }
    }

    override fun symbol(constructor: KtConstructor<*>): KaConstructorSymbol = withPsiValidityAssertion(constructor) {
        KaFirConstructorSymbol(constructor, analysisSession)
    }

    override fun symbol(typeParameter: KtTypeParameter): KaTypeParameterSymbol = withPsiValidityAssertion(typeParameter) {
        KaFirTypeParameterSymbol(typeParameter, analysisSession)
    }

    override fun symbol(typeAlias: KtTypeAlias): KaTypeAliasSymbol = withPsiValidityAssertion(typeAlias) {
        KaFirTypeAliasSymbol(typeAlias, analysisSession)
    }

    override fun symbol(enumEntry: KtEnumEntry): KaEnumEntrySymbol = withPsiValidityAssertion(enumEntry) {
        KaFirEnumEntrySymbol(enumEntry, analysisSession)
    }

    override fun symbol(functionLiteral: KtFunctionLiteral): KaAnonymousFunctionSymbol = withPsiValidityAssertion(functionLiteral) {
        KaFirAnonymousFunctionSymbol(functionLiteral, analysisSession)
    }

    override fun symbol(property: KtProperty): KaVariableSymbol = withPsiValidityAssertion(property) {
        if (property.isLocal) {
            KaFirLocalVariableSymbol(property, analysisSession)
        } else {
            KaFirKotlinPropertySymbol.create(property, analysisSession)
        }
    }

    override fun symbol(backingField: KtBackingField): KaBackingFieldSymbol = withPsiValidityAssertion(backingField) {
        val owningProperty = backingField.parent as? KtProperty ?: errorWithAttachment("Orphaned backing field") {
            withPsiEntry("psi", backingField)
        }

        KaFirBackingFieldSymbol(
            backingField,
            analysisSession,
            symbol(owningProperty) as KaFirKotlinPropertySymbol<*> // Backing fields for local properties don't parse
        )
    }

    override fun symbol(objectLiteralExpression: KtObjectLiteralExpression): KaAnonymousObjectSymbol =
        withPsiValidityAssertion(objectLiteralExpression) {
            KaFirAnonymousObjectSymbol(objectLiteralExpression.objectDeclaration, analysisSession)
        }

    override fun symbol(objectDeclaration: KtObjectDeclaration): KaClassSymbol = withPsiValidityAssertion(objectDeclaration) {
        if (objectDeclaration.isObjectLiteral()) {
            KaFirAnonymousObjectSymbol(objectDeclaration, analysisSession)
        } else {
            KaFirNamedClassSymbol(objectDeclaration, analysisSession)
        }
    }

    override fun classSymbol(classOrObject: KtClassOrObject): KaClassSymbol? = withPsiValidityAssertion(classOrObject) {
        when (classOrObject) {
            is KtEnumEntry -> null
            is KtObjectDeclaration -> symbol(classOrObject)
            else -> KaFirNamedClassSymbol(classOrObject, analysisSession)
        }
    }

    override fun namedClassSymbol(classOrObject: KtClassOrObject): KaNamedClassSymbol? = withPsiValidityAssertion(classOrObject) {
        if (classOrObject is KtEnumEntry || classOrObject.isObjectLiteral()) {
            return null
        }

        KaFirNamedClassSymbol(classOrObject, analysisSession)
    }

    override fun symbol(propertyAccessor: KtPropertyAccessor): KaPropertyAccessorSymbol = withPsiValidityAssertion(propertyAccessor) {
        if (propertyAccessor.isGetter) {
            KaFirPropertyGetterSymbol.create(propertyAccessor, analysisSession)
        } else {
            KaFirPropertySetterSymbol.create(propertyAccessor, analysisSession)
        }
    }

    override fun symbol(classInitializer: KtClassInitializer): KaClassInitializerSymbol = withPsiValidityAssertion(classInitializer) {
        KaFirClassInitializerSymbol(classInitializer, analysisSession)
    }

    override fun symbol(entry: KtDestructuringDeclarationEntry): KaVariableSymbol = withPsiValidityAssertion(entry) {
        when (val parent = entry.parent) {
            is KtDestructuringDeclaration -> {
                if (parent.parent?.parent is KtScript) {
                    KaFirKotlinPropertySymbol.create(entry, analysisSession)
                } else {
                    KaFirLocalVariableSymbol(entry, analysisSession)
                }
            }

            is PsiErrorElement -> {
                val destructuringDeclaration = parent.parent as KtDestructuringDeclaration
                KaFirErrorVariableSymbol(destructuringDeclaration, analysisSession)
            }

            else -> errorWithFirSpecificEntries("Unexpected type of parent", psi = entry) {
                withPsiEntry("parent", parent)
            }
        }
    }

    override fun symbol(destructuringDeclaration: KtDestructuringDeclaration): KaDestructuringDeclarationSymbol =
        withPsiValidityAssertion(destructuringDeclaration) {
            KaFirDestructuringDeclarationSymbol(destructuringDeclaration, analysisSession)
        }

    override fun symbol(contextReceiver: KtContextReceiver): KaContextParameterSymbol =
        withPsiValidityAssertion(contextReceiver) {
            KaFirContextReceiverBasedContextParameterSymbol(contextReceiver, analysisSession)
        }

    override fun findClass(classId: ClassId): KaNamedClassSymbol? = withValidityAssertion {
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
