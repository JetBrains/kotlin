/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.components.KaSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DynamicFunctionDescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import java.nio.file.Path
import java.nio.file.Paths

internal class KaFe10SymbolContainingDeclarationProvider(
    override val analysisSession: KaFe10Session
) : KaSymbolContainingDeclarationProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getContainingDeclaration(symbol: KaSymbol): KaDeclarationSymbol? {
        if (symbol is KaSymbolWithKind && symbol.symbolKind == KaSymbolKind.TOP_LEVEL) {
            return null
        }

        return when (symbol) {
            is KaBackingFieldSymbol -> symbol.owningProperty
            is KaPropertyAccessorSymbol -> {
                (symbol.getDescriptor() as? PropertyAccessorDescriptor)?.correspondingProperty
                    ?.toKtSymbol(analysisContext) as? KaDeclarationSymbol
            }
            else -> {
                symbol.getDescriptor()?.containingDeclaration
                    ?.toKtSymbol(analysisContext) as? KaDeclarationSymbol
            }
        }
    }

    private val KaSymbol.containingSymbolOrSelf: KaSymbol
        get() {
            return when (this) {
                is KaValueParameterSymbol -> {
                    getContainingDeclaration(this) as? KaFunctionLikeSymbol ?: this
                }
                is KaPropertyAccessorSymbol -> {
                    getContainingDeclaration(this) as? KaPropertySymbol ?: this
                }
                is KaBackingFieldSymbol -> this.owningProperty
                else -> this
            }
        }

    override fun getContainingFileSymbol(symbol: KaSymbol): KaFileSymbol? {
        if (symbol is KaFileSymbol) return null
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

    override fun getContainingJvmClassName(symbol: KaCallableSymbol): String? {
        val platform = getContainingModule(symbol).platform
        if (!platform.has<JvmPlatform>()) return null

        val containingSymbolOrSelf = symbol.containingSymbolOrSelf as KaSymbolWithKind
        return when (val descriptor = containingSymbolOrSelf.getDescriptor()) {
            is DescriptorWithContainerSource -> {
                when (val containerSource = descriptor.containerSource) {
                    is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
                    is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
                    else -> null
                }?.fqNameForClassNameWithoutDollars?.asString()
            }
            else -> {
                return if (containingSymbolOrSelf.symbolKind == KaSymbolKind.TOP_LEVEL) {
                    descriptor?.let(DescriptorToSourceUtils::getContainingFile)
                        ?.takeUnless { it.isScript() }
                        ?.javaFileFacadeFqName?.asString()
                } else {
                    val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                        ?: (containingSymbolOrSelf as? KaCallableSymbol)?.callableId?.classId
                    classId?.takeUnless { it.shortClassName.isSpecial }
                        ?.asFqNameString()
                }
            }
        }
    }

    // TODO this is a dummy and incorrect implementation just to satisfy some tests
    override fun getContainingModule(symbol: KaSymbol): KtModule {
        val descriptor = when (symbol) {
            is KaValueParameterSymbol -> {
                val paramDescriptor = symbol.getDescriptor()
                (paramDescriptor as? ValueParameterDescriptor)?.containingDeclaration ?: paramDescriptor
            }
            is KaPropertyAccessorSymbol -> {
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

        if (symbol is KaBackingFieldSymbol) {
            return getContainingModule(symbol.owningProperty)
        }

        if (symbol is KaFe10DynamicFunctionDescValueParameterSymbol) {
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
            override val project: Project
                get() = analysisSession.analysisContext.resolveSession.project

        }
    }
}
