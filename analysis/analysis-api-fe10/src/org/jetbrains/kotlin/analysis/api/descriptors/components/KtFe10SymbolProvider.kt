/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

internal class KtFe10SymbolProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolProvider(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
        get() = KtFe10PackageSymbol(FqName.ROOT, analysisContext)

    override fun getFileSymbol(psi: KtFile): KtFileSymbol {
        return KtFe10FileSymbol(psi, analysisContext)
    }

    override fun getScriptSymbol(psi: KtScript): KtScriptSymbol {
        return KtFe10PsiScriptSymbol(psi, analysisContext)
    }

    override fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol {
        return when {
            psi.isFunctionTypeParameter -> error("Function type parameters are not supported in getParameterSymbol()")
            psi.isLoopParameter -> KtFe10PsiLoopParameterLocalVariableSymbol(psi, analysisContext)
            else -> KtFe10PsiValueParameterSymbol(psi, analysisContext)
        }
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol {
        return if (psi.hasBody() && (psi.funKeyword == null || psi.nameIdentifier == null)) {
            getAnonymousFunctionSymbol(psi)
        } else {
            KtFe10PsiFunctionSymbol(psi, analysisContext)
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol {
        return KtFe10PsiConstructorSymbol(psi, analysisContext)
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol {
        return KtFe10PsiTypeParameterSymbol(psi, analysisContext)
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol {
        return KtFe10PsiTypeAliasSymbol(psi, analysisContext)
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol {
        return KtFe10PsiEnumEntrySymbol(psi, analysisContext)
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol {
        return KtFe10PsiAnonymousFunctionSymbol(psi, analysisContext)
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol {
        return KtFe10PsiLiteralAnonymousFunctionSymbol(psi, analysisContext)
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol {
        return if (psi.isLocal) {
            KtFe10PsiLocalVariableSymbol(psi, analysisContext)
        } else {
            KtFe10PsiKotlinPropertySymbol(psi, analysisContext)
        }
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol {
        return KtFe10PsiAnonymousObjectSymbol(psi.objectDeclaration, analysisContext)
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol? {
        return if (psi is KtEnumEntry) {
            null
        } else if (psi is KtObjectDeclaration && psi.isObjectLiteral()) {
            KtFe10PsiAnonymousObjectSymbol(psi, analysisContext)
        } else {
            KtFe10PsiNamedClassOrObjectSymbol(psi, analysisContext)
        }
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol? {
        if (psi is KtEnumEntry || psi.nameIdentifier == null) {
            return null
        }

        return KtFe10PsiNamedClassOrObjectSymbol(psi, analysisContext)
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol {
        return if (psi.isGetter) {
            KtFe10PsiPropertyGetterSymbol(psi, analysisContext)
        } else {
            KtFe10PsiPropertySetterSymbol(psi, analysisContext)
        }
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol {
        return KtFe10PsiClassInitializerSymbol(psi, analysisContext)
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassSymbol(analysisContext)
    }

    override fun getTypeAliasByClassId(classId: ClassId): KtTypeAliasSymbol? {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findTypeAliasAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassifierSymbol(analysisContext) as? KtTypeAliasSymbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtCallableSymbol> {
        val packageViewDescriptor = analysisContext.resolveSession.moduleDescriptor.getPackage(packageFqName)
        return packageViewDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, nameFilter = { it == name })
            .asSequence()
            .filter { it.name == name }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KtCallableSymbol }
    }

    override fun getPackageSymbolIfPackageExists(packageFqName: FqName): KtPackageSymbol? {
        if (analysisContext.resolveSession.packageFragmentProvider.isEmpty(packageFqName)) return null
        return KtFe10PackageSymbol(packageFqName, analysisContext)
    }

    override fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KtLocalVariableSymbol {
        return KtFe10PsiLocalVariableSymbol(psi, analysisContext)
    }

    override fun getDestructuringDeclarationSymbol(psi: KtDestructuringDeclaration): KtDestructuringDeclarationSymbol {
        return KtFe10PsiDestructuringDeclarationSymbol(psi, analysisSession)
    }
}