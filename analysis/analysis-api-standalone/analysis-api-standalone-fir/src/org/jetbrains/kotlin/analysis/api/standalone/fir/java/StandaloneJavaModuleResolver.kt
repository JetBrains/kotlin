/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.platform.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.baseContextModuleOrSelf
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.modules.AbstractJavaModuleResolver
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.convert
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

internal class StandaloneJavaModuleResolver(
    javaModuleGraph: JavaModuleGraph,
    javaModuleFinder: CliJavaModuleFinder,
    private val projectStructureProvider: KotlinStaticProjectStructureProvider,
    project: Project,
    fileFinderFactory: CliVirtualFileFinderFactory
) : AbstractJavaModuleResolver(javaModuleGraph, javaModuleFinder.systemModules.toList()) {
    private val javaModulesByKaSourceModule by lazy {
        createJavaModulesByKaSourceModule().also { modules ->
            modules.values.forEach { module ->
                check(javaModuleFinder.findModule(module.name) == null) {
                    "Java module '${module.name}' is already registered; " +
                            "Standalone cannot resolve multiple Java modules with the same name"
                }
                javaModuleFinder.addUserModule(module)
            }
        }
    }

    private val javaModuleByPhysicalSourceFile = NullableCaffeineCache<VirtualFile, JavaModule.Explicit> { it }

    private val psiManager = PsiManager.getInstance(project)
    private val sourceFileFinder = fileFinderFactory.create(AllJavaSourcesInProjectScope(project))
    private val classpathFinder = fileFinderFactory.create(JvmClasspath.ProjectLibraries())

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val virtualFile = sourceFileFinder.findSourceOrBinaryVirtualFile(classId)
            ?: classpathFinder.findSourceOrBinaryVirtualFile(classId)
            ?: return null
        return (findJavaModule(virtualFile) as? JavaModule.Explicit)?.moduleInfo?.annotations
    }

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
        val referencedModule = findJavaModule(referencedFile)
        if (referencedModule == null && !isSystemModuleFile(referencedFile) && !isOwnedBySourceModule(referencedFile)) {
            // TODO(KT-91827): Java modules are resolved only for source modules, so preserve Standalone's fail-open behavior
            // otherwise.
            return null
        }

        return checkAccessibility(fileFromOurModule?.let(::findJavaModule), referencedModule, referencedPackage)
    }

    private fun isOwnedBySourceModule(file: VirtualFile): Boolean =
        psiManager.findFile(file)?.let(::findKaSourceModule) != null

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
                val file = moduleDeclaration.containingFile
                val sourceModule = findKaSourceModule(file) ?: continue
                if (sourceModule in this) continue
                val moduleInfoFile = file.virtualFile

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

    private fun PsiFileSystemItem.findJavaModuleDeclaration(): PsiJavaModule? =
        (this as? PsiJavaFile)?.takeIf { name == PsiJavaModule.MODULE_INFO_FILE }?.moduleDeclaration

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
