/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.container.topologicalSort
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon

/**
 * [FirModuleData] is an abstraction over modules module inside FIR compiler.
 *
 * Each FIR declaration holds module data in [FirDeclaration.moduleData], and this module data represents
 *   module which this declaration belongs to
 *
 * Module data contains minimal information about module ([name] and [platform]) and
 *   number of collections with different kinds of dependencies of current module.
 * There are three different kinds of dependencies:
 *   - [dependencies] are just regular dependencies. Current module can reference public declarations from them
 *   - [friendDependencies] are dependencies with friend relation. Current module can reference public and internal
 *       declarations from them
 *   - [dependsOnDependencies] are special kind of dependencies which came from MPP project model. Current module can reference
 *       public and internal declarations from them (like from friends) and also can actualize `common` declarations from them
 *       with corresponding `actual` declarations
 *
 * Each module data should belong to some [FirSession], but some FirSessions can have multiple module data. Basically, there are
 *   two main kinds of FirSession: session for module with sources and session for binary dependencies. Each source session must
 *   have exactly one module data which represents corresponding module (and it is accessible via [FirSession.moduleData] extension).
 *   Libraries sessions may have multiple module data for different kinds of dependencies (one for regular, friend and depends dependencies
 *   set), and it's impossible to extract any module data from such session.
 *
 * Mental model for libraries sessions and module data:
 *   Each klib/jar/smth else in binary dependency may be added to project with one of three kinds which are declared above, so we need
 *   different module data for them. But for deserializing declarations from dependencies we should use single provider to reduce number
 *   of IO operations and false matches. To solve this problem we use single session for all dependencies with single deserialized
 *   symbol provider. And during creation FIR for some declaration symbol provider chose which module this declaration will belong to
 *   basing on path of this declaration and passed compiler arguments
 *
 * With MPP mode, all modules have the same platform, but some checkers need info about whether the current module is common or not.
 *   For this purpose the flag [isCommon] is used
 */
abstract class FirModuleData : FirSessionComponent {
    abstract val name: Name
    abstract val dependencies: List<FirModuleData>
    abstract val dependsOnDependencies: List<FirModuleData>

    /** Transitive closure over [dependsOnDependencies] */
    abstract val allDependsOnDependencies: List<FirModuleData>

    abstract val friendDependencies: List<FirModuleData>
    abstract val platform: TargetPlatform
    abstract val isCommon: Boolean

    open val capabilities: FirModuleCapabilities
        get() = FirModuleCapabilities.Empty

    protected var boundSession: FirSession? = null
        private set

    abstract val session: FirSession

    fun bindSession(session: FirSession) {
        if (boundSession != null) {
            error("module data already bound to $this")
        }
        boundSession = session
    }

    override fun toString(): String {
        return "Module $name"
    }
}

class FirModuleDataImpl(
    override val name: Name,
    override val dependencies: List<FirModuleData>,
    override val dependsOnDependencies: List<FirModuleData>,
    override val friendDependencies: List<FirModuleData>,
    override val platform: TargetPlatform,
    override val capabilities: FirModuleCapabilities = FirModuleCapabilities.Empty,
    override val isCommon: Boolean = platform.isCommon(),
) : FirModuleData() {
    override val session: FirSession
        get() = boundSession
            ?: error("module data ${this::class.simpleName}:${name} not bound to session")

    override val allDependsOnDependencies: List<FirModuleData> = topologicalSort(dependsOnDependencies) { it.dependsOnDependencies }
}

val FirSession.nullableModuleData: FirModuleData? by FirSession.nullableSessionComponentAccessor()
val FirSession.moduleData: FirModuleData
    get() = nullableModuleData ?: error("Module data is not registered in $this")
