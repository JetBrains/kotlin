/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageExistenceChecker
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinEmptyPackageExistenceChecker
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageExistenceChecker
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageExistenceCheckerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageExistenceCheckerMerger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class KotlinStandalonePackageExistenceChecker(
    project: Project,
    scope: GlobalSearchScope,
    files: Collection<KtFile>,
    val modules: List<KaModule>,
) : KotlinPackageExistenceChecker {
    // TODO
    private val provider = KotlinStandalonePackageProvider(project, scope, files)

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return provider.doesKotlinOnlyPackageExist(packageFqName)
    }
}

class KotlinStandalonePackageExistenceCheckerFactory(
    private val project: Project,
    private val files: Collection<KtFile>,
) : KotlinPackageExistenceCheckerFactory {
    override fun createPackageExistenceChecker(modules: List<KaModule>): KotlinPackageExistenceChecker {
        if (modules.isEmpty()) return KotlinEmptyPackageExistenceChecker
        return KotlinStandalonePackageExistenceChecker(project, GlobalSearchScope.union(modules.map { it.contentScope }), files, modules)
    }
}

class KotlinStandalonePackageExistenceCheckerMerger(private val project: Project) : KotlinPackageExistenceCheckerMerger {
    override fun merge(providers: List<KotlinPackageExistenceChecker>): KotlinPackageExistenceChecker =
        providers.mergeSpecificProviders<_, KotlinStandalonePackageExistenceChecker>(KotlinCompositePackageExistenceChecker.factory) { targetProviders ->
            KotlinPackageExistenceCheckerFactory.getInstance(project).createPackageExistenceChecker(
                providers.flatMap { (it as KotlinStandalonePackageExistenceChecker).modules } + targetProviders.flatMap { it.modules }
            )
        }
}