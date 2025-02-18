/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.JsFlexibleTypeFactory
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
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.jetbrains.kotlin.util.Logger as KLogger

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLFirSession,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            JvmClassFileBasedSymbolProvider(
                session,
                SingleModuleDataProvider(session.moduleData),
                session.kotlinScopeProvider,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(session.project).create(scope),
                firJavaFacade
            )
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: LLFirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kotlinScopeProvider = session.kotlinScopeProvider
        return buildList {
            add(
                MetadataSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider as PackageAndMetadataPartProvider,
                    VirtualFileFinderFactory.getInstance(session.project).create(scope),
                )
            )

            val kLibs = moduleData.getLibraryKLibs()
            if (kLibs.isNotEmpty()) {
                add(KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs))
            }
        }
    }

    override fun createNativeLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
            FORWARD_DECLARATIONS_MODULE_NAME,
            moduleDataProvider.platform,
        ).apply {
            bindSession(session)
        }

        val kotlinScopeProvider = session.kotlinScopeProvider
        val kLibs = moduleData.getLibraryKLibs()
        return listOfNotNull(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs),
            NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, kLibs),
        )
    }

    override fun createJsLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(
                session, moduleDataProvider, session.kotlinScopeProvider, kLibs,
                flexibleTypeFactory = JsFlexibleTypeFactory(session),
            )
        )
    }

    override fun createWasmLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(session, moduleDataProvider, session.kotlinScopeProvider, kLibs)
        )
    }

    override fun createBuiltinsSymbolProvider(session: LLFirSession): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val kotlinScopeProvider = session.kotlinScopeProvider
        return listOf(
            FirFallbackBuiltinSymbolProvider(session, moduleData, kotlinScopeProvider),
            FirBuiltinSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider)
        )
    }

    fun LLFirModuleData.getLibraryKLibs(): List<KotlinLibrary> {
        val ktLibraryModule = ktModule as? KaLibraryModule ?: return emptyList()

        return ktLibraryModule.binaryRoots
            .filter { it.isDirectory() || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { it.tryResolveAsKLib() }
    }

    fun Path.tryResolveAsKLib(): KotlinLibrary? {
        return try {
            val konanFile = File(absolutePathString())
            ToolingSingleFileKlibResolveStrategy.tryResolve(konanFile, IntellijLogBasedLogger)
        } catch (e: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            LOG.warn("Cannot resolve a KLib $this", e)
            null
        }
    }

    val LOG = Logger.getInstance(LLBinaryOriginLibrarySymbolProviderFactory::class.java)

    object IntellijLogBasedLogger : KLogger {
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

