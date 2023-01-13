/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.library.resolver

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.library.resolver to package org.jetbrains.kotlin.library.metadata.resolver",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver")
)
typealias KotlinLibraryResolver<L> = org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver<L>

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.library.resolver to package org.jetbrains.kotlin.library.metadata.resolver",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult")
)
typealias KotlinLibraryResolveResult = org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult

@Deprecated(
    "This type alias has been moved from package org.jetbrains.kotlin.library.resolver to package org.jetbrains.kotlin.library.metadata.resolver",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.LibraryOrder")
)
typealias LibraryOrder = org.jetbrains.kotlin.library.metadata.resolver.LibraryOrder

@Deprecated(
    "This property has been moved from package org.jetbrains.kotlin.library.resolver to package org.jetbrains.kotlin.library.metadata.resolver",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder")
)
inline val TopologicalLibraryOrder: org.jetbrains.kotlin.library.metadata.resolver.LibraryOrder
    get() = org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.library.resolver to package org.jetbrains.kotlin.library.metadata.resolver",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary")
)
typealias KotlinResolvedLibrary = org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
