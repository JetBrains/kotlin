/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.execution.configurations.RunConfigurationBase
import org.jetbrains.kotlin.idea.run.JetRunConfiguration
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.coverage.CoverageSuitesBundle
import java.io.File
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiNamedElement
import com.intellij.coverage.PackageAnnotator
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.coverage.JavaCoverageAnnotator
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.idea.util.application.runReadAction

public class KotlinCoverageExtension(): JavaCoverageEngineExtension() {
    private val LOG = Logger.getInstance(javaClass<KotlinCoverageExtension>())

    override fun isApplicableTo(conf: RunConfigurationBase?): Boolean = conf is JetRunConfiguration

    override fun suggestQualifiedName(sourceFile: PsiFile, classes: Array<out PsiClass>, names: MutableSet<String>): Boolean {
        if (sourceFile is JetFile) {
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
        if (element !is JetFile) {
            return null
        }
        LOG.info("Retrieving coverage for " + element.getName())

        val qualifiedNames = collectGeneratedClassQualifiedNames(findOutputRoot(element), element)
        return if (qualifiedNames == null) null else totalCoverageForQualifiedNames(coverageAnnotator, qualifiedNames)
    }

    // Implements API added in IDEA 14.1
    override fun keepCoverageInfoForClassWithoutSource(bundle: CoverageSuitesBundle, classFile: File): Boolean {
        // TODO check scope and source roots
        return true  // keep everything, sort it out later
    }

    // Implements API added in IDEA 14.1
    override fun ignoreCoverageForClass(bundle: CoverageSuitesBundle, classFile: File): Boolean {
        // Ignore classes that only contain bridge methods delegating to package parts.
        if (looksLikePackageFacade(classFile)) {
            val classVFile = LocalFileSystem.getInstance().findFileByIoFile(classFile)
            if (classVFile == null) return false
            val header = KotlinBinaryClassCache.getKotlinBinaryClass(classVFile)?.getClassHeader()
            return header != null && header.kind == KotlinClassHeader.Kind.PACKAGE_FACADE;
        }
        return false;
    }

    fun looksLikePackageFacade(classFile: File): Boolean {
        val packageName = classFile.getParentFile().getName()
        return classFile.getName() == StringUtil.capitalize(packageName) + PackageClassUtils.PACKAGE_CLASS_NAME_SUFFIX + ".class"
    }

    override fun collectOutputFiles(srcFile: PsiFile,
                                    output: VirtualFile?,
                                    testoutput: VirtualFile?,
                                    suite: CoverageSuitesBundle,
                                    classFiles: MutableSet<File>): Boolean {
        if (srcFile is JetFile) {
            val fileIndex = ProjectRootManager.getInstance(srcFile.getProject()).getFileIndex()
            if (fileIndex.isInLibraryClasses(srcFile.getVirtualFile()) ||
                fileIndex.isInLibrarySource(srcFile.getVirtualFile())) {
                return false
            }

            runReadAction {
                val outputRoot = findOutputRoot(srcFile)
                val existingClassFiles = getClassesGeneratedFromFile(outputRoot, srcFile)
                existingClassFiles.mapTo(classFiles) { File(it.getPath()) }
            }
            return true
        }
        return false
    }

    default object {
        private val LOG = Logger.getInstance(javaClass<KotlinCoverageExtension>())

        fun collectGeneratedClassQualifiedNames(outputRoot: VirtualFile?, file: JetFile): List<String>? {
            val existingClassFiles = getClassesGeneratedFromFile(outputRoot, file)
            if (existingClassFiles.isEmpty()) {
                return null
            }
            LOG.debug("Classfiles: [${existingClassFiles.map { it.getName() }.join()}]")
            return existingClassFiles.map {
                val relativePath = VfsUtilCore.getRelativePath(it, outputRoot)
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
                    LOG.debug("Found no coverage for ${it}")
                }
            }
            return result
        }

        private fun getClassesGeneratedFromFile(outputRoot: VirtualFile?, file: JetFile): List<VirtualFile> {
            val relativePath = file.getPackageFqName().asString().replace('.', '/')
            val packageOutputDir = outputRoot?.findFileByRelativePath(relativePath)
            if (packageOutputDir == null) return listOf()

            val prefixes = collectClassFilePrefixes(file)
            LOG.debug("Classfile prefixes: [${prefixes.join(", ")}]")
            return packageOutputDir.getChildren().filter {
                file -> prefixes.any {
                (file.getName().startsWith(it + "$") && FileUtilRt.getExtension(file.getName()) == "class") ||
                file.getName() == it + ".class"
            }
            }
        }

        private fun findOutputRoot(file: JetFile): VirtualFile? {
            val module = ModuleUtilCore.findModuleForPsiElement(file)
            if (module == null) return null
            val fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex()
            val inTests = fileIndex.isInTestSourceContent(file.getVirtualFile())
            val compilerOutputExtension = CompilerModuleExtension.getInstance(module)
            return if (inTests)
                compilerOutputExtension.getCompilerOutputPathForTests()
            else
                compilerOutputExtension.getCompilerOutputPath()
        }

        private fun collectClassFilePrefixes(file: JetFile): Collection<String> {
            val result = file.getChildren().filter { it is JetClassOrObject }.map { (it as JetClassOrObject).getName() }
            val packagePartFqName = PackagePartClassUtils.getPackagePartFqName(file)
            return result.union(arrayListOf(packagePartFqName.shortName().asString()))
        }
    }
}
