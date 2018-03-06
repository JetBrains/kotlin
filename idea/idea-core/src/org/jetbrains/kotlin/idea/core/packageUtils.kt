/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.idea.caches.PerModulePackageCacheService
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

fun PsiDirectory.getPackage(): PsiPackage? = JavaDirectoryService.getInstance()!!.getPackage(this)

fun PsiFile.getFqNameByDirectory(): FqName {
    val qualifiedNameByDirectory = parent?.getPackage()?.qualifiedName
    return qualifiedNameByDirectory?.let(::FqName) ?: FqName.ROOT
}

fun PsiDirectory.getFqNameWithImplicitPrefix(): FqName {
    val packageFqName = getPackage()?.qualifiedName?.let(::FqName) ?: FqName.ROOT
    sourceRoot?.let {
        val implicitPrefix = PerModulePackageCacheService.getInstance(project).getImplicitPackagePrefix(it)
        return FqName.fromSegments((implicitPrefix.pathSegments() + packageFqName.pathSegments()).map { it.asString() })
    }
    return packageFqName
}

fun KtFile.packageMatchesDirectory(): Boolean = packageFqName == getFqNameByDirectory()

fun KtFile.packageMatchesDirectoryOrImplicit() =
    packageFqName == getFqNameByDirectory() || packageFqName == parent?.getFqNameWithImplicitPrefix()
