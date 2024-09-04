/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.KotlinCompositeProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinCompositeProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform


public interface KotlinPackageExistenceChecker : KotlinComposableProvider {
    public fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean
}

public class KotlinCompositePackageExistenceChecker private constructor(
    override val providers: List<KotlinPackageExistenceChecker>,
) : KotlinPackageExistenceChecker, KotlinCompositeProvider<KotlinPackageExistenceChecker> {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return providers.any { it.doesKotlinOnlyPackageExist(packageFqName) }
    }

    public companion object {
        public val factory: KotlinCompositeProviderFactory<KotlinPackageExistenceChecker> = KotlinCompositeProviderFactory(
            KotlinEmptyPackageExistenceChecker,
            ::KotlinCompositePackageExistenceChecker,
        )

        public fun create(providers: List<KotlinPackageExistenceChecker>): KotlinPackageExistenceChecker = factory.create(providers)
    }
}

public object KotlinEmptyPackageExistenceChecker : KotlinPackageExistenceChecker {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return false
    }
}

public interface KotlinPackageExistenceCheckerFactory {
    public fun createPackageExistenceChecker(modules: List<KaModule>): KotlinPackageExistenceChecker
    public fun createPackageExistenceChecker(module: KaModule): KotlinPackageExistenceChecker =
        createPackageExistenceChecker(listOf(module))

    public companion object {
        public fun getInstance(project: Project): KotlinPackageExistenceCheckerFactory = project.service()
    }
}

public interface KotlinPackageExistenceCheckerMerger : KotlinComposableProviderMerger<KotlinPackageExistenceChecker>,
    KotlinPlatformComponent {
    public companion object {
        public fun getInstance(project: Project): KotlinPackageExistenceCheckerMerger = project.service()
    }
}


public fun Project.mergePackageExistenceChecker(checkers: List<KotlinPackageExistenceChecker>): KotlinPackageExistenceChecker =
    KotlinPackageExistenceCheckerMerger.getInstance(this).merge(checkers)
