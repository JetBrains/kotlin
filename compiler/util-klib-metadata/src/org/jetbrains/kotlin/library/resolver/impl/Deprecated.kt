/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.library.resolver.impl

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver

@Suppress("NOTHING_TO_INLINE")
@Deprecated(
    "This extension method has been moved from package org.jetbrains.kotlin.library.resolver.impl to package org.jetbrains.kotlin.library.metadata.resolver.impl",
    ReplaceWith("libraryResolver", "org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver")
)
inline fun <L : KotlinLibrary> SearchPathResolver<L>.libraryResolver(resolveManifestDependenciesLenient: Boolean = false) =
    libraryResolver(resolveManifestDependenciesLenient)

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.library.resolver.impl to package org.jetbrains.kotlin.library.metadata.resolver.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinLibraryResolverImpl")
)
typealias KotlinLibraryResolverImpl<L> = org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinLibraryResolverImpl<L>

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.library.resolver.impl to package org.jetbrains.kotlin.library.metadata.resolver.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinLibraryResolverResultImpl")
)
typealias KotlinLibraryResolverResultImpl = org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinLibraryResolverResultImpl

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.library.resolver.impl to package org.jetbrains.kotlin.library.metadata.resolver.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl")
)
typealias KotlinResolvedLibraryImpl = org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
