package org.jetbrains.kotlin.idea.fir.low.level.api.ide

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.IdePackageOracleFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceChecker
import org.jetbrains.kotlin.name.FqName

internal class PackageExistenceCheckerIdeImpl(
    project: Project,
    module: ModuleInfo
) : PackageExistenceChecker() {
    private val oracle = project.service<IdePackageOracleFactory>().createOracle(module)

    override fun isPackageExists(packageFqName: FqName): Boolean = oracle.packageExists(packageFqName)
}