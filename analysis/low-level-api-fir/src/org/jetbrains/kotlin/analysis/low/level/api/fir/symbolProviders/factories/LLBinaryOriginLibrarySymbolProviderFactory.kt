/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getLibraryPathsForVirtualFiles
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLJvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirDelegatingSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.JsFlexibleTypeFactory
import org.jetbrains.kotlin.fir.session.KlibBasedSymbolProvider
import org.jetbrains.kotlin.fir.session.MetadataSymbolProvider
import org.jetbrains.kotlin.fir.session.NativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.WasmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.jetbrains.kotlin.util.Logger as KLogger

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
internal object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLFirSession,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        return listOf(
            LLJvmClassFileBasedSymbolProvider(
                session,
                SingleModuleDataProvider(session.moduleData),
                session.kotlinScopeProvider,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(session.project).create(scope),
                firJavaFacade,
            ),
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: LLFirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
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
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val forwardDeclarationsModuleData = LLNativeForwardDeclarationsModuleData(moduleData.ktModule).apply {
            bindSession(session)
        }

        val kotlinScopeProvider = session.kotlinScopeProvider
        val kLibs = moduleData.getLibraryKLibs()
        return listOfNotNull(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs),
            NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, kLibs),
        )
    }

    /**
     * The module data specifically for [originalModule]'s associated native forward declarations. In particular, this module data is an
     * [LLFirModuleData]-compliant replacement for the [FirBinaryDependenciesModuleData] used on the compiler side (see
     * `FirNativeSessionFactory.createAdditionalDependencyProviders`).
     */
    private class LLNativeForwardDeclarationsModuleData(originalModule: KaModule) : LLFirModuleData(originalModule) {
        override val name: Name
            get() = FORWARD_DECLARATIONS_MODULE_NAME
    }

    override fun createJsLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(
                session,
                moduleDataProvider,
                session.kotlinScopeProvider,
                kLibs,
                flexibleTypeFactory = JsFlexibleTypeFactory(session),
            ),
        )
    }

    override fun createWasmLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(session, moduleDataProvider, session.kotlinScopeProvider, kLibs),
        )
    }

    override fun createBuiltinsSymbolProvider(session: LLFirSession): List<FirSymbolProvider> =
        listOf(
            createFallbackBuiltinsSymbolProvider(session),
            FirBuiltinSyntheticFunctionInterfaceProvider(session, session.moduleData, session.kotlinScopeProvider),
        )

    private fun LLFirModuleData.getLibraryKLibs(): List<KotlinLibrary> {
        val ktLibraryModule = ktModule as? KaLibraryModule ?: return emptyList()

        return getLibraryPathsForVirtualFiles(ktLibraryModule.binaryVirtualFiles)
            .filter { it.isDirectory() || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { it.tryResolveAsKLib() }
    }

    private fun Path.tryResolveAsKLib(): KotlinLibrary? {
        return try {
            val konanFile = File(absolutePathString())
            ToolingSingleFileKlibResolveStrategy.tryResolve(konanFile, IntellijLogBasedLogger)
        } catch (e: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            LOG.warn("Cannot resolve a KLib $this", e)
            null
        }
    }

    private val LOG = Logger.getInstance(LLBinaryOriginLibrarySymbolProviderFactory::class.java)

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

/**
 * Creates a fallback builtins symbol provider for the [session], taking into account its specific target platform.
 *
 * [FirFallbackBuiltinSymbolProvider] includes `kotlin.Cloneable` regardless of the target platform. But `Cloneable` should not be available
 * in non-JVM/Common platforms. As such, we specifically exclude the symbol for other platforms.
 *
 * This is a workaround for the larger problem of platform-specific fallback builtins. For now, we've sourced fallback builtins from the
 * tooling's runtime stdlib regardless of the target platform, so fallback builtins for e.g. Native will be sourced from a JAR. `Cloneable`
 * is a specific visible symptom of this, but might not be the only problem. See KT-79930.
 */
private fun createFallbackBuiltinsSymbolProvider(session: LLFirSession): FirSymbolProvider {
    val targetPlatform = session.moduleData.platform
    val isCloneableAvailable = when {
        targetPlatform.all { it is JvmPlatform } -> true
        targetPlatform.all { it is NativePlatform } -> false
        targetPlatform.all { it is JsPlatform } -> false
        targetPlatform.all { it is WasmPlatform } -> false
        else -> true // Common
    }

    val baseProvider = FirFallbackBuiltinSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
    return if (!isCloneableAvailable) {
        LLCloneableExcludingSymbolProvider(baseProvider)
    } else {
        baseProvider
    }
}

private class LLCloneableExcludingSymbolProvider(delegate: FirSymbolProvider) : FirDelegatingSymbolProvider(delegate) {
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId == StandardClassIds.Cloneable) return null
        return super.getClassLikeSymbolByClassId(classId)
    }
}
