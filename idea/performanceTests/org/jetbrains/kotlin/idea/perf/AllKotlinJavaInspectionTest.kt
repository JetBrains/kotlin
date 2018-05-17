/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.codeInspection.ex.Tools
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import kotlin.system.measureNanoTime

class AllKotlinJavaInspectionTest : AllKotlinTest() {
    override fun provideFiles(): Collection<VirtualFile> {
        val scope = object : DelegatingGlobalSearchScope(ProjectScope.getContentScope(project)) {
            val index = ProjectFileIndex.getInstance(myProject)
            override fun contains(file: VirtualFile): Boolean {
                return super.contains(file) &&
                        ProjectRootsUtil.isInContent(myProject, file, true, false, false, false, index)
            }
        }

        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
    }

    private lateinit var profileTools: List<Tools>

    override fun setUp() {

        super.setUp()

        val wrappers = InspectionToolRegistrar.getInstance().createTools().map { it.tool }

        enableInspectionTools(*wrappers.toTypedArray())

        profileTools = ProjectInspectionProfileManager.getInstance(project).currentProfile.getAllEnabledInspectionTools(project)
    }

    class ExceptionWhileInspection(inspectionId: String, cause: Throwable) : RuntimeException(inspectionId, cause)

    override fun doTest(file: VirtualFile): PerFileTestResult {

        val results = mutableMapOf<String, Long>()
        var totalNs = 0L

        val psiFile = file.toPsiFile(project) ?: run {
            return PerFileTestResult(results, totalNs, listOf(AssertionError("PsiFile not found for $file")))
        }

        val errors = mutableListOf<Throwable>()

        for (profileTool in profileTools) {
            val localInspectionTool = profileTool.tool.tool as? LocalInspectionTool ?: continue
            if (profileTool.tool.language !in setOf(null, "java", "UAST", JavaLanguage.INSTANCE.id)) continue
            val result = measureNanoTime {
                try {
                    localInspectionTool.analyze(psiFile)
                } catch (t: Throwable) {
                    val myEx = ExceptionWhileInspection(profileTool.tool.id, t)
                    myEx.printStackTrace()
                    errors += myEx
                }
            }
            results[profileTool.tool.id] = result
            totalNs += result
            if (errors.isNotEmpty()) break
        }

        return PerFileTestResult(results, totalNs, errors)
    }

    private fun LocalInspectionTool.analyze(file: PsiFile) {
        if (file.textRange == null) return

        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
    }

}