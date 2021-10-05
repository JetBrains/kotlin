/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

internal class KtFe10SymbolProvider(override val analysisSession: KtFe10AnalysisSession) : KtSymbolProvider() {
    override val token: ValidityToken
        get() = analysisSession.token

    override val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
        get() = KtFe10PackageSymbol(FqName.ROOT, analysisSession)

    override fun getFileSymbol(psi: KtFile): KtFileSymbol = withValidityAssertion {
        return KtFe10FileSymbol(psi, analysisSession)
    }

    override fun getParameterSymbol(psi: KtParameter): KtValueParameterSymbol = withValidityAssertion {
        return KtFe10PsiValueParameterSymbol(psi, analysisSession)
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol = withValidityAssertion {
        return if (psi.hasBody() && (psi.funKeyword == null || psi.nameIdentifier == null)) {
            getAnonymousFunctionSymbol(psi)
        } else {
            KtFe10PsiFunctionSymbol(psi, analysisSession)
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {
        return KtFe10PsiConstructorSymbol(psi, analysisSession)
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        return KtFe10PsiTypeParameterSymbol(psi, analysisSession)
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        return KtFe10PsiTypeAliasSymbol(psi, analysisSession)
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        return KtFe10PsiEnumEntrySymbol(psi, analysisSession)
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        return KtFe10PsiAnonymousFunctionSymbol(psi, analysisSession)
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol = withValidityAssertion {
        return KtFe10PsiLiteralAnonymousFunctionSymbol(psi, analysisSession)
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        return if (psi.isLocal) {
            KtFe10PsiLocalVariableSymbol(psi, analysisSession)
        } else {
            KtFe10PsiKotlinPropertySymbol(psi, analysisSession)
        }
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol = withValidityAssertion {
        return KtFe10PsiAnonymousObjectSymbol(psi.objectDeclaration, analysisSession)
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        return if (psi is KtObjectDeclaration && psi.isObjectLiteral()) {
            KtFe10PsiAnonymousObjectSymbol(psi, analysisSession)
        } else {
            KtFe10PsiNamedClassOrObjectSymbol(psi, analysisSession)
        }
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol? = withValidityAssertion {
        if (psi is KtEnumEntry || psi.nameIdentifier == null) {
            return null
        }

        return KtFe10PsiNamedClassOrObjectSymbol(psi, analysisSession)
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        return if (psi.isGetter) {
            KtFe10PsiPropertyGetterSymbol(psi, analysisSession)
        } else {
            KtFe10PsiPropertySetterSymbol(psi, analysisSession)
        }
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol = withValidityAssertion {
        return KtFe10PsiClassInitializerSymbol(psi, analysisSession)
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? = withValidityAssertion {
        val descriptor = analysisSession.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassSymbol(analysisSession)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> = withValidityAssertion {
        val packageViewDescriptor = analysisSession.resolveSession.moduleDescriptor.getPackage(packageFqName)
        return packageViewDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, nameFilter = { it == name })
            .asSequence()
            .filter { it.name == name }
            .mapNotNull { it.toKtSymbol(analysisSession) as? KtCallableSymbol }
    }
}