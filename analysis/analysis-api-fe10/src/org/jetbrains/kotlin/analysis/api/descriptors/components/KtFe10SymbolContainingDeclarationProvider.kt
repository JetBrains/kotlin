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
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescDefaultBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.descriptorUtil.platform
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
            else -> symbol.getDescriptor()?.containingDeclaration?.toKtSymbol(analysisContext) as? KtDeclarationSymbol
        }
    }

    // TODO this is a dummy and incorrect implementation just to satisfy some tests
    override fun getContainingModule(symbol: KtSymbol): KtModule {
        val descriptor = symbol.getDescriptor()

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

        TODO(symbol::class.java.name)
    }

    private fun getFakeContainingKtModule(descriptor: DescriptorWithContainerSource): KtModule {
        val libraryPath = Paths.get((descriptor.containerSource as JvmPackagePartSource).knownJvmBinaryClass?.containingLibrary!!)
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
