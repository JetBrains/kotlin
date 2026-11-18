/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage", "DEPRECATION")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.lang.MetaLanguage
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.pom.java.InternalPersistentJavaLanguageLevelReaderService
import com.intellij.psi.JavaModuleGraphHelper
import com.intellij.psi.JavaModuleSystem
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.JavaFileCodeStyleFacadeFactory
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.util.JavaClassSupers
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

open class KotlinCoreApplicationEnvironment protected constructor(
    parentDisposable: Disposable,
    unitTestMode: Boolean,
) : JavaCoreApplicationEnvironment(parentDisposable, unitTestMode) {
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

        @Volatile
        @CoreEnvironmentDeprecation
        var instance: KotlinCoreApplicationEnvironment? = null
            private set

        private var ourProjectCount = 0

        val environmentFactoryForProduction: (Disposable) -> KotlinCoreApplicationEnvironment get() = {
            KotlinCoreApplicationEnvironment(it, unitTestMode = false)
        }

        @CoreEnvironmentDeprecation
        fun getOrCreateApplicationEnvironment(
            projectDisposable: Disposable,
            environmentFactory: (Disposable) -> KotlinCoreApplicationEnvironment = environmentFactoryForProduction,
        ): KotlinCoreApplicationEnvironment {
            underApplicationLock {
                if (instance == null) {
                    val disposable = Disposer.newDisposable("Disposable for the KotlinCoreApplicationEnvironment")
                    instance = create(disposable, environmentFactory)
                    ourProjectCount = 0
                    Disposer.register(disposable) {
                        underApplicationLock {
                            instance = null
                        }
                    }
                }
                try {
                    val disposeAppEnv =
                        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value.toBooleanLenient() != true
                    // Disposer uses identity of passed object to deduplicate registered disposables
                    // We should everytime pass new instance to avoid un-registering from previous one
                    @Suppress("ObjectLiteralToLambda")
                    Disposer.register(projectDisposable, object : Disposable {
                        override fun dispose() {
                            underApplicationLock {
                                // Build-systems may run many instances of the compiler in parallel
                                // All projects share the same ApplicationEnvironment, and when the last project is disposed,
                                // the ApplicationEnvironment is disposed as well
                                if (--ourProjectCount <= 0) {
                                    // Do not use this property unless you sure need it, causes Application to MEMORY LEAK
                                    // Only valid use-case is when Application should be cached to avoid
                                    // initialization costs
                                    if (disposeAppEnv) {
                                        disposeApplicationEnvironment()
                                    } else {
                                        instance?.idleCleanup()
                                    }
                                }
                            }
                        }
                    })
                } finally {
                    ourProjectCount++
                }

                return instance!!
            }
        }


        fun create(
            parentDisposable: Disposable,
            environmentFactory: (Disposable) -> KotlinCoreApplicationEnvironment = environmentFactoryForProduction,
        ): KotlinCoreApplicationEnvironment {
            val environment = environmentFactory(parentDisposable)
            registerExtensionPoints()
            registerApplicationServicesForCLI(environment)
            registerApplicationServices(environment)
            return environment
        }

        @Suppress("UnstableApiUsage")
        private fun registerExtensionPoints() {
            registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java)
            registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage::class.java)
            registerApplicationExtensionPoint(ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy::class.java)
            registerApplicationExtensionPoint(JavaModuleSystem.EP_NAME, JavaModuleSystem::class.java)
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

        /**
         * This method is also used in Gradle after configuration phase finished.
         */
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun disposeApplicationEnvironment() {
            underApplicationLock {
                val environment = instance ?: return
                instance = null
                environment.dispose()
            }
        }
    }
}
