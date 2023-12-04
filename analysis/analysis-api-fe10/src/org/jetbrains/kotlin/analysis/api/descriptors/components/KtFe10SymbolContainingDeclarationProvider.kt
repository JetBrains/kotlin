/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DynamicFunctionDescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import java.nio.file.Path
import java.nio.file.Paths

internal class KtFe10SymbolContainingDeclarationProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolContainingDeclarationProvider(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun getContainingDeclaration(symbol: KtSymbol): KtDeclarationSymbol? {
        if (symbol is KtSymbolWithKind && symbol.symbolKind == KtSymbolKind.TOP_LEVEL) {
            return null
        }

        return when (symbol) {
            is KtBackingFieldSymbol -> symbol.owningProperty
            is KtPropertyAccessorSymbol -> {
                (symbol.getDescriptor() as? PropertyAccessorDescriptor)?.correspondingProperty
                    ?.toKtSymbol(analysisContext) as? KtDeclarationSymbol
            }
            else -> {
                symbol.getDescriptor()?.containingDeclaration
                    ?.toKtSymbol(analysisContext) as? KtDeclarationSymbol
            }
        }
    }

    private val KtSymbol.containingSymbolOrSelf: KtSymbol
        get() {
            return when (this) {
                is KtValueParameterSymbol -> {
                    getContainingDeclaration(this) as? KtFunctionLikeSymbol ?: this
                }
                is KtPropertyAccessorSymbol -> {
                    getContainingDeclaration(this) as? KtPropertySymbol ?: this
                }
                is KtBackingFieldSymbol -> this.owningProperty
                else -> this
            }
        }

    override fun getContainingFileSymbol(symbol: KtSymbol): KtFileSymbol? {
        if (symbol is KtFileSymbol) return null
        // psiBased
        (symbol.psi?.containingFile as? KtFile)?.let { ktFile ->
            with(analysisSession) {
                return ktFile.getFileSymbol()
            }
        }
        // descriptorBased
        val descriptor = symbol.containingSymbolOrSelf.getDescriptor()
        val ktFile = descriptor?.let(DescriptorToSourceUtils::getContainingFile) ?: return null
        with(analysisSession) {
            return ktFile.getFileSymbol()
        }
    }

    override fun getContainingJvmClassName(symbol: KtCallableSymbol): String? {
        val platform = getContainingModule(symbol).platform
        if (!platform.has<JvmPlatform>()) return null

        val containingSymbolOrSelf = symbol.containingSymbolOrSelf as KtSymbolWithKind
        return when (val descriptor = containingSymbolOrSelf.getDescriptor()) {
            is DescriptorWithContainerSource -> {
                when (val containerSource = descriptor.containerSource) {
                    is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
                    is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
                    else -> null
                }?.fqNameForClassNameWithoutDollars?.asString()
            }
            else -> {
                return if (containingSymbolOrSelf.symbolKind == KtSymbolKind.TOP_LEVEL) {
                    descriptor?.let(DescriptorToSourceUtils::getContainingFile)
                        ?.takeUnless { it.isScript() }
                        ?.javaFileFacadeFqName?.asString()
                } else {
                    val classId = (containingSymbolOrSelf as? KtConstructorSymbol)?.containingClassIdIfNonLocal
                        ?: (containingSymbolOrSelf as? KtCallableSymbol)?.callableIdIfNonLocal?.classId
                    classId?.takeUnless { it.shortClassName.isSpecial }
                        ?.asFqNameString()
                }
            }
        }
    }

    // TODO this is a dummy and incorrect implementation just to satisfy some tests
    override fun getContainingModule(symbol: KtSymbol): KtModule {
        val descriptor = when (symbol) {
            is KtValueParameterSymbol -> {
                val paramDescriptor = symbol.getDescriptor()
                (paramDescriptor as? ValueParameterDescriptor)?.containingDeclaration ?: paramDescriptor
            }
            is KtPropertyAccessorSymbol -> {
                val accessorDescriptor = symbol.getDescriptor()
                (accessorDescriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: accessorDescriptor
            }
            else ->
                symbol.getDescriptor()
        }

        val symbolPsi = descriptor?.let(DescriptorToSourceUtils::getContainingFile) ?: symbol.psi
        if (symbolPsi != null) {
            return analysisSession.getModule(symbolPsi)
        }

        if (descriptor is DescriptorWithContainerSource) {
            return getFakeContainingKtModule(descriptor)
        }

        if (symbol is KtBackingFieldSymbol) {
            return getContainingModule(symbol.owningProperty)
        }

        if (symbol is KtFe10DynamicFunctionDescValueParameterSymbol) {
            return getContainingModule(symbol.owner)
        }

        TODO(symbol::class.java.name)
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
            override val analyzerServices: PlatformDependentAnalyzerServices
                get() = JvmPlatformAnalyzerServices
            override val project: Project
                get() = analysisSession.analysisContext.resolveSession.project

        }
    }
}
