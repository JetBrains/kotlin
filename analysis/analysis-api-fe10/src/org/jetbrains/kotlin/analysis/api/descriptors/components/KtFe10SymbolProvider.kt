/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKaClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

internal class KaFe10SymbolProvider(
    override val analysisSession: KaFe10Session
) : KaSymbolProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override val ROOT_PACKAGE_SYMBOL: KaPackageSymbol
        get() = KaFe10PackageSymbol(FqName.ROOT, analysisContext)

    override fun getFileSymbol(psi: KtFile): KaFileSymbol {
        return KaFe10FileSymbol(psi, analysisContext)
    }

    override fun getScriptSymbol(psi: KtScript): KaScriptSymbol {
        return KaFe10PsiScriptSymbol(psi, analysisContext)
    }

    override fun getParameterSymbol(psi: KtParameter): KaVariableLikeSymbol {
        return when {
            psi.isFunctionTypeParameter -> error("Function type parameters are not supported in getParameterSymbol()")
            psi.isLoopParameter -> KaFe10PsiLoopParameterLocalVariableSymbol(psi, analysisContext)
            else -> KaFe10PsiValueParameterSymbol(psi, analysisContext)
        }
    }

    override fun getFunctionLikeSymbol(psi: KtNamedFunction): KaFunctionLikeSymbol {
        return if (psi.hasBody() && (psi.funKeyword == null || psi.nameIdentifier == null)) {
            getAnonymousFunctionSymbol(psi)
        } else {
            KaFe10PsiFunctionSymbol(psi, analysisContext)
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KaConstructorSymbol {
        return KaFe10PsiConstructorSymbol(psi, analysisContext)
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KaTypeParameterSymbol {
        return KaFe10PsiTypeParameterSymbol(psi, analysisContext)
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KaTypeAliasSymbol {
        return KaFe10PsiTypeAliasSymbol(psi, analysisContext)
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KaEnumEntrySymbol {
        return KaFe10PsiEnumEntrySymbol(psi, analysisContext)
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KaAnonymousFunctionSymbol {
        return KaFe10PsiAnonymousFunctionSymbol(psi, analysisContext)
    }

    override fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KaAnonymousFunctionSymbol {
        return KaFe10PsiLiteralAnonymousFunctionSymbol(psi, analysisContext)
    }

    override fun getVariableSymbol(psi: KtProperty): KaVariableSymbol {
        return if (psi.isLocal) {
            KaFe10PsiLocalVariableSymbol(psi, analysisContext)
        } else {
            KaFe10PsiKotlinPropertySymbol(psi, analysisContext)
        }
    }

    override fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KaAnonymousObjectSymbol {
        return KaFe10PsiAnonymousObjectSymbol(psi.objectDeclaration, analysisContext)
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KaClassOrObjectSymbol? {
        return if (psi is KtEnumEntry) {
            null
        } else if (psi is KtObjectDeclaration && psi.isObjectLiteral()) {
            KaFe10PsiAnonymousObjectSymbol(psi, analysisContext)
        } else {
            KaFe10PsiNamedClassOrObjectSymbol(psi, analysisContext)
        }
    }

    override fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KaNamedClassOrObjectSymbol? {
        if (psi is KtEnumEntry || psi.nameIdentifier == null) {
            return null
        }

        return KaFe10PsiNamedClassOrObjectSymbol(psi, analysisContext)
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KaPropertyAccessorSymbol {
        return if (psi.isGetter) {
            KaFe10PsiPropertyGetterSymbol(psi, analysisContext)
        } else {
            KaFe10PsiPropertySetterSymbol(psi, analysisContext)
        }
    }

    override fun getClassInitializerSymbol(psi: KtClassInitializer): KaClassInitializerSymbol {
        return KaFe10PsiClassInitializerSymbol(psi, analysisContext)
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KaClassOrObjectSymbol? {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKaClassSymbol(analysisContext)
    }

    override fun getTypeAliasByClassId(classId: ClassId): KaTypeAliasSymbol? {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findTypeAliasAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassifierSymbol(analysisContext) as? KaTypeAliasSymbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> {
        val packageViewDescriptor = analysisContext.resolveSession.moduleDescriptor.getPackage(packageFqName)
        return packageViewDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, nameFilter = { it == name })
            .asSequence()
            .filter { it.name == name }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaCallableSymbol }
    }

    override fun getPackageSymbolIfPackageExists(packageFqName: FqName): KaPackageSymbol? {
        if (analysisContext.resolveSession.packageFragmentProvider.isEmpty(packageFqName)) return null
        return KaFe10PackageSymbol(packageFqName, analysisContext)
    }

    override fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KaVariableSymbol {
        return KaFe10PsiLocalVariableSymbol(psi, analysisContext)
    }

    override fun getDestructuringDeclarationSymbol(psi: KtDestructuringDeclaration): KaDestructuringDeclarationSymbol {
        return KaFe10PsiDestructuringDeclarationSymbol(psi, analysisSession)
    }
}