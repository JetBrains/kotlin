/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.withKaModuleEntry
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleInformationProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirDanglingFileSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

@LLFirInternals
class LLFirSessionCache(
    private val project: Project,
    val storage: LLFirSessionCacheStorage,
) : Disposable {
    constructor(project: Project) : this(
        project,
        LLFirSessionCacheStorage.createEmpty { LLFirSessionCleaner(it.requestedDisposableOrNull) }
    )

    @KaCachedService
    private val moduleInformationProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinModuleInformationProvider.getInstance(project)
    }

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     *
     * Must be called from a read action.
     */
    fun getSession(module: KaModule, preferBinary: Boolean = false): LLFirSession =
        when (module) {
            is KaBuiltinsModule if preferBinary ->
                LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(module.targetPlatform)

            is KaLibraryModule if preferBinary -> getBinaryLibraryCachedSession(module, storage.binaryCache)

            // Fallback dependencies aren't resolvable and thus always binary, regardless of `preferBinary`.
            is KaLibraryFallbackDependenciesModule -> getBinaryLibraryCachedSession(module, storage.libraryFallbackDependenciesCache)

            is KaDanglingFileModule -> getDanglingFileCachedSession(module)
            else -> getCachedSession(module, storage.sourceCache, factory = ::createSession)
        }

    /**
     * Returns the [LLFirSession] for [module], to be used as a *dependency*, or `null` if it doesn't make sense to create such a session as
     * a dependency. This is an optimization for [KaModule]s of certain kinds, like empty modules.
     *
     * Dependency sessions are implicitly binary-preferred because sessions used as dependencies do not need to be resolvable.
     */
    fun getDependencySession(module: KaModule): LLFirSession? {
        if (moduleInformationProvider?.isEmpty(module) == true) return null
        return getSession(module, preferBinary = true)
    }

    private fun getBinaryLibraryCachedSession(module: KaModule, storage: SessionStorage): LLFirSession =
        getCachedSession(module, storage) {
            LLFirSessionFactory(project, module.targetPlatform).createBinaryLibrarySession(module)
        }

    private fun getDanglingFileCachedSession(module: KaDanglingFileModule): LLFirSession {
        if (module.isStable) {
            return getCachedSession(module, storage.danglingFileSessionCache, ::createSession)
        }

        checkCanceled()

        storage.unstableDanglingFileSessionCache[module]
            ?.takeIf { it.isValidDanglingFileSession }
            ?.let { return it }

        // The creation of an unstable dangling file session might require accessing `unstableDanglingFileSessionCache` again to get the
        // context session, so we have to create the session outside `compute` to avoid recursive updates.
        //
        // `newSession` might be thrown away if another thread added its own new session, but the result will be consistent due to
        // `compute`.
        val newSession = createSession(module)
        val session = storage.unstableDanglingFileSessionCache.compute(module) { _, existingSession ->
            if (existingSession?.isValidDanglingFileSession == true) {
                existingSession
            } else {
                newSession
            }
        }

        requireNotNull(session)
        checkSessionValidity(session)
        return session
    }

    private val LLFirSession.isValidDanglingFileSession: Boolean
        get() {
            // For unstable dangling files, the `isValid` check on the session will likely always pass because unstable dangling file
            // sessions are not actively invalidated (`isValid` is written during session invalidation). However, it is important for
            // consistency to check the session's own validity. In the future, session validity might be updated for additional purposes.
            return this is LLFirDanglingFileSession && !hasFileModifications && isValid
        }

    private fun <T : KaModule> getCachedSession(module: T, storage: SessionStorage, factory: (T) -> LLFirSession): LLFirSession {
        checkCanceled()

        val session = if (module.supportsIsolatedSessionCreation) {
            storage.computeIfAbsent(module) { factory(module) }
        } else {
            // Non-isolated session creation may need to access other sessions, so we should create the session outside `computeIfAbsent` to
            // avoid recursive update exceptions.
            storage.getOrPut(module) { factory(module) }
        }

        checkSessionValidity(session)
        return session
    }

    private fun checkSessionValidity(session: LLFirSession) {
        requireWithAttachment(session.isValid, { "A session acquired via `getSession` should always be valid." }) {
            withKaModuleEntry("module", session.ktModule)
            withEntry("invalidationInformation", session.invalidationInformation)
        }
    }

    /**
     * Whether the session for this [KaModule] can be created without getting other sessions from the cache. Should be kept in sync with
     * [createSession].
     */
    private val KaModule.supportsIsolatedSessionCreation: Boolean
        get() = this !is KaDanglingFileModule

    private fun createSession(module: KaModule): LLFirSession {
        val sessionFactory = LLFirSessionFactory(project, module.targetPlatform)
        return when (module) {
            is KaSourceModule -> sessionFactory.createSourcesSession(module)
            is KaBuiltinsModule -> sessionFactory.createResolvableLibrarySession(module)
            is KaLibraryModule -> sessionFactory.createResolvableLibrarySession(module)
            is KaLibrarySourceModule -> sessionFactory.createResolvableLibrarySession(module)
            is KaLibraryFallbackDependenciesModule -> sessionFactory.createBinaryLibrarySession(module)
            is KaScriptModule -> sessionFactory.createScriptSession(module)
            is KaDanglingFileModule -> {
                //  Dangling file context must have an analyzable session, so we can properly compile code against it.
                val contextSession = getSession(module.contextModule, preferBinary = false)
                sessionFactory.createDanglingFileSession(module, contextSession)
            }
            is KaNotUnderContentRootModule -> sessionFactory.createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    override fun dispose() {
    }

    @LLFirInternals
    companion object {
        fun getInstance(project: Project): LLFirSessionCache = project.service()
    }
}

internal fun LLFirSessionConfigurator.Companion.configure(session: LLFirSession) {
    val project = session.project
    for (extension in extensionPointName.getExtensionList(project)) {
        extension.configure(session)
    }
}

@KaImplementationDetail
@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
fun createEmptySession(): FirSession {
    return object : FirSession(Kind.Source) {}.apply {
        val moduleData = FirSourceModuleData(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
