/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle.Companion.INSTANCE
import org.jetbrains.kotlin.idea.formatter.ProjectCodeStyleImporter
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.NewProjectWizardModuleBuilder
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import java.nio.file.Path
import java.util.*
import com.intellij.openapi.module.Module as IdeaModule

class IdeaJpsWizardService(
    private val project: Project,
    private val modulesModel: ModifiableModuleModel,
    private val modulesBuilder: NewProjectWizardModuleBuilder,
    private val ideWizard: IdeWizard
) : ProjectImportingWizardService, IdeaWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean =
        buildSystemType == BuildSystemType.Jps

    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>,
        buildSystem: BuildSystemType
    ): TaskResult<Unit> = runWriteAction {
        ideWizard.jpsData.jdk?.let { jdk -> NewProjectUtil.applyJdkToProject(project, jdk) }
        KotlinSdkType.setUpIfNeeded()
        modulesBuilder.addModuleConfigurationUpdater(JpsModuleConfigurationUpdater(ideWizard.jpsData))

        ProjectImporter(project, modulesModel, path, modulesIrs).import()
    }
}

private class JpsModuleConfigurationUpdater(private val jpsData: IdeWizard.JpsData) : ModuleBuilder.ModuleConfigurationUpdater() {
    override fun update(module: IdeaModule, rootModel: ModifiableRootModel) = with(jpsData) {
        libraryOptionsPanel.apply()?.addLibraries(
            rootModel,
            ArrayList(),
            librariesContainer
        )
        libraryDescription.finishLibConfiguration(module, rootModel, true)
        ProjectCodeStyleImporter.apply(module.project, INSTANCE)
    }
}

private class ProjectImporter(
    private val project: Project,
    private val modulesModel: ModifiableModuleModel,
    private val path: Path,
    private val modulesIrs: List<ModuleIR>
) {
    private val librariesPath: Path
        get() = path / "libs"

    fun import() = modulesIrs.mapSequence { moduleIR ->
        convertModule(moduleIR).map { moduleIR to it }
    }.map { irsToIdeaModule ->
        val irsToIdeaModuleMap = irsToIdeaModule.associate { (ir, module) -> ir.name to module }
        irsToIdeaModule.forEach { (moduleIr, ideaModule) ->
            addModuleDependencies(moduleIr, ideaModule, irsToIdeaModuleMap)
        }
    } andThen safe { modulesModel.commit() }

    private fun convertModule(moduleIr: ModuleIR): TaskResult<IdeaModule> {
        val module = modulesModel.newModule(
            (moduleIr.path / "${moduleIr.name}.iml").toString(),
            ModuleTypeId.JAVA_MODULE
        )
        val rootModel = ModuleRootManager.getInstance(module).modifiableModel
        val contentRoot = rootModel.addContentEntry(moduleIr.path.url)

        SourcesetType.ALL.forEach { sourceset ->
            val isTest = sourceset == SourcesetType.test
            contentRoot.addSourceFolder(
                (moduleIr.path / "src" / sourceset.name / "kotlin").url,
                if (isTest) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
            )
            contentRoot.addSourceFolder(
                (moduleIr.path / "src" / sourceset.name / "resources").url,
                if (isTest) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE
            )
        }

        rootModel.inheritSdk()
        rootModel.commit()
        addLibrariesToTheModule(moduleIr, module)
        return Success(module)
    }

    private fun addLibrariesToTheModule(moduleIr: ModuleIR, module: IdeaModule) {
        moduleIr.irs.forEach { ir ->
            if (ir is LibraryDependencyIR && !ir.isKotlinStdlib) {
                attachLibraryToModule(ir, module)
            }
        }
    }

    private fun addModuleDependencies(
        moduleIr: ModuleIR,
        module: com.intellij.openapi.module.Module,
        moduleNameToIdeaModuleMap: Map<String, IdeaModule>
    ) {
        moduleIr.irs.forEach { ir ->
            if (ir is ModuleDependencyIR) {
                attachModuleDependencyToModule(ir, module, moduleNameToIdeaModuleMap)
            }
        }
    }

    private fun attachModuleDependencyToModule(
        moduleDependency: ModuleDependencyIR,
        module: IdeaModule,
        moduleNameToIdeaModuleMap: Map<String, IdeaModule>
    ) {
        val dependencyName = moduleDependency.path.parts.lastOrNull() ?: return
        val dependencyModule = moduleNameToIdeaModuleMap[dependencyName] ?: return
        ModuleRootModificationUtil.addDependency(module, dependencyModule)
    }

    private fun attachLibraryToModule(
        libraryDependency: LibraryDependencyIR,
        module: IdeaModule
    ) {
        val artifact = libraryDependency.artifact as? MavenArtifact ?: return
        val libraryProperties = RepositoryLibraryProperties(
            artifact.groupId,
            artifact.artifactId,
            libraryDependency.version.toString()
        )
        val classesRoots = downloadLibraryAndGetItsClasses(libraryProperties)

        ModuleRootModificationUtil.addModuleLibrary(
            module,
            if (classesRoots.size > 1) libraryProperties.artifactId else null,
            OrderEntryFix.refreshAndConvertToUrls(classesRoots),
            emptyList(),
            when (libraryDependency.dependencyType) {
                DependencyType.MAIN -> DependencyScope.COMPILE
                DependencyType.TEST -> DependencyScope.TEST
            }
        )
    }

    private fun downloadLibraryAndGetItsClasses(libraryProperties: RepositoryLibraryProperties) =
        JarRepositoryManager.loadDependenciesModal(
            project,
            libraryProperties,
            false,
            false,
            librariesPath.toString(),
            null
        ).asSequence()
            .filter { it.type == OrderRootType.CLASSES }
            .map { PathUtil.getLocalPath(it.file) }
            .toList()
}

private val Path.url
    get() = VfsUtil.pathToUrl(toString())