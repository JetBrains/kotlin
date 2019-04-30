/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.tests

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

object KonanTestLocator : SMTestLocator {
  override fun getLocation(protocol: String, path: String, project: Project, scope: GlobalSearchScope): List<Location<KtElement>> {
    return when (protocol) {
      "ktest:suite" -> locateSuite(path, project, scope)
      "ktest:test" -> locateTest(path, project, scope)
      else -> emptyList()
    }
  }
}

private fun locateSuite(fqName: String, project: Project, scope: GlobalSearchScope): List<PsiLocation<KtElement>> {
  return findTestSuites(fqName, project, scope).map { PsiLocation(project, it as KtElement) }
}

private fun locateTest(fqName: String, project: Project, scope: GlobalSearchScope): List<Location<KtElement>> {
  val (suiteFqName, methodName) = fqName.splitLastDot()
  return findTestSuites(suiteFqName, project, scope)
    .flatMap { it.declarations.filter { it.name == methodName } }
    .map { test: KtElement -> PsiLocation(project, test) }
}

private fun findTestSuites(fqName: String, project: Project, scope: GlobalSearchScope): Collection<KtDeclarationContainer> {
  return findTestClasses(fqName, project, scope) + findTestFiles(fqName, project, scope)
}

private fun findTestClasses(fqName: String, project: Project, scope: GlobalSearchScope): Collection<KtClassOrObject> {
  return KotlinFullClassNameIndex.getInstance().get(fqName, project, scope)
}

private fun findTestFiles(fqName: String, project: Project, scope: GlobalSearchScope): Collection<KtFile> {
  if (!fqName.endsWith("Kt")) return emptyList()
  val (packageName, fileShortName) = fqName.splitLastDot()
  return PackageIndexUtil.findFilesWithExactPackage(FqName(packageName), scope, project)
    .filter { PackagePartClassUtils.getFilePartShortName(it.name) == fileShortName }
}

private fun String.splitLastDot() = substringBeforeLast('.') to substringAfterLast('.')