/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.java

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.baseContextModuleOrSelf
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleResolverBase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.convert
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

internal class StandaloneJavaModuleResolver(
    javaModuleGraph: JavaModuleGraph,
    javaModuleFinder: CliJavaModuleFinder,
    systemModules: List<JavaModule.Explicit>,
    private val projectStructureProvider: KotlinStaticProjectStructureProvider,
    project: Project
) : JavaModuleResolverBase(javaModuleGraph, systemModules, project) {
    private val javaModulesByKaSourceModule = createJavaModulesByKaSourceModule()

    init {
        javaModulesByKaSourceModule.values.forEach(javaModuleFinder::addUserModule)
    }

    private val javaModulesBySourceFile = buildMap<VirtualFile, JavaModule.Explicit> {
        javaModulesByKaSourceModule.entries.forEach { (sourceModule = key, javaModule = value) ->
            sourceModule.psiRoots.forEach { psiRoot ->
                put(psiRoot.virtualFile, javaModule)
            }
        }
    }

    private val psiManager = PsiManager.getInstance(project)

    override fun findUserModule(file: VirtualFile): JavaModule? {
        return when (file.fileType) {
            KotlinFileType.INSTANCE, JavaFileType.INSTANCE -> javaModulesBySourceFile[file] ?: findJavaModuleByContext(file)
            else -> null
        }
    }

    private fun findJavaModuleByContext(file: VirtualFile): JavaModule.Explicit? {
        val psiFile = psiManager.findFile(file) ?: return null
        val sourceModule = projectStructureProvider.getModule(psiFile, null)
            .baseContextModuleOrSelf as? KaSourceModule ?: return null

        return javaModulesByKaSourceModule[sourceModule]
    }

    private fun createJavaModulesByKaSourceModule(): Map<KaSourceModule, JavaModule.Explicit> {
        return projectStructureProvider.allSourceFiles
            .filterIsInstance<PsiJavaFile>()
            .mapNotNull { javaFile ->
                val moduleDeclaration = javaFile.moduleDeclaration ?: return@mapNotNull null
                val sourceModule = projectStructureProvider.getModule(javaFile, null) as? KaSourceModule ?: return@mapNotNull null
                val moduleInfoFile = moduleDeclaration.containingFile.virtualFile

                sourceModule to JavaModule.Explicit(
                    createJavaModuleInfo(moduleDeclaration),
                    listOf(JavaModule.Root(moduleInfoFile.parent, isBinary = false)),
                    moduleInfoFile
                )
            }
            .toMap()
    }

    private fun createJavaModuleInfo(moduleDeclaration: PsiJavaModule): JavaModuleInfo = JavaModuleInfo(
        moduleDeclaration.name,
        moduleDeclaration.requires.mapNotNull { statement ->
            statement.moduleName?.let { moduleName ->
                JavaModuleInfo.Requires(moduleName, statement.hasModifierProperty(PsiModifier.TRANSITIVE))
            }
        },
        moduleDeclaration.exports.mapNotNull { statement ->
            statement.packageName?.let { packageName ->
                JavaModuleInfo.Exports(FqName(packageName), statement.moduleNames)
            }
        },
        moduleDeclaration.annotations.convert { annotation ->
            JavaAnnotationImpl(
                JavaElementSourceFactory.getInstance(moduleDeclaration.project).createPsiSource(annotation)
            )
        },
    )
}
