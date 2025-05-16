/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFe10SymbolProvider(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaBaseSymbolProvider<KaFe10Session>(), KaFe10SessionComponent {
    override val rootPackageSymbol: KaPackageSymbol
        get() = withValidityAssertion {
            KaFe10PackageSymbol(FqName.ROOT, analysisContext)
        }

    override val KtFile.symbol: KaFileSymbol
        get() = withPsiValidityAssertion { KaFe10FileSymbol(this, this@KaFe10SymbolProvider.analysisContext) }

    override val KtScript.symbol: KaScriptSymbol
        get() = withPsiValidityAssertion { KaFe10PsiScriptSymbol(this, analysisContext) }

    override val KtParameter.symbol: KaVariableSymbol
        get() = withPsiValidityAssertion {
            when {
                isFunctionTypeParameter -> errorWithAttachment(
                    "Creating ${KaVariableSymbol::class.simpleName} for function type parameter is not possible. " +
                            "Please see the KDoc of `symbol`"
                ) {
                    withPsiEntry("parameter", this@symbol)
                }

                isLoopParameter -> KaFe10PsiLoopParameterLocalVariableSymbol(this, analysisContext)
                isContextParameter -> KaFe10PsiContextParameterSymbol(this, analysisContext)
                else -> KaFe10PsiValueParameterSymbol(this, analysisContext)
            }
        }

    override val KtContextReceiver.symbol: KaContextParameterSymbol
        get() = withPsiValidityAssertion {
            KaFe10PsiContextReceiverBasedContextParameterSymbol(this, analysisContext)
        }

    override val KtNamedFunction.symbol: KaFunctionSymbol
        get() = withPsiValidityAssertion {
            if (hasBody() && (funKeyword == null || nameIdentifier == null)) {
                KaFe10PsiAnonymousFunctionSymbol(this, analysisContext)
            } else {
                KaFe10PsiNamedFunctionSymbol(this, analysisContext)
            }
        }

    override val KtConstructor<*>.symbol: KaConstructorSymbol
        get() = withPsiValidityAssertion { KaFe10PsiConstructorSymbol(this, analysisContext) }

    override val KtTypeParameter.symbol: KaTypeParameterSymbol
        get() = withPsiValidityAssertion { KaFe10PsiTypeParameterSymbol(this, analysisContext) }

    override val KtTypeAlias.symbol: KaTypeAliasSymbol
        get() = withPsiValidityAssertion { KaFe10PsiTypeAliasSymbol(this, analysisContext) }

    override val KtEnumEntry.symbol: KaEnumEntrySymbol
        get() = withPsiValidityAssertion { KaFe10PsiEnumEntrySymbol(this, analysisContext) }

    override val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
        get() = withPsiValidityAssertion { KaFe10PsiLiteralAnonymousFunctionSymbol(this, analysisContext) }

    override val KtProperty.symbol: KaVariableSymbol
        get() = withPsiValidityAssertion {
            if (isLocal) {
                KaFe10PsiLocalVariableSymbol(this, analysisContext)
            } else {
                KaFe10PsiKotlinPropertySymbol(this, analysisContext)
            }
        }

    override val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
        get() = withPsiValidityAssertion { KaFe10PsiAnonymousObjectSymbol(objectDeclaration, analysisContext) }

    override val KtObjectDeclaration.symbol: KaClassSymbol
        get() = withPsiValidityAssertion {
            if (isObjectLiteral())
                KaFe10PsiAnonymousObjectSymbol(this, analysisContext)
            else
                KaFe10PsiNamedClassSymbol(this, analysisContext)
        }

    override val KtClassOrObject.classSymbol: KaClassSymbol?
        get() = withPsiValidityAssertion {
            when (this) {
                is KtEnumEntry -> null
                is KtObjectDeclaration -> symbol
                else -> KaFe10PsiNamedClassSymbol(this, analysisContext)
            }
        }

    override val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?
        get() = withPsiValidityAssertion {
            if (this is KtEnumEntry || nameIdentifier == null) {
                return null
            }

            KaFe10PsiNamedClassSymbol(this, analysisContext)
        }

    override val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
        get() = withPsiValidityAssertion {
            if (isGetter) {
                KaFe10PsiPropertyGetterSymbol(this, analysisContext)
            } else {
                KaFe10PsiPropertySetterSymbol(this, analysisContext)
            }
        }

    override val KtClassInitializer.symbol: KaClassInitializerSymbol
        get() = withPsiValidityAssertion { KaFe10PsiClassInitializerSymbol(this, analysisContext) }

    override val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
        get() = withPsiValidityAssertion { KaFe10PsiLocalVariableSymbol(this, analysisContext) }

    override val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
        get() = withPsiValidityAssertion { KaFe10PsiDestructuringDeclarationSymbol(this, analysisSession) }

    override fun findClass(classId: ClassId): KaClassSymbol? = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        descriptor.toKaClassSymbol(analysisContext)
    }

    override fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol? = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findTypeAliasAcrossModuleDependencies(classId) ?: return null
        descriptor.toKtClassifierSymbol(analysisContext) as? KaTypeAliasSymbol
    }

    override fun findClassLike(classId: ClassId): KaClassLikeSymbol? = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassifierAcrossModuleDependencies(classId) ?: return null
        return descriptor.toKtClassifierSymbol(analysisContext) as? KaClassLikeSymbol
    }

    override fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> = withValidityAssertion {
        val packageViewDescriptor = analysisContext.resolveSession.moduleDescriptor.getPackage(packageFqName)
        packageViewDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, nameFilter = { it == name })
            .asSequence()
            .filter { it.name == name }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaCallableSymbol }
    }

    override fun findPackage(fqName: FqName): KaPackageSymbol? = withValidityAssertion {
        if (analysisContext.resolveSession.packageFragmentProvider.isEmpty(fqName)) return null
        KaFe10PackageSymbol(fqName, analysisContext)
    }
}