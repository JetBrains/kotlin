/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.DynamicBundle
import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.lang.MetaLanguage
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.FileContextProvider
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.JavaFileCodeStyleFacadeFactory
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.meta.MetaDataContributor
import org.jetbrains.kotlin.cli.jvm.compiler.IdeaExtensionPoints.registerVersionSpecificAppExtensionPoints
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem

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
            synchronized(KotlinCoreEnvironment.APPLICATION_LOCK) {
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

    companion object {
        @Deprecated(
            message = "The `unitTestMode` flag is deprecated in favor of `KotlinCoreApplicationEnvironmentMode` configuration.",
            replaceWith = ReplaceWith("create(parentDisposable, KotlinCoreApplicationEnvironmentMode.fromUnitTestModeFlag(unitTestMode))"),
        )
        fun create(
            parentDisposable: Disposable,
            unitTestMode: Boolean,
        ): KotlinCoreApplicationEnvironment {
            return create(parentDisposable, KotlinCoreApplicationEnvironmentMode.fromUnitTestModeFlag(unitTestMode))
        }

        fun create(
            parentDisposable: Disposable,
            environmentMode: KotlinCoreApplicationEnvironmentMode,
        ): KotlinCoreApplicationEnvironment {
            val environment = KotlinCoreApplicationEnvironment(parentDisposable, environmentMode)
            registerExtensionPoints()
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
    /**
     * We need to remember whether write access is allowed per thread because the application can be shared between multiple concurrent test
     * runs.
     */
    private val isWriteAccessAllowedInThread: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    override fun isUnitTestMode(): Boolean = true

    override fun isWriteAccessAllowed(): Boolean = isWriteAccessAllowedInThread.get()

    override fun runWriteAction(action: Runnable) {
        withWriteAccessAllowedInThread {
            action.run()
        }
    }

    override fun <T : Any?> runWriteAction(computation: Computable<T?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    override fun <T : Any?, E : Throwable?> runWriteAction(computation: ThrowableComputable<T?, E?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    private inline fun <A> withWriteAccessAllowedInThread(action: () -> A): A {
        isWriteAccessAllowedInThread.set(true)
        try {
            return action()
        } finally {
            isWriteAccessAllowedInThread.set(false)
        }
    }
}
