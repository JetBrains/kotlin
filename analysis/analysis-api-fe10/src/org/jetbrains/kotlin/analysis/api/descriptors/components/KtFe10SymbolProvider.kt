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
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKaSymbolProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
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
    override val analysisSessionProvider: () -> KaFe10Session
) : AbstractKaSymbolProvider<KaFe10Session>(), KaFe10SessionComponent {
    override val rootPackageSymbol: KaPackageSymbol
        get() = withValidityAssertion {
            KaFe10PackageSymbol(FqName.ROOT, analysisContext)
        }

    override val KtFile.symbol: KaFileSymbol
        get() = withValidityAssertion { KaFe10FileSymbol(this, this@KaFe10SymbolProvider.analysisContext) }

    override val KtScript.symbol: KaScriptSymbol
        get() = withValidityAssertion { KaFe10PsiScriptSymbol(this, analysisContext) }

    override val KtParameter.symbol: KaVariableLikeSymbol
        get() = withValidityAssertion {
            when {
                isFunctionTypeParameter -> error("Function type parameters are not supported in getParameterSymbol()")
                isLoopParameter -> KaFe10PsiLoopParameterLocalVariableSymbol(this, analysisContext)
                else -> KaFe10PsiValueParameterSymbol(this, analysisContext)
            }
        }

    override val KtNamedFunction.symbol: KaFunctionLikeSymbol
        get() = withValidityAssertion {
            return if (hasBody() && (funKeyword == null || nameIdentifier == null)) {
                anonymousSymbol
            } else {
                KaFe10PsiFunctionSymbol(this, analysisContext)
            }
        }

    override val KtConstructor<*>.symbol: KaConstructorSymbol
        get() = withValidityAssertion { KaFe10PsiConstructorSymbol(this, analysisContext) }

    override val KtTypeParameter.symbol: KaTypeParameterSymbol
        get() = withValidityAssertion { KaFe10PsiTypeParameterSymbol(this, analysisContext) }

    override val KtTypeAlias.symbol: KaTypeAliasSymbol
        get() = withValidityAssertion { KaFe10PsiTypeAliasSymbol(this, analysisContext) }

    override val KtEnumEntry.symbol: KaEnumEntrySymbol
        get() = withValidityAssertion { KaFe10PsiEnumEntrySymbol(this, analysisContext) }

    override val KtNamedFunction.anonymousSymbol: KaAnonymousFunctionSymbol
        get() = withValidityAssertion { KaFe10PsiAnonymousFunctionSymbol(this, analysisContext) }

    override val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
        get() = withValidityAssertion { KaFe10PsiLiteralAnonymousFunctionSymbol(this, analysisContext) }

    override val KtProperty.symbol: KaVariableSymbol
        get() = withValidityAssertion {
            return if (isLocal) {
                KaFe10PsiLocalVariableSymbol(this, analysisContext)
            } else {
                KaFe10PsiKotlinPropertySymbol(this, analysisContext)
            }
        }

    override val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
        get() = withValidityAssertion { KaFe10PsiAnonymousObjectSymbol(objectDeclaration, analysisContext) }

    override val KtObjectDeclaration.symbol: KaClassOrObjectSymbol
        get() = withValidityAssertion { KaFe10PsiNamedClassOrObjectSymbol(this, analysisContext) }

    override val KtClassOrObject.classSymbol: KaClassOrObjectSymbol?
        get() = withValidityAssertion {
            return if (this is KtEnumEntry) {
                null
            } else if (this is KtObjectDeclaration && isObjectLiteral()) {
                KaFe10PsiAnonymousObjectSymbol(this, analysisContext)
            } else {
                KaFe10PsiNamedClassOrObjectSymbol(this, analysisContext)
            }
        }

    override val KtClassOrObject.namedClassSymbol: KaNamedClassOrObjectSymbol?
        get() = withValidityAssertion {
            if (this is KtEnumEntry || nameIdentifier == null) {
                return null
            }

            return KaFe10PsiNamedClassOrObjectSymbol(this, analysisContext)
        }

    override val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
        get() = withValidityAssertion {
            return if (isGetter) {
                KaFe10PsiPropertyGetterSymbol(this, analysisContext)
            } else {
                KaFe10PsiPropertySetterSymbol(this, analysisContext)
            }
        }

    override val KtClassInitializer.symbol: KaClassInitializerSymbol
        get() = withValidityAssertion { KaFe10PsiClassInitializerSymbol(this, analysisContext) }

    override val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
        get() = withValidityAssertion { KaFe10PsiLocalVariableSymbol(this, analysisContext) }

    override val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
        get() = withValidityAssertion { KaFe10PsiDestructuringDeclarationSymbol(this, analysisSession) }

    override fun findClass(classId: ClassId): KaClassOrObjectSymbol? = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKaClassSymbol(analysisContext)
    }

    override fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findTypeAliasAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassifierSymbol(analysisContext) as? KaTypeAliasSymbol
    }

    override fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> = withValidityAssertion {
        val packageViewDescriptor = analysisContext.resolveSession.moduleDescriptor.getPackage(packageFqName)
        return packageViewDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, nameFilter = { it == name })
            .asSequence()
            .filter { it.name == name }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaCallableSymbol }
    }

    override fun findPackage(fqName: FqName): KaPackageSymbol? = withValidityAssertion {
        if (analysisContext.resolveSession.packageFragmentProvider.isEmpty(fqName)) return null
        return KaFe10PackageSymbol(fqName, analysisContext)
    }
}