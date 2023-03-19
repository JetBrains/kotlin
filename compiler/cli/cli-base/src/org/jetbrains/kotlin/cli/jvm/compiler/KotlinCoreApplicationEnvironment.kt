/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.DynamicBundle
import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.lang.MetaLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.FileContextProvider
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.meta.MetaDataContributor
import org.jetbrains.kotlin.cli.jvm.compiler.IdeaExtensionPoints.registerVersionSpecificAppExtensionPoints
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem

class KotlinCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable, unitTestMode: Boolean
) :
    JavaCoreApplicationEnvironment(parentDisposable, unitTestMode) {

    init {
        registerFileType(JavaClassFileType.INSTANCE, "sig");
    }

    override fun createJrtFileSystem(): VirtualFileSystem {
        return CoreJrtFileSystem()
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
        fun create(
            parentDisposable: Disposable, unitTestMode: Boolean
        ): KotlinCoreApplicationEnvironment {
            val environment = KotlinCoreApplicationEnvironment(parentDisposable, unitTestMode)
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
