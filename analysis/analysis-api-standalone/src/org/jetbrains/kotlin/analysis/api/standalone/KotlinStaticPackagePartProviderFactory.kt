/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

internal class KotlinStaticPackagePartProviderFactory(
    private val packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) : KotlinPackagePartProviderFactory() {
    private val cache = ContainerUtil.createConcurrentSoftMap<GlobalSearchScope, PackagePartProvider>()

    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        return cache.getOrPut(scope) {
            packagePartProvider(scope)
        }
    }
}