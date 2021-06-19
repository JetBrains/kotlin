/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

abstract class KtPackageProvider {
    abstract fun isPackageExists(packageFqName: FqName): Boolean
    abstract fun getJavaAndKotlinSubPackageFqNames(packageFqName: FqName, targetPlatform: TargetPlatform): Set<Name>
}

abstract class KtPackageProviderFactory {
    abstract fun createPackageProvider(searchScope: GlobalSearchScope): KtPackageProvider
}

fun Project.createPackageProvider(searchScope: GlobalSearchScope): KtPackageProvider =
    ServiceManager.getService(this, KtPackageProviderFactory::class.java)
        .createPackageProvider(searchScope)