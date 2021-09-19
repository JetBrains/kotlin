/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Provides information about Kotlin packages in the given scope. Can be constructed via [KotlinPackageProviderFactory]
 * [doKotlinPackageExists] is called very often by a FIR compiler. And implementations should consider cache results.
 */
public abstract class KotlinPackageProvider {
    /**
     * Checks if a package with given [FqName] exists in current [GlobalSearchScope].
     * Note, that for Kotlin it is not mandatory for a package to correspond to a directory structure like in Java.
     * So, a package [FqName] is determined by Kotlin files package directive.
     */
    public abstract fun doKotlinPackageExists(packageFqName: FqName): Boolean
    public abstract fun getKotlinSubPackageFqNames(packageFqName: FqName): Set<Name>
}

public abstract class KotlinPackageProviderFactory {
    public abstract fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider
}

public fun Project.createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
    ServiceManager.getService(this, KotlinPackageProviderFactory::class.java)
        .createPackageProvider(searchScope)