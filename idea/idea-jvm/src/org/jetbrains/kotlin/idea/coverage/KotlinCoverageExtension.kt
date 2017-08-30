/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.coverage

import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageAnnotator
import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.coverage.PackageAnnotator
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.run.JetRunConfiguration
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class KotlinCoverageExtension : JavaCoverageEngineExtension() {
    private val LOG = Logger.getInstance(KotlinCoverageExtension::class.java)

    override fun isApplicableTo(conf: RunConfigurationBase?): Boolean = conf is JetRunConfiguration

    override fun suggestQualifiedName(sourceFile: PsiFile, classes: Array<out PsiClass>, names: MutableSet<String>): Boolean {
        if (sourceFile is KtFile) {
            val qNames = collectGeneratedClassQualifiedNames(findOutputRoot(sourceFile), sourceFile)
            if (qNames != null) {
                names.addAll(qNames)
                return true
            }
        }
        return false
    }

    // Implements API added in IDEA 14.1
    override fun getSummaryCoverageInfo(coverageAnnotator: JavaCoverageAnnotator,
                               element: PsiNamedElement): PackageAnnotator.ClassCoverageInfo? {
        if (element !is KtFile) {
            return null
        }
        LOG.info("Retrieving coverage for " + element.name)

        val qualifiedNames = collectGeneratedClassQualifiedNames(findOutputRoot(element), element)
        return if (qualifiedNames == null) null else totalCoverageForQualifiedNames(coverageAnnotator, qualifiedNames)
    }

    // Implements API added in IDEA 14.1
    override fun keepCoverageInfoForClassWithoutSource(bundle: CoverageSuitesBundle, classFile: File): Boolean {
        // TODO check scope and source roots
        return true  // keep everything, sort it out later
    }

    override fun collectOutputFiles(srcFile: PsiFile,
                                    output: VirtualFile?,
                                    testoutput: VirtualFile?,
                                    suite: CoverageSuitesBundle,
                                    classFiles: MutableSet<File>): Boolean {
        if (srcFile is KtFile) {
            val fileIndex = ProjectRootManager.getInstance(srcFile.getProject()).fileIndex
            if (fileIndex.isInLibraryClasses(srcFile.getVirtualFile()) ||
                fileIndex.isInLibrarySource(srcFile.getVirtualFile())) {
                return false
            }

            runReadAction {
                val outputRoot = findOutputRoot(srcFile)
                val existingClassFiles = getClassesGeneratedFromFile(outputRoot, srcFile)
                existingClassFiles.mapTo(classFiles) { File(it.path) }
            }
            return true
        }
        return false
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinCoverageExtension::class.java)

        fun collectGeneratedClassQualifiedNames(outputRoot: VirtualFile?, file: KtFile): List<String>? {
            val existingClassFiles = getClassesGeneratedFromFile(outputRoot, file)
            if (existingClassFiles.isEmpty()) {
                return null
            }
            LOG.debug("Classfiles: [${existingClassFiles.joinToString { it.name }}]")
            return existingClassFiles.map {
                val relativePath = VfsUtilCore.getRelativePath(it, outputRoot!!)!!
                StringUtil.trimEnd(relativePath, ".class").replace("/", ".")
            }
        }

        private fun totalCoverageForQualifiedNames(coverageAnnotator: JavaCoverageAnnotator,
                                                   qualifiedNames: List<String>): PackageAnnotator.ClassCoverageInfo {
            val result = PackageAnnotator.ClassCoverageInfo()
            result.totalClassCount = 0
            qualifiedNames.forEach {
                val classInfo = coverageAnnotator.getClassCoverageInfo(it)
                if (classInfo != null) {
                    result.totalClassCount += classInfo.totalClassCount
                    result.coveredClassCount += classInfo.coveredClassCount
                    result.totalMethodCount += classInfo.totalMethodCount
                    result.coveredMethodCount += classInfo.coveredMethodCount
                    result.totalLineCount += classInfo.totalLineCount
                    result.fullyCoveredLineCount += classInfo.fullyCoveredLineCount
                    result.partiallyCoveredLineCount += classInfo.partiallyCoveredLineCount
                }
                else {
                    LOG.debug("Found no coverage for $it")
                }
            }
            return result
        }

        private fun getClassesGeneratedFromFile(outputRoot: VirtualFile?, file: KtFile): List<VirtualFile> {
            val relativePath = file.packageFqName.asString().replace('.', '/')
            val packageOutputDir = outputRoot?.findFileByRelativePath(relativePath)
            if (packageOutputDir == null) return listOf()

            val prefixes = collectClassFilePrefixes(file)
            LOG.debug("Classfile prefixes: [${prefixes.joinToString(", ")}]")
            return packageOutputDir.children.filter {
                file -> prefixes.any {
                (file.name.startsWith(it + "$") && FileUtilRt.getExtension(file.name) == "class") ||
                file.name == it + ".class"
            }
            }
        }

        private fun findOutputRoot(file: KtFile): VirtualFile? {
            val module = ModuleUtilCore.findModuleForPsiElement(file)
            if (module == null) return null
            val fileIndex = ProjectRootManager.getInstance(file.project).fileIndex
            val inTests = fileIndex.isInTestSourceContent(file.virtualFile)
            val compilerOutputExtension = CompilerModuleExtension.getInstance(module)
            return if (inTests)
                compilerOutputExtension!!.compilerOutputPathForTests
            else
                compilerOutputExtension!!.compilerOutputPath
        }

        private fun collectClassFilePrefixes(file: KtFile): Collection<String> {
            val result = file.children.filter { it is KtClassOrObject }.map { (it as KtClassOrObject).name!! }
            val packagePartFqName = JvmFileClassUtil.getFileClassInfoNoResolve(file).fileClassFqName
            return result.union(arrayListOf(packagePartFqName.shortName().asString()))
        }
    }
}
