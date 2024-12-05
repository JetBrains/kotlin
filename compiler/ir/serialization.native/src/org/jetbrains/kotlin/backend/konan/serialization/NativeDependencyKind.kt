/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.konan.library.KonanLibrary

sealed class NativeDependencyKind {
    object WholeModule : NativeDependencyKind()
    class CertainFiles(val files: List<String>) : NativeDependencyKind()
}

data class NativeUnresolvedDependency(val libName: String, val kind: NativeDependencyKind) {
    companion object {
        fun wholeModule(libName: String) = NativeUnresolvedDependency(libName, NativeDependencyKind.WholeModule)
        fun certainFiles(libName: String, files: List<String>) = NativeUnresolvedDependency(libName, NativeDependencyKind.CertainFiles(files))
    }
}

data class NativeResolvedDependency(val library: KonanLibrary, val kind: NativeDependencyKind) {
    companion object {
        fun wholeModule(library: KonanLibrary) = NativeResolvedDependency(library, NativeDependencyKind.WholeModule)
        fun certainFiles(library: KonanLibrary, files: List<String>) = NativeResolvedDependency(library, NativeDependencyKind.CertainFiles(files))
    }
}

