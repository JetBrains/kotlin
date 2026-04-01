/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.mock

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.SystemIndependent
import org.picocontainer.PicoContainer
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

@Suppress("UnstableApiUsage")
@OptIn(DelicateCoroutinesApi::class)
open class MockProject @ApiStatus.Internal constructor(parent: PicoContainer?, parentDisposable: Disposable) :
    MockComponentManager(parent, parentDisposable), Project, ComponentManagerEx {
    val stubVersion = "251.patched"

    private val myCoroutineScope: CoroutineScope = run {
        require(Dispatchers.Unconfined[Job] == null) {
            "Context must not specify a Job: ${Dispatchers.Unconfined}"
        }

        ChildScope(GlobalScope.coroutineContext + Dispatchers.Unconfined + CoroutineName("MockProject: $this"))
    }

    @Suppress("INVISIBLE_REFERENCE")
    private class ChildScope(ctx: CoroutineContext) : kotlinx.coroutines.JobImpl(ctx[Job]), CoroutineScope {

        override fun childCancelled(cause: Throwable): Boolean = false

        override val coroutineContext: CoroutineContext = ctx + this

        override fun toString(): String {
            val coroutineName = coroutineContext[CoroutineName]?.name
            return (if (coroutineName != null) "\"$coroutineName\":" else "") + "supervisor:" + super.toString()
        }
    }

    override fun dispose() {
        myCoroutineScope.cancel(CancellationException())

        super.dispose()
    }

    override fun getDisposed(): Condition<*> {
        return Condition { _: Any? -> isDisposed }
    }

    override fun isOpen(): Boolean = false

    override fun isInitialized(): Boolean = true

    override fun getCoroutineScope(): CoroutineScope = myCoroutineScope

    override fun getActualComponentManager(): ComponentManager = this

    override fun getProjectFile(): VirtualFile? = null

    override fun getName(): String = ""

    @NonNls
    override fun getLocationHash(): String = "mock"

    override fun getProjectFilePath(): @SystemIndependent String? = null

    override fun getWorkspaceFile(): VirtualFile? = null

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getBaseDir(): VirtualFile? = null

    @Suppress("Unused")
    fun setBaseDir(baseDir: VirtualFile?) {
    }

    override fun getBasePath(): @SystemIndependent String? = null

    override fun save() {}
}
