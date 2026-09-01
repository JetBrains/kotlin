/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtPlatformInterface

/**
 * Provides the `.kotlin_builtins` files that back the *fallback* built-ins of the analysis.
 *
 * Built-ins normally resolve from the Kotlin stdlib among the analyzed module's dependencies, which ships `.kotlin_builtins` files of
 * its own. The fallback built-ins are a second source, ordered behind every real dependency, so they answer only what the stdlib did
 * not: a module with no stdlib on its dependency path, or a declaration its stdlib variant does not publish — `kotlin-stdlib-common`,
 * for one, leaves built-in declarations out of its `.kotlin_metadata` files.
 *
 * They therefore have to be there even when the analyzed project has no stdlib at all, which is why they are read from the classpath
 * of the analysis implementation rather than from the project: resources of the Kotlin plugin in the IDE, entries of a JAR on the
 * classpath in Standalone mode. Turning such a resource into a [VirtualFile] is what a platform supplies here.
 *
 * A platform is expected to implement [BuiltinsVirtualFileProviderBaseImpl] instead of this class directly and register the result as
 * an application service.
 */
@KtPlatformInterface
abstract class BuiltinsVirtualFileProvider {
    /**
     * Returns the virtual files of all built-in declaration files.
     */
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    /**
     * Creates a search scope covering exactly the files of [getBuiltinVirtualFiles].
     *
     * The scope is not a part of any module content, as built-ins do not belong to the analyzed project.
     */
    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    @KtPlatformInterface
    companion object {
        /**
         * Returns the [BuiltinsVirtualFileProvider] registered by the current platform.
         */
        fun getInstance(): BuiltinsVirtualFileProvider = ApplicationManager.getApplication().service()
    }
}
