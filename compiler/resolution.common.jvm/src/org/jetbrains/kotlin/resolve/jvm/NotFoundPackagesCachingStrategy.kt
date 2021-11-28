/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

interface NotFoundPackagesCachingStrategy {

    fun chooseStrategy(isLibrarySearchScope: Boolean, qualifiedName: String): CacheType

    enum class CacheType {
        LIB_SCOPE, SCOPE, NO_CACHING
    }

    object Default : NotFoundPackagesCachingStrategy {
        override fun chooseStrategy(isLibrarySearchScope: Boolean, qualifiedName: String): CacheType {
            // qualifiedName could be like a proper package name, e.g `org.jetbrains.kotlin`
            // but it could be as well part of typed text like `fooba`
            //
            // all those temporary names and those don't even look like a package name should be stored in a short term cache
            // while names those are potentially proper package name could be stored for a long time
            // (till PROJECT_ROOTS or specific VFS changes)
            val packageLikeQName = qualifiedName.indexOf('.') > 0

            return if (isLibrarySearchScope && packageLikeQName) CacheType.LIB_SCOPE
            else CacheType.SCOPE
        }
    }
}
