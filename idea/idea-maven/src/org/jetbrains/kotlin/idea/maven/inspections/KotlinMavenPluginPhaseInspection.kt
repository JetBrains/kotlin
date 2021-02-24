/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomGoal
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.dom.model.MavenDomPluginExecution
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.kotlin.idea.maven.KotlinMavenBundle
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import java.util.*

class KotlinMavenPluginPhaseInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    companion object {
        private val JVM_STDLIB_IDS = JvmIdePlatformKind.tooling
            .mavenLibraryIds.map { MavenId(KotlinMavenConfigurator.GROUP_ID, it, null) }

        private val JS_STDLIB_MAVEN_ID = MavenId(KotlinMavenConfigurator.GROUP_ID, MAVEN_JS_STDLIB_ID, null)
    }

    override fun getStaticDescription() = KotlinMavenBundle.message("inspection.description")

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        val module = domFileElement.module ?: return
        val manager = MavenProjectsManager.getInstance(module.project)
        val mavenProject = manager.findProject(module) ?: return

        val pom = PomFile.forFileOrNull(domFileElement.file) ?: return
        val hasJavaFiles = module.hasJavaFiles()

        // all executions including inherited
        val executions = mavenProject.plugins
            .filter { it.isKotlinMavenPlugin() }
            .flatMap { it.executions }
        val allGoalsSet: Set<String> = executions.flatMapTo(HashSet()) { it.goals }
        val hasJvmExecution = PomFile.KotlinGoals.Compile in allGoalsSet || PomFile.KotlinGoals.TestCompile in allGoalsSet
        val hasJsExecution = PomFile.KotlinGoals.Js in allGoalsSet || PomFile.KotlinGoals.TestJs in allGoalsSet

        val pomKotlinPlugins = pom.findKotlinPlugins()

        for (kotlinPlugin in pomKotlinPlugins) {
            if (PomFile.KotlinGoals.Compile !in allGoalsSet && PomFile.KotlinGoals.Js !in allGoalsSet) {
                val fixes = if (hasJavaFiles) {
                    arrayOf(AddExecutionLocalFix(domFileElement.file, module, kotlinPlugin, PomFile.KotlinGoals.Compile))
                } else {
                    arrayOf(
                        AddExecutionLocalFix(domFileElement.file, module, kotlinPlugin, PomFile.KotlinGoals.Compile),
                        AddExecutionLocalFix(domFileElement.file, module, kotlinPlugin, PomFile.KotlinGoals.Js)
                    )
                }

                holder.createProblem(
                    kotlinPlugin.artifactId.createStableCopy(),
                    HighlightSeverity.WARNING,
                    KotlinMavenBundle.message("inspection.no.executions"),
                    *fixes
                )
            } else {
                if (hasJavaFiles) {
                    pom.findExecutions(kotlinPlugin, PomFile.KotlinGoals.Compile).notAtPhase(PomFile.DefaultPhases.ProcessSources)
                        .forEach { badExecution ->
                            val javacPlugin = mavenProject.findPlugin("org.apache.maven.plugins", "maven-compiler-plugin")
                            val existingJavac = pom.domModel.build.plugins.plugins.firstOrNull {
                                it.groupId.stringValue == "org.apache.maven.plugins" &&
                                        it.artifactId.stringValue == "maven-compiler-plugin"
                            }

                            if (existingJavac == null
                                || !pom.isPluginAfter(existingJavac, kotlinPlugin)
                                || pom.isExecutionEnabled(javacPlugin, "default-compile")
                                || pom.isExecutionEnabled(javacPlugin, "default-testCompile")
                                || pom.isPluginExecutionMissing(javacPlugin, "default-compile", "compile")
                                || pom.isPluginExecutionMissing(javacPlugin, "default-testCompile", "testCompile")
                            ) {

                                holder.createProblem(
                                    badExecution.phase.createStableCopy(),
                                    HighlightSeverity.WARNING,
                                    KotlinMavenBundle.message("inspection.should.run.before.javac"),
                                    FixExecutionPhaseLocalFix(badExecution, PomFile.DefaultPhases.ProcessSources),
                                    AddJavaExecutionsLocalFix(module, domFileElement.file, kotlinPlugin)
                                )
                            }
                        }

                    pom.findExecutions(kotlinPlugin, PomFile.KotlinGoals.Js, PomFile.KotlinGoals.TestJs).forEach { badExecution ->
                        holder.createProblem(
                            badExecution.goals.goals.first { it.isJsGoal() }.createStableCopy(),
                            HighlightSeverity.WARNING,
                            KotlinMavenBundle.message("inspection.javascript.in.java.module")
                        )
                    }
                }

                if (hasJvmExecution && pom.findDependencies(JVM_STDLIB_IDS).isEmpty()) {
                    val stdlibDependencies = mavenProject.findDependencies(KotlinMavenConfigurator.GROUP_ID, MAVEN_STDLIB_ID)
                    if (stdlibDependencies.isEmpty()) {
                        holder.createProblem(
                            kotlinPlugin.artifactId.createStableCopy(),
                            HighlightSeverity.WARNING,
                            KotlinMavenBundle.message("inspection.jvm.no.stdlib.dependency", MAVEN_STDLIB_ID),
                            FixAddStdlibLocalFix(domFileElement.file, MAVEN_STDLIB_ID, kotlinPlugin.version.rawText)
                        )
                    }
                }

                if (hasJsExecution && pom.findDependencies(JVM_STDLIB_IDS).isEmpty()) {
                    val jsDependencies = mavenProject.findDependencies(KotlinMavenConfigurator.GROUP_ID, MAVEN_JS_STDLIB_ID)
                    if (jsDependencies.isEmpty()) {
                        holder.createProblem(
                            kotlinPlugin.artifactId.createStableCopy(),
                            HighlightSeverity.WARNING,
                            KotlinMavenBundle.message("inspection.javascript.no.stdlib.dependency", MAVEN_JS_STDLIB_ID),
                            FixAddStdlibLocalFix(domFileElement.file, MAVEN_JS_STDLIB_ID, kotlinPlugin.version.rawText)
                        )
                    }
                }
            }
        }

        val jvmStdlibDependencies = pom.findDependencies(JVM_STDLIB_IDS)
        if (!hasJvmExecution && jvmStdlibDependencies.isNotEmpty()) {
            jvmStdlibDependencies.forEach { dep ->
                holder.createProblem(
                    dep.artifactId.createStableCopy(),
                    HighlightSeverity.WARNING,
                    KotlinMavenBundle.message("inspection.configured.no.execution", dep.artifactId),
                    ConfigurePluginExecutionLocalFix(module, domFileElement.file, PomFile.KotlinGoals.Compile, dep.version.rawText)
                )
            }
        }

        val stdlibJsDependencies = pom.findDependencies(JS_STDLIB_MAVEN_ID)
        if (!hasJsExecution && stdlibJsDependencies.isNotEmpty()) {
            stdlibJsDependencies.forEach { dep ->
                holder.createProblem(
                    dep.artifactId.createStableCopy(),
                    HighlightSeverity.WARNING,
                    KotlinMavenBundle.message("inspection.configured.no.execution", dep.artifactId),
                    ConfigurePluginExecutionLocalFix(module, domFileElement.file, PomFile.KotlinGoals.Js, dep.version.rawText)
                )
            }
        }

        pom.findKotlinExecutions().filter {
            it.goals.goals.any { goal ->
                goal.rawText == PomFile.KotlinGoals.Compile || goal.rawText == PomFile.KotlinGoals.Js
            } && it.goals.goals.any { goal ->
                goal.rawText == PomFile.KotlinGoals.TestCompile || goal.rawText == PomFile.KotlinGoals.TestJs
            }
        }.forEach { badExecution ->
            holder.createProblem(
                badExecution.goals.createStableCopy(),
                HighlightSeverity.WEAK_WARNING,
                KotlinMavenBundle.message("inspection.same.execution.compile.test")
            )
        }
    }

    private class AddExecutionLocalFix(
        file: XmlFile,
        val module: Module,
        val kotlinPlugin: MavenDomPlugin,
        val goal: String
    ) : LocalQuickFix {
        private val pointer = file.createSmartPointer()

        override fun getName() = KotlinMavenBundle.message("fix.add.execution.name", goal)

        override fun getFamilyName() = KotlinMavenBundle.message("fix.add.execution.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = pointer.element ?: return
            PomFile.forFileOrNull(file)
                ?.addKotlinExecution(module, kotlinPlugin, goal, PomFile.getPhase(module.hasJavaFiles(), false), false, listOf(goal))
        }
    }

    private class FixExecutionPhaseLocalFix(val execution: MavenDomPluginExecution, val newPhase: String) : LocalQuickFix {
        override fun getName() = KotlinMavenBundle.message("fix.execution.phase.name", newPhase)

        override fun getFamilyName() = KotlinMavenBundle.message("fix.execution.phase.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            execution.phase.value = newPhase
        }
    }

    private class AddJavaExecutionsLocalFix(val module: Module, file: XmlFile, val kotlinPlugin: MavenDomPlugin) : LocalQuickFix {
        private val pointer = file.createSmartPointer()

        override fun getName() = KotlinMavenBundle.message("fix.add.java.executions.name")
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = pointer.element ?: return
            PomFile.forFileOrNull(file)?.addJavacExecutions(module, kotlinPlugin)
        }
    }

    private class FixAddStdlibLocalFix(pomFile: XmlFile, val id: String, val version: String?) : LocalQuickFix {
        private val pointer = pomFile.createSmartPointer()

        override fun getName() = KotlinMavenBundle.message("fix.add.stdlib.name", id)
        override fun getFamilyName() = KotlinMavenBundle.message("fix.add.stdlib.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = pointer.element ?: return
            PomFile.forFileOrNull(file)?.addDependency(MavenId(KotlinMavenConfigurator.GROUP_ID, id, version), MavenArtifactScope.COMPILE)
        }
    }

    private class ConfigurePluginExecutionLocalFix(
        val module: Module,
        xmlFile: XmlFile,
        val goal: String,
        val version: String?
    ) : LocalQuickFix {
        private val pointer = xmlFile.createSmartPointer()

        override fun getName() = KotlinMavenBundle.message("fix.configure.plugin.execution.name", goal)
        override fun getFamilyName() = KotlinMavenBundle.message("fix.configure.plugin.execution.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = pointer.element ?: return
            PomFile.forFileOrNull(file)?.let { pom ->
                val plugin = pom.addKotlinPlugin(version)
                pom.addKotlinExecution(module, plugin, "compile", PomFile.getPhase(module.hasJavaFiles(), false), false, listOf(goal))
            }
        }
    }
}

fun Module.hasJavaFiles(): Boolean {
    return FileTypeIndex.containsFileOfType(JavaFileType.INSTANCE, GlobalSearchScope.moduleScope(this))
}

private fun MavenPlugin.isKotlinMavenPlugin() = groupId == KotlinMavenConfigurator.GROUP_ID
        && artifactId == KotlinMavenConfigurator.MAVEN_PLUGIN_ID

private fun MavenDomGoal.isJsGoal() = rawText == PomFile.KotlinGoals.Js || rawText == PomFile.KotlinGoals.TestJs

private fun List<MavenDomPluginExecution>.notAtPhase(phase: String) = filter { it.phase.stringValue != phase }
