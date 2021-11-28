/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

abstract class PackagePartProviderFactory {
    /**
     * Create a [PackagePartProvider] for a given scope. [PackagePartProvider] is responsible for searching sub packages in a library.
     */
    abstract fun createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider
}

/**
 * Create a [PackagePartProvider] for a given scope. [PackagePartProvider] is responsible for searching sub packages in a library.
 */
internal fun Project.createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider =
    getService(PackagePartProviderFactory::class.java).createPackagePartProviderForLibrary(scope)