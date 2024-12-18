/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DynamicFunctionDescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.denotedClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.multiplatform.isCompatibleOrWeaklyIncompatible
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.sam.createSamConstructorFunction
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.util.ImplementationStatus
import java.nio.file.Path
import java.nio.file.Paths

internal class KaFe10SymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaSymbolRelationProvider, KaFe10SessionComponent {
    override val KaSymbol.containingSymbol: KaSymbol?
        get() = withValidityAssertion {
            when (this) {
                is KaPackageSymbol -> null
                is KaFileSymbol -> KaFe10PackageSymbol(kotlinPackageFqn, analysisContext)
                else -> containingDeclaration ?: containingFile
            }
        }

    override val KaSymbol.containingDeclaration: KaDeclarationSymbol?
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
                    return ktFile.symbol
                }
            }

            // descriptorBased
            val descriptor = computeContainingSymbolOrSelf(this, analysisSession).getDescriptor()
            val ktFile = descriptor?.let(DescriptorToSourceUtils::getContainingFile) ?: return null
            with(analysisSession) {
                return ktFile.symbol
            }
        }

    override val KaSymbol.containingModule: KaModule
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

    private fun getFakeContainingKtModule(descriptor: DescriptorWithContainerSource): KaModule {
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
        return object : KaLibraryModule {
            override val libraryName: String = libraryPath.fileName.toString().substringBeforeLast(".")
            override val librarySources: KaLibrarySourceModule? = null
            override val isSdk: Boolean = false
            override val binaryRoots: Collection<Path> = listOf(libraryPath)

            @KaExperimentalApi
            override val binaryVirtualFiles: Collection<VirtualFile> = emptyList()
            override val directRegularDependencies: List<KaModule> = emptyList()
            override val directDependsOnDependencies: List<KaModule> = emptyList()
            override val transitiveDependsOnDependencies: List<KaModule> = emptyList()
            override val directFriendDependencies: List<KaModule> = emptyList()
            override val contentScope: GlobalSearchScope = ProjectScope.getLibrariesScope(project)
            override val targetPlatform: TargetPlatform
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

    override val KaSamConstructorSymbol.constructedClass: KaClassLikeSymbol
        get() = withValidityAssertion {
            (getDescriptor() as SamConstructorDescriptor).baseDescriptorForSynthetic.toKaClassSymbol(analysisContext)
        }

    @KaExperimentalApi
    override val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?
        get() = withValidityAssertion { null }

    private val overridesProvider = KaFe10SymbolDeclarationOverridesProvider(analysisSessionProvider)

    override val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getDirectlyOverriddenSymbols(this)
        }

    override val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getAllOverriddenSymbols(this)
        }

    override fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean = withValidityAssertion {
        overridesProvider.isSubClassOf(this, superClass)
    }

    override fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean = withValidityAssertion {
        overridesProvider.isDirectSubClassOf(this, superClass)
    }

    override val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
        get() = withValidityAssertion {
            throw NotImplementedError("Method is not implemented for FE 1.0")
        }

    override fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus? {
        withValidityAssertion {
            throw NotImplementedError("Method is not implemented for FE 1.0")
        }
    }

    override val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol
        get() = withValidityAssertion {
            val callableDescriptor = getSymbolDescriptor(this) as? CallableMemberDescriptor ?: return this
            val originalCallableDescriptor = callableDescriptor.findOriginalTopMostOverriddenDescriptors().firstOrNull() ?: return this
            return originalCallableDescriptor.toKtCallableSymbol(analysisContext) ?: this
        }

    override fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> = withValidityAssertion {
        if (psiSafe<KtDeclaration>()?.hasActualModifier() != true) return emptyList()
        val memberDescriptor = (getSymbolDescriptor(this) as? MemberDescriptor)?.takeIf { it.isActual } ?: return emptyList()

        return ExpectedActualResolver.findExpectedForActual(memberDescriptor).orEmpty().asSequence()
            .filter { it.key.isCompatibleOrWeaklyIncompatible }
            .flatMap { it.value }
            .map { it.toKtSymbol(analysisContext) as KaDeclarationSymbol }
            .toList()
    }

    override val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
        get() = withValidityAssertion {
            val classDescriptor = getSymbolDescriptor(this) as? ClassDescriptor ?: return emptyList()

            val inheritorsProvider = analysisContext.resolveSession.sealedClassInheritorsProvider
            val allowInDifferentFiles = analysisContext.resolveSession.languageVersionSettings
                .supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage)

            return inheritorsProvider.computeSealedSubclasses(classDescriptor, allowInDifferentFiles)
                .mapNotNull { it.toKtClassifierSymbol(analysisContext) as? KaNamedClassSymbol }
        }
}

internal fun computeContainingSymbolOrSelf(symbol: KaSymbol, analysisSession: KaSession): KaSymbol {
    with(analysisSession) {
        return when (symbol) {
            is KaValueParameterSymbol -> {
                symbol.containingDeclaration as? KaFunctionSymbol ?: symbol
            }
            is KaPropertyAccessorSymbol -> {
                symbol.containingDeclaration as? KaPropertySymbol ?: symbol
            }
            is KaBackingFieldSymbol -> symbol.owningProperty
            else -> symbol
        }
    }
}