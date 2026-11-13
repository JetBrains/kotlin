/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.mock.MockApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.BinaryLightVirtualFile
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.BuiltinsVirtualFileProviderCliImpl
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtPlatformInterface

/**
 * In production (IDE & Standalone modes), builtins usually reside in the
 * stdlib, which is bundled into the IDE/Standalone mode application. The
 * JVM stdlib used in the project being analyzed by the Analysis API is a
 * different stdlib.
 *
 * This class allows providing different implementations
 * for stdlib builtins and builtins from [org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule]
 * For `KaBuiltinsModule`, we now create VirtualFiles in the air. This allows us to
 * decompile our builtins in both cases differently. See KT-61757 for the context.
 */
@OptIn(KtPlatformInterface::class)
internal class BuiltinsVirtualFileProviderTestImpl() : BuiltinsVirtualFileProvider() {
    private val coreVirtualFileProvider = BuiltinsVirtualFileProviderCliImpl()

    private val files by lazy {
        coreVirtualFileProvider.getBuiltinVirtualFiles().mapTo(mutableSetOf()) { file ->
            BinaryLightVirtualFile(file.name, file.contentsToByteArray())
        }
    }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun createBuiltinsScope(project: Project): GlobalSearchScope =
        GlobalSearchScope.filesScope(project, files)
}

/**
 * Installs [BuiltinsVirtualFileProviderTestImpl] as the application service for the built-ins, in place of
 * the CLI implementation from `analysis-api-standalone-fir.xml`.
 *
 * On the test classpath, the built-ins resources sit in the same `kotlin-stdlib` jar that test modules also
 * depend on as a binary root. The CLI provider resolves the built-ins to a classpath VirtualFile that then
 * collides with the dependency-mounted one, so the same built-ins file gets decompiled into two PSI trees,
 * and PSI elements from different trees are mutually invalid. The detached provider serves the same
 * built-ins content from in-memory files instead, which cannot collide with the mounted jar.
 *
 * A plain `registerService` call is not enough here: the XML descriptor registers the CLI implementation as
 * a service implementation class, and the container keeps constructing it on `getService`, so the adapter
 * must be unregistered first (the same pattern as the IJPL-237691 workaround in `FirStandaloneServiceRegistrar`).
 *
 * This must be called from a service registrar's `registerApplicationServices` that runs after
 * `FirStandaloneServiceRegistrar` (which loads the XML descriptor), such as
 * `AnalysisApiFirTestServiceRegistrar`. `ApplicationServiceRegistration` guarantees such a registrar runs
 * exactly once per shared application, under its write lock and before any test creates modules, so the
 * service swap cannot race with the parallel tests sharing the application (see KT-64167).
 *
 * A single provider per application also keeps the detached built-ins files stable across tests, which lets
 * the application-level KotlinStandaloneIndexCache share them between tests.
 */
@OptIn(KtPlatformInterface::class)
fun installDetachedBuiltinsProvider(application: MockApplication) {
    KotlinCoreEnvironment.underApplicationLock {
        if (application.getServiceIfCreated(BuiltinsVirtualFileProvider::class.java) is BuiltinsVirtualFileProviderTestImpl) {
            return@underApplicationLock
        }
        application.picoContainer.unregisterComponent(BuiltinsVirtualFileProvider::class.java.name)
        application.registerService(BuiltinsVirtualFileProvider::class.java, BuiltinsVirtualFileProviderTestImpl())
    }
}


