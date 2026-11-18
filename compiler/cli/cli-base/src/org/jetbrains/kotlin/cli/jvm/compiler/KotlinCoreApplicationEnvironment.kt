/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage", "DEPRECATION")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.DynamicBundle
import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.lang.MetaLanguage
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.pom.java.InternalPersistentJavaLanguageLevelReaderService
import com.intellij.psi.FileContextProvider
import com.intellij.psi.JavaModuleGraphHelper
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.JavaFileCodeStyleFacadeFactory
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.util.JavaClassSupers
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.IdeaExtensionPoints.registerVersionSpecificAppExtensionPoints
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

sealed interface KotlinCoreApplicationEnvironmentMode {
    object Production : KotlinCoreApplicationEnvironmentMode

    object UnitTest : KotlinCoreApplicationEnvironmentMode

    companion object {
        fun fromUnitTestModeFlag(isUnitTestMode: Boolean): KotlinCoreApplicationEnvironmentMode =
            if (isUnitTestMode) UnitTest else Production
    }
}

class KotlinCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable,
    environmentMode: KotlinCoreApplicationEnvironmentMode,
) : JavaCoreApplicationEnvironment(parentDisposable, environmentMode == KotlinCoreApplicationEnvironmentMode.UnitTest) {

    init {
        registerApplicationService(JavaFileCodeStyleFacadeFactory::class.java, DummyJavaFileCodeStyleFacadeFactory())
        if (application.getService(JavaModuleGraphHelper::class.java) == null) {
            registerApplicationService(JavaModuleGraphHelper::class.java, DumbJavaModuleGraphHelper())
        }
        registerFileType(JavaClassFileType.INSTANCE, "sig")
    }

    override fun createJrtFileSystem(): VirtualFileSystem {
        return CoreJrtFileSystem()
    }

    override fun createApplication(parentDisposable: Disposable): MockApplication {
        val mock = super.createApplication(parentDisposable)

        /**
         * We can't use [environmentMode] from the constructor to decide whether we're in unit test mode, because the corresponding property
         * is not yet initialized when this function is called from the superclass constructor.
         */
        return if (mock.isUnitTestMode) {
            KotlinCoreUnitTestApplication(parentDisposable)
        } else {
            mock
        }
    }

    private var fastJarFileSystemField: FastJarFileSystem? = null
    private var fastJarFileSystemFieldInitialized = false

    val fastJarFileSystem: FastJarFileSystem?
        get() {
            underApplicationLock {
                if (!fastJarFileSystemFieldInitialized) {

                    // may return null e.g. on the old JDKs, therefore fastJarFileSystemFieldInitialized flag is needed
                    fastJarFileSystemField = FastJarFileSystem.createIfUnmappingPossible()?.also {
                        Disposer.register(parentDisposable) {
                            it.clearHandlersCache()
                        }
                    }
                    fastJarFileSystemFieldInitialized = true
                }
                return fastJarFileSystemField
            }
        }

    fun idleCleanup() {
        fastJarFileSystemField?.clearHandlersCache()
    }

    @CoreEnvironmentDeprecation
    fun dispose() {
        Disposer.dispose(parentDisposable)
        resetApplicationManager(application)
        ZipHandler.clearFileAccessorCache()
    }

    override fun createLocalFileSystem(): KotlinLocalFileSystem {
        return KotlinLocalFileSystem()
    }

    companion object {
        @PublishedApi
        internal val APPLICATION_LOCK = Any()

        inline fun <R> underApplicationLock(action: () -> R): R =
            synchronized(APPLICATION_LOCK) { action() }

        fun create(
            parentDisposable: Disposable,
            environmentMode: KotlinCoreApplicationEnvironmentMode,
        ): KotlinCoreApplicationEnvironment {
            val environment = KotlinCoreApplicationEnvironment(parentDisposable, environmentMode)
            registerExtensionPoints()
            registerApplicationServicesForCLI(environment)
            registerApplicationServices(environment)
            return environment
        }

        @Suppress("UnstableApiUsage")
        private fun registerExtensionPoints() {
            registerApplicationExtensionPoint(DynamicBundle.LanguageBundleEP.EP_NAME, DynamicBundle.LanguageBundleEP::class.java)
            registerApplicationExtensionPoint(FileContextProvider.EP_NAME, FileContextProvider::class.java)
            registerApplicationExtensionPoint(MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
            registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java)
            registerApplicationExtensionPoint(JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class.java)
            registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider::class.java)
            registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage::class.java)
            registerApplicationExtensionPoint(SmartPointerAnchorProvider.EP_NAME, SmartPointerAnchorProvider::class.java)
            registerVersionSpecificAppExtensionPoints(ApplicationManager.getApplication().extensionArea)
        }

        private fun registerApplicationServicesForCLI(applicationEnvironment: KotlinCoreApplicationEnvironment) {
            // ability to get text from annotations xml files
            applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml")
            applicationEnvironment.registerParserDefinition(JavaParserDefinition())
        }

        private fun registerApplicationServices(applicationEnvironment: KotlinCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
                registerParserDefinition(KotlinParserDefinition())
                application.registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
                application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
                application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
                application.registerService(VirtualFileSetFactory::class.java, KotlinCoreVirtualFileSetFactory)
                application.registerService(InternalPersistentJavaLanguageLevelReaderService::class.java, InternalPersistentJavaLanguageLevelReaderService.DefaultImpl())
            }
        }

        /**
         * Resets the application managed by [ApplicationManager] to `null`. If [applicationToReset] is specified, [resetApplicationManager]
         * will only reset the application if it's the expected one. Otherwise, the application will already have been changed to another
         * application. For example, application disposal can trigger one of the disposables registered via
         * [ApplicationManager.setApplication], which reset the managed application to the previous application.
         */
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun resetApplicationManager(applicationToReset: Application? = null) {
            val currentApplication = ApplicationManager.getApplication() ?: return
            if (applicationToReset != null && applicationToReset != currentApplication) {
                return
            }

            try {
                val ourApplicationField = ApplicationManager::class.java.getDeclaredField("ourApplication")
                ourApplicationField.isAccessible = true
                ourApplicationField.set(null, null)
            } catch (exception: Exception) {
                // Resetting the application manager is not critical in a production context. If the reflective access fails, we shouldn't
                // expose the user to the failure.
                if (currentApplication.isUnitTestMode) {
                    throw exception
                }
            }
        }
    }
}

/**
 * A [MockApplication] which allows write actions only in [runWriteAction] blocks.
 *
 * The Analysis API is usually not allowed to be used from a write action. To properly support Analysis API tests, the application should
 * not return `true` for [isWriteAccessAllowed] indiscriminately like [MockApplication]. On the other hand, we have some tests which require
 * write access, but don't access the Analysis API. Hence, we remember which threads have started a write action with [runWriteAction].
 */
private class KotlinCoreUnitTestApplication(parentDisposable: Disposable) : MockApplication(parentDisposable) {
    override fun isUnitTestMode(): Boolean = true

    override fun isWriteAccessAllowed(): Boolean = PlatformWriteAccessSupport.isWriteAccessAllowed()

    override fun runWriteAction(action: Runnable) {
        withWriteAccessAllowedInThread {
            action.run()
        }
    }

    override fun <T : Any?> runWriteAction(computation: Computable<T?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    override fun <T : Any?, E : Throwable?> runWriteAction(computation: ThrowableComputable<T?, E?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    private inline fun <A> withWriteAccessAllowedInThread(action: () -> A): A = PlatformWriteAccessSupport.withWriteAccessAllowedInThread(action)
}

/**
 * This object is required because write access may be requested during the entire tree disposal.
 * Thus, if at that moment the application was already disposed, the information about requested write access may be lost.
 */
private object PlatformWriteAccessSupport {
    /**
     * We need to remember whether write access is allowed per thread because the application can be shared between multiple concurrent test
     * runs.
     */
    private val isWriteAccessAllowedInThread: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    fun isWriteAccessAllowed(): Boolean = isWriteAccessAllowedInThread.get()

    inline fun <A> withWriteAccessAllowedInThread(action: () -> A): A {
        isWriteAccessAllowedInThread.set(true)
        try {
            return action()
        } finally {
            isWriteAccessAllowedInThread.set(false)
        }
    }
}
