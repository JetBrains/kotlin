/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DynamicFunctionDescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.denotedClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
import org.jetbrains.kotlin.resolve.sam.createSamConstructorFunction
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import java.nio.file.Path
import java.nio.file.Paths

internal class KaFe10SymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFe10Session,
    override val token: KaLifetimeToken
) : KaSessionComponent<KaFe10Session>(), KaSymbolRelationProvider, KaFe10SessionComponent {
    override val KaSymbol.containingSymbol: KaDeclarationSymbol?
        get() = withValidityAssertion {
            if (isTopLevel) {
                return null
            }

            return when (this) {
                is KaBackingFieldSymbol -> owningProperty
                is KaPropertyAccessorSymbol -> {
                    (getDescriptor() as? PropertyAccessorDescriptor)?.correspondingProperty
                        ?.toKtSymbol(analysisContext) as? KaDeclarationSymbol
                }
                else -> {
                    getDescriptor()?.containingDeclaration
                        ?.toKtSymbol(analysisContext) as? KaDeclarationSymbol
                }
            }
        }

    override val KaSymbol.containingFile: KaFileSymbol?
        get() = withValidityAssertion {
            if (this is KaFileSymbol) {
                return null
            }

            // psiBased
            (psi?.containingFile as? KtFile)?.let { ktFile ->
                with(analysisSession) {
                    return ktFile.getFileSymbol()
                }
            }

            // descriptorBased
            val descriptor = computeContainingSymbolOrSelf(this, analysisSession).getDescriptor()
            val ktFile = descriptor?.let(DescriptorToSourceUtils::getContainingFile) ?: return null
            with(analysisSession) {
                return ktFile.getFileSymbol()
            }
        }

    override val KaSymbol.containingModule: KtModule
        get() = withValidityAssertion {
            val descriptor = when (this) {
                is KaValueParameterSymbol -> {
                    val parameterDescriptor = getDescriptor()
                    (parameterDescriptor as? ValueParameterDescriptor)?.containingDeclaration ?: parameterDescriptor
                }
                is KaPropertyAccessorSymbol -> {
                    val accessorDescriptor = getDescriptor()
                    (accessorDescriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: accessorDescriptor
                }
                else -> getDescriptor()
            }

            val symbolPsi = descriptor?.let(DescriptorToSourceUtils::getContainingFile) ?: psi
            if (symbolPsi != null) {
                return analysisSession.getModule(symbolPsi)
            }

            if (descriptor is DescriptorWithContainerSource) {
                return getFakeContainingKtModule(descriptor)
            }

            if (this is KaBackingFieldSymbol) {
                return owningProperty.containingModule
            }

            if (this is KaFe10DynamicFunctionDescValueParameterSymbol) {
                return owner.containingModule
            }

            TODO(this::class.java.name)
        }

    private fun getFakeContainingKtModule(descriptor: DescriptorWithContainerSource): KtModule {
        val library = when (val containerSource = descriptor.containerSource) {
            is JvmPackagePartSource -> containerSource.knownJvmBinaryClass?.containingLibrary
            is KotlinJvmBinarySourceElement -> containerSource.binaryClass.containingLibrary
            else -> {
                when (val containingDeclaration = descriptor.containingDeclaration) {
                    is DescriptorWithContainerSource -> {
                        // Deserialized member
                        return getFakeContainingKtModule(containingDeclaration)
                    }
                    is LazyJavaPackageFragment -> {
                        // Deserialized top-level
                        (containingDeclaration.source as KotlinJvmBinarySourceElement).binaryClass.containingLibrary
                    }
                    else -> null
                }
            }
        } ?: TODO(descriptor::class.java.name)
        val libraryPath = Paths.get(library)
        return object : KtLibraryModule {
            override val libraryName: String = libraryPath.fileName.toString().substringBeforeLast(".")
            override val librarySources: KtLibrarySourceModule? = null
            override fun getBinaryRoots(): Collection<Path> = listOf(libraryPath)
            override val directRegularDependencies: List<KtModule> = emptyList()
            override val directDependsOnDependencies: List<KtModule> = emptyList()
            override val transitiveDependsOnDependencies: List<KtModule> = emptyList()
            override val directFriendDependencies: List<KtModule> = emptyList()
            override val contentScope: GlobalSearchScope = ProjectScope.getLibrariesScope(project)
            override val platform: TargetPlatform
                get() = descriptor.platform!!
            override val project: Project
                get() = analysisSession.analysisContext.resolveSession.project

        }
    }

    override val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
        get() = withValidityAssertion {
            val descriptor = (getSymbolDescriptor(this) as? ClassifierDescriptorWithTypeParameters)?.denotedClassDescriptor
            if (descriptor !is ClassDescriptor || getSingleAbstractMethodOrNull(descriptor) == null) return null

            val constructorDescriptor = createSamConstructorFunction(
                descriptor.containingDeclaration,
                descriptor,
                analysisContext.resolveSession.samConversionResolver,
                JvmSamConversionOracle(analysisContext.resolveSession.languageVersionSettings),
            )

            return KaFe10DescSamConstructorSymbol(constructorDescriptor, analysisContext)
        }

    private val overridesProvider = KaFe10SymbolDeclarationOverridesProvider(analysisSessionProvider, token)

    override val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getDirectlyOverriddenSymbols(this)
        }

    override val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getAllOverriddenSymbols(this)
        }

    override fun KaClassOrObjectSymbol.isSubClassOf(superClass: KaClassOrObjectSymbol): Boolean = withValidityAssertion {
        overridesProvider.isSubClassOf(this, superClass)
    }

    override fun KaClassOrObjectSymbol.isDirectSubClassOf(superClass: KaClassOrObjectSymbol): Boolean = withValidityAssertion {
        overridesProvider.isDirectSubClassOf(this, superClass)
    }

    override val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
        get() = withValidityAssertion {
            throw NotImplementedError("Method is not implemented for FE 1.0")
        }
}

internal fun computeContainingSymbolOrSelf(symbol: KaSymbol, analysisSession: KaSession): KaSymbol {
    with(analysisSession) {
        return when (symbol) {
            is KaValueParameterSymbol -> {
                symbol.containingSymbol as? KaFunctionLikeSymbol ?: symbol
            }
            is KaPropertyAccessorSymbol -> {
                symbol.containingSymbol as? KaPropertySymbol ?: symbol
            }
            is KaBackingFieldSymbol -> symbol.owningProperty
            else -> symbol
        }
    }
}