/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.LLSharedCacheLocks
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.KlibBasedSymbolProvider
import org.jetbrains.kotlin.fir.session.MetadataSymbolProvider
import org.jetbrains.kotlin.fir.session.NativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.jetbrains.kotlin.util.Logger as KLogger

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
class LLBinaryOriginLibrarySymbolProviderFactory(private val project: Project) : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            JvmClassFileBasedSymbolProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(project).create(scope),
                firJavaFacade,
                sharedClassComputationLock = LLSharedCacheLocks.sharedJavaClassComputationLock,
            )
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return buildList {
            add(
                MetadataSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider as PackageAndMetadataPartProvider,
                    VirtualFileFinderFactory.getInstance(project).create(scope),
                )
            )
            val kLibs = moduleData.getLibraryKLibs()
            if (kLibs.isNotEmpty()) {
                add(KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs))
            }
        }
    }

    override fun createNativeLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
            FORWARD_DECLARATIONS_MODULE_NAME,
            moduleDataProvider.platform,
        ).apply {
            bindSession(session)
        }
        val kLibs = moduleData.getLibraryKLibs()
        return listOfNotNull(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs),
            NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, kLibs),
        )
    }

    override fun createJsLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs)
        )
    }

    override fun createBuiltinsSymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
    ): List<FirSymbolProvider> {
        return listOf(
            FirFallbackBuiltinSymbolProvider(session, moduleData, kotlinScopeProvider),
            FirBuiltinSyntheticFunctionInterfaceProvider.initialize(session, moduleData, kotlinScopeProvider)
        )
    }

    private fun LLFirModuleData.getLibraryKLibs(): List<KotlinLibrary> {
        val ktLibraryModule = ktModule as? KaLibraryModule ?: return emptyList()

        return ktLibraryModule.binaryRoots
            .filter { it.isDirectory() || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { it.tryResolveAsKLib() }
    }

    private fun Path.tryResolveAsKLib(): KotlinLibrary? {
        return try {
            val konanFile = File(absolutePathString())
            ToolingSingleFileKlibResolveStrategy.tryResolve(konanFile, IntellijLogBasedLogger)
        } catch (e: Exception) {
            LOG.warn("Cannot resolve a KLib $this", e)
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(LLBinaryOriginLibrarySymbolProviderFactory::class.java)
    }

    private object IntellijLogBasedLogger : KLogger {
        override fun log(message: String) {
            LOG.info(message)
        }

        override fun error(message: String) {
            LOG.error(message)
        }

        override fun warning(message: String) {
            LOG.warn(message)
        }

        @Deprecated(KLogger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(KLogger.FATAL_REPLACEMENT))
        override fun fatal(message: String): Nothing {
            throw IllegalStateException(message)
        }
    }
}

