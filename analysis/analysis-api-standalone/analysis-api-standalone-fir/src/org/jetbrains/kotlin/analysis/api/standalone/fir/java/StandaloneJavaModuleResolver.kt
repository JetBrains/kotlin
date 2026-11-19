/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.java

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.platform.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.baseContextModuleOrSelf
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.modules.AbstractJavaModuleResolver
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.convert
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

internal class StandaloneJavaModuleResolver(
    javaModuleGraph: JavaModuleGraph,
    javaModuleFinder: CliJavaModuleFinder,
    systemModules: List<JavaModule.Explicit>,
    private val projectStructureProvider: KotlinStaticProjectStructureProvider,
    project: Project,
) : AbstractJavaModuleResolver(javaModuleGraph, systemModules, project) {
    private val javaModulesByKaSourceModule by lazy {
        createJavaModulesByKaSourceModule().also { modules ->
            modules.values.forEach { module -> javaModuleFinder.addUserModule(module) }
        }
    }

    private val javaModuleByPhysicalSourceFile = NullableCaffeineCache<VirtualFile, JavaModule.Explicit> { it }

    private val psiManager = PsiManager.getInstance(project)

    override fun findSourceModule(file: VirtualFile): JavaModule? {
        if (javaModulesByKaSourceModule.isEmpty()) return null
        val psiFile = psiManager.findFile(file) ?: return null

        if (!psiFile.isPhysical || psiFile is KtFile && psiFile.isDangling) return findJavaModule(psiFile)
        return javaModuleByPhysicalSourceFile.get(file) { findJavaModule(psiFile) }
    }

    override fun findBinaryModule(file: VirtualFile): JavaModule? = null

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? {
        if (referencedFile.fileType == JavaClassFileType.INSTANCE && !isSystemModuleFile(referencedFile)) {
            // ISSUE: KT-91827 Binary Java modules are not resolved per use-site, so preserve Standalone's fail-open behavior.
            return null
        }

        return super.checkAccessibility(fileFromOurModule, referencedFile, referencedPackage)
    }

    private fun findJavaModule(file: PsiFile): JavaModule.Explicit? =
        findKaSourceModule(file)?.let(javaModulesByKaSourceModule::get)

    private fun findKaSourceModule(file: PsiFile): KaSourceModule? {
        return projectStructureProvider.getModule(file, null)
            .baseContextModuleOrSelf as? KaSourceModule
    }

    private fun createJavaModulesByKaSourceModule(): Map<KaSourceModule, JavaModule.Explicit> {
        return buildMap {
            for (sourceFile in projectStructureProvider.allSourceFiles) {
                val moduleDeclaration = sourceFile.findJavaModuleDeclaration() ?: continue
                val sourceModule = findKaSourceModule(moduleDeclaration.containingFile) ?: continue
                if (sourceModule in this) continue
                val moduleInfoFile = moduleDeclaration.containingFile.virtualFile

                put(
                    sourceModule,
                    JavaModule.Explicit(
                        createJavaModuleInfo(moduleDeclaration),
                        listOf(JavaModule.Root(moduleInfoFile.parent, isBinary = false)),
                        moduleInfoFile
                    )
                )
            }
        }
    }

    private fun PsiFileSystemItem.findJavaModuleDeclaration(): PsiJavaModule? {
        val moduleInfoFile = when (this) {
            is PsiDirectory -> findFile(PsiJavaModule.MODULE_INFO_FILE)
            is PsiJavaFile -> takeIf { name == PsiJavaModule.MODULE_INFO_FILE }
            else -> null
        } as? PsiJavaFile

        return moduleInfoFile?.moduleDeclaration
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
        }
    )
}
