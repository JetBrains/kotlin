/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.name.FqName

public abstract class KotlinPackageProviderByKtModule {
    public abstract fun getContainedPackages(ktModule: KtModule): Collection<FqName>

    public companion object {
        public fun getInstance(project: Project): KotlinPackageProviderByKtModule =
            project.getService(KotlinPackageProviderByKtModule::class.java)
    }
}