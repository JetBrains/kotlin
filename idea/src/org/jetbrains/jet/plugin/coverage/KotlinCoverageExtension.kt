/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.coverage

import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.execution.configurations.RunConfigurationBase
import org.jetbrains.jet.plugin.run.JetRunConfiguration
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.coverage.CoverageSuitesBundle
import java.io.File
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.debugger.JetPositionManager
import org.jetbrains.jet.plugin.caches.resolve.getModuleInfo
import java.util.HashSet
import org.jetbrains.jet.lang.psi.JetTreeVisitor
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.annotations.TestOnly
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import com.intellij.psi.PsiElement
import java.util.LinkedHashSet

public class KotlinCoverageExtension(): JavaCoverageEngineExtension() {
    override fun isApplicableTo(conf: RunConfigurationBase?): Boolean = conf is JetRunConfiguration

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

            val classNames = ApplicationManager.getApplication().runReadAction<Set<String>> { collectOutputClassNames(srcFile) }

            fun findUnder(name: String, root: VirtualFile?): File? {
                if (root != null) {
                    val outputFile = File(root.getPath(), name + ".class")
                    if (outputFile.exists()) {
                        return outputFile
                    }
                }
                return null
            }

            classNames.map { findUnder(it, output) ?: findUnder(it, testoutput) }.filterNotNullTo(classFiles)
            return true
        }
        return false
    }

    class object {
        public fun collectOutputClassNames(srcFile: JetFile): Set<String> {
            val typeMapper = JetPositionManager.createTypeMapper(srcFile, srcFile.getModuleInfo())
            val classNames = LinkedHashSet<String>()
            srcFile.acceptChildren(object: JetTreeVisitorVoid() {
                override fun visitDeclaration(dcl: JetDeclaration) {
                    super.visitDeclaration(dcl)
                    collectClassName(dcl.getFirstChild())
                }

                override fun visitFunctionLiteralExpression(expression: JetFunctionLiteralExpression) {
                    super.visitFunctionLiteralExpression(expression)
                    collectClassName(expression.getFunctionLiteral().getFirstChild())
                }

                private fun collectClassName(element: PsiElement) {
                    val className = JetPositionManager.getClassNameForElement(element, typeMapper, srcFile, false)
                    if (className != null) {
                        classNames.add(className)
                    }
                }
            })
            return classNames
        }
    }
}