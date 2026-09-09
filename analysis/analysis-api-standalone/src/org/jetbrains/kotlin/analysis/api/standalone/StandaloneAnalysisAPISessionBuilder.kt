/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.project.structure.builder.KaModuleContainerBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public interface StandaloneAnalysisAPISessionBuilder {
    public val application: Application
    public val project: Project

    public fun buildKtModuleProvider(init: KaModuleContainerBuilder.() -> Unit)

    public fun build(): StandaloneAnalysisAPISession

    public fun <T : Any> registerApplicationService(serviceInterface: Class<T>, serviceImplementation: T)

    public fun <T : Any> registerApplicationService(serviceImplementation: Class<T>)

    public fun <T : Any> registerProjectExtensionPoint(extensionPointName: ExtensionPointName<T>, extensionClass: Class<T>)

    public fun <T : Any> registerProjectService(serviceInterface: Class<T>, serviceImplementation: T)

    public fun <T : Any> registerProjectService(serviceImplementation: Class<T>)

    /**
     * Enables a cache cleaner for the current session. It might attempt to drop internal caches to avoid memory issues.
     *
     * The cleaner is mostly designed for long-running workloads where analyze affects so many modules that cache size blows out of proportion.
     *
     * See [KT-70489](https://youtrack.jetbrains.com/issue/KT-70489) for more details.
     */
    public fun enableCacheCleaner()
}


@OptIn(ExperimentalContracts::class)
public inline fun buildStandaloneAnalysisAPISession(
    projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    unitTestMode: Boolean = false,
    init: StandaloneAnalysisAPISessionBuilder.() -> Unit,
): StandaloneAnalysisAPISession {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KaStandaloneInternalsProvider.instance.getStandaloneSessionBuilder(
        projectDisposable,
        unitTestMode,
    ).apply(init).build()
}

/**
 * Disposes global resources which would persist after unloading Analysis API and IJ platform classes.
 *
 * **Important:** Once this function has been called, Analysis API *and* IntelliJ platform classes should not be used anymore. The classes
 * should either be unloaded or the whole program should be shut down.
 *
 * You don't need to use this endpoint right before your program shuts down. The purpose of this function is rather to dispose global
 * resources which would persist after unloading Analysis API and IJ platform classes. For example, an IJ platform class may be registered
 * with a JDK class. If Analysis API & IJ platform classes are unloaded, this global registration may keep alive the old class loader.
 *
 * Note: Everything in Standalone is experimental, but this endpoint is likely to change in the *near future*. Please consult with the
 * Analysis API team if you want to use this. (We'll want to know about your use case.)
 */
@Suppress("UnstableApiUsage")
@KaExperimentalApi
public fun disposeGlobalStandaloneApplicationServices() {
    AppExecutorUtil.shutdownApplicationScheduledExecutorService()
}
