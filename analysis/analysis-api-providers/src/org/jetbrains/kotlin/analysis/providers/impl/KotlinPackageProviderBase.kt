/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm


public abstract class KotlinPackageProviderBase(
    protected val
    project: Project,
    protected val searchScope: GlobalSearchScope
) : KotlinPackageProvider() {

    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return doesPlatformSpecificPackageExist(packageFqName, platform) || doesKotlinOnlyPackageExist(packageFqName)
    }

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        when {
            platform.isJvm() -> {
                val fqNameString = packageFqName.asString()
                forEachNonKotlinPsiElementFinder(project) { finder ->
                    val psiPackage = finder.findPackage(fqNameString)
                    if (psiPackage != null) {
                        // we cannot easily check if some PsiPackage is in GlobalSearchScope or not
                        return true
                    }
                }
                return false
            }
            else -> {
                // non-JVM platforms are not supported yet
                return false
            }
        }
    }

    override fun getSubPackageFqNames(packageFqName: FqName, platform: TargetPlatform, nameFilter: (Name) -> Boolean): Set<Name> =
        buildSet {
            addAll(getKotlinOnlySubPackagesFqNames(packageFqName, nameFilter))
            addAll(getPlatformSpecificSubPackagesFqNames(packageFqName, platform, nameFilter))
        }


    override fun getPlatformSpecificSubPackagesFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name> = when {
        platform.isJvm() -> {
            val fqNameString = packageFqName.asString()
            buildSet {
                forEachNonKotlinPsiElementFinder(project) { finder ->
                    val psiPackage = finder.findPackage(fqNameString) ?: return@forEachNonKotlinPsiElementFinder
                    for (subPackage in finder.getSubPackages(psiPackage, searchScope)) {
                        val name = subPackage.name?.let(Name::identifierIfValid) ?: continue
                        if (!nameFilter(name)) continue
                        add(name)
                    }
                }
            }
        }
        else -> {
            // non-JVM platforms are not supported yet
            emptySet()
        }
    }
}

