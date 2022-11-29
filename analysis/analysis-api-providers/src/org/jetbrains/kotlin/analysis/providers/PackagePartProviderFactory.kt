/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

public abstract class PackagePartProviderFactory {
    /**
     * Create a [PackagePartProvider] for a given scope. [PackagePartProvider] is responsible for searching sub packages in a library.
     */
    public abstract fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider
}

/**
 * Create a [PackagePartProvider] for a given scope. [PackagePartProvider] is responsible for searching sub packages in a library.
 */
public fun Project.createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider =
    getService(PackagePartProviderFactory::class.java).createPackagePartProvider(scope)