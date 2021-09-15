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

public abstract class KotlinPackageProvider {
    public abstract fun doKotlinPackageExists(packageFqName: FqName): Boolean
    public abstract fun getKotlinSubPackageFqNames(packageFqName: FqName): Set<Name>
}

public abstract class KotlinPackageProviderFactory {
    public abstract fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider
}

public fun Project.createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
    ServiceManager.getService(this, KotlinPackageProviderFactory::class.java)
        .createPackageProvider(searchScope)