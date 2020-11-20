/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PackageOracleFactory
import org.jetbrains.kotlin.idea.caches.resolve.IdePackageOracleFactory
import org.jetbrains.kotlin.name.FqName

internal sealed class PackageExistenceChecker {
    abstract fun isPackageExists(packageFqName: FqName): Boolean
}

internal class PackageExistenceCheckerForSingleModule(
    project: Project,
    module: ModuleInfo
) : PackageExistenceChecker() {
    private val oracle =
        project.service<IdePackageOracleFactory>().createOracle(module)
    override fun isPackageExists(packageFqName: FqName): Boolean = oracle.packageExists(packageFqName)
}

internal class PackageExistenceCheckerForMultipleModules(
    project: Project,
    modules: List<ModuleInfo>
) : PackageExistenceChecker() {
    private val oracles = run {
        val factory = project.service<IdePackageOracleFactory>()
        modules.map { factory.createOracle(it) }
    }

    override fun isPackageExists(packageFqName: FqName): Boolean =
        oracles.any { oracle -> oracle.packageExists(packageFqName) }
}