/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID
import java.util.*

class KotlinMavenPluginPhaseInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    override fun getStaticDescription() = "Reports kotlin-maven-plugin configuration issues"

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
                }
                else {
                    arrayOf(AddExecutionLocalFix(domFileElement.file, module, kotlinPlugin, PomFile.KotlinGoals.Compile),
                            AddExecutionLocalFix(domFileElement.file, module, kotlinPlugin, PomFile.KotlinGoals.Js))
                }

                holder.createProblem(kotlinPlugin.artifactId.createStableCopy(),
                                     HighlightSeverity.WARNING,
                                     "Kotlin plugin has no compile executions",
                                     *fixes)
            }
            else {
                if (hasJavaFiles) {
                    pom.findExecutions(kotlinPlugin, PomFile.KotlinGoals.Compile).notAtPhase(PomFile.DefaultPhases.ProcessSources).forEach { badExecution ->
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
                            || pom.isPluginExecutionMissing(javacPlugin, "default-testCompile", "testCompile")) {

                            holder.createProblem(badExecution.phase.createStableCopy(),
                                                 HighlightSeverity.WARNING,
                                                 "Kotlin plugin should run before javac so kotlin classes could be visible from Java",
                                                 FixExecutionPhaseLocalFix(badExecution, PomFile.DefaultPhases.ProcessSources),
                                                 AddJavaExecutionsLocalFix(module, domFileElement.file, kotlinPlugin))
                        }
                    }

                    pom.findExecutions(kotlinPlugin, PomFile.KotlinGoals.Js, PomFile.KotlinGoals.TestJs).forEach { badExecution ->
                        holder.createProblem(badExecution.goals.goals.first { it.isJsGoal() }.createStableCopy(),
                                             HighlightSeverity.WARNING,
                                             "JavaScript goal configured for module with Java files")
                    }
                }

                val stdlibDependencies = mavenProject.findDependencies(KotlinMavenConfigurator.GROUP_ID, MAVEN_STDLIB_ID)
                val jsDependencies = mavenProject.findDependencies(KotlinMavenConfigurator.GROUP_ID, MAVEN_JS_STDLIB_ID)

                if (hasJvmExecution && stdlibDependencies.isEmpty()) {
                    holder.createProblem(kotlinPlugin.artifactId.createStableCopy(),
                                         HighlightSeverity.WARNING,
                                         "Kotlin JVM compiler configured but no $MAVEN_STDLIB_ID dependency",
                                         FixAddStdlibLocalFix(domFileElement.file, MAVEN_STDLIB_ID, kotlinPlugin.version.rawText))
                }
                if (hasJsExecution && jsDependencies.isEmpty()) {
                    holder.createProblem(kotlinPlugin.artifactId.createStableCopy(),
                                         HighlightSeverity.WARNING,
                                         "Kotlin JavaScript compiler configured but no ${MAVEN_JS_STDLIB_ID} dependency",
                                         FixAddStdlibLocalFix(domFileElement.file, MAVEN_JS_STDLIB_ID, kotlinPlugin.version.rawText))
                }
            }
        }

        val stdlibDependencies = pom.findDependencies(MavenId(KotlinMavenConfigurator.GROUP_ID, MAVEN_STDLIB_ID, null))
        if (!hasJvmExecution && stdlibDependencies.isNotEmpty()) {
            stdlibDependencies.forEach { dep ->
                holder.createProblem(dep.artifactId.createStableCopy(),
                                     HighlightSeverity.WARNING,
                                     "You have ${dep.artifactId} configured but no corresponding plugin execution",
                                     ConfigurePluginExecutionLocalFix(module, domFileElement.file, PomFile.KotlinGoals.Compile, dep.version.rawText))
            }
        }

        val stdlibJsDependencies = pom.findDependencies(MavenId(KotlinMavenConfigurator.GROUP_ID, MAVEN_JS_STDLIB_ID, null))
        if (!hasJsExecution && stdlibJsDependencies.isNotEmpty()) {
            stdlibJsDependencies.forEach { dep ->
                holder.createProblem(dep.artifactId.createStableCopy(),
                                     HighlightSeverity.WARNING,
                                     "You have ${dep.artifactId} configured but no corresponding plugin execution",
                                     ConfigurePluginExecutionLocalFix(module, domFileElement.file, PomFile.KotlinGoals.Js, dep.version.rawText))
            }
        }

        pom.findKotlinExecutions().filter {
            it.goals.goals.any { it.rawText == PomFile.KotlinGoals.Compile || it.rawText == PomFile.KotlinGoals.Js }
            && it.goals.goals.any { it.rawText == PomFile.KotlinGoals.TestCompile || it.rawText == PomFile.KotlinGoals.TestJs }
        }.forEach { badExecution ->
            holder.createProblem(badExecution.goals.createStableCopy(),
                                 HighlightSeverity.WEAK_WARNING,
                                 "It is not recommended to have both test and compile goals in the same execution")
        }
    }

    private class AddExecutionLocalFix(val file: XmlFile, val module: Module, val kotlinPlugin: MavenDomPlugin, val goal: String) : LocalQuickFix {
        override fun getName() = "Create $goal execution"

        override fun getFamilyName() = "Create kotlin execution"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            PomFile.forFileOrNull(file)?.addKotlinExecution(module, kotlinPlugin, goal, PomFile.getPhase(module.hasJavaFiles(), false), false, listOf(goal))
        }
    }

    private class FixExecutionPhaseLocalFix(val execution: MavenDomPluginExecution, val newPhase: String) : LocalQuickFix {
        override fun getName() = "Change phase to $newPhase"

        override fun getFamilyName() = "Change phase"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            execution.phase.value = newPhase
        }
    }

    private class AddJavaExecutionsLocalFix(val module: Module, val file: XmlFile, val kotlinPlugin: MavenDomPlugin) : LocalQuickFix {
        override fun getName() = "Configure maven-compiler-plugin executions in the right order"
        override fun getFamilyName() = getName()

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            PomFile.forFileOrNull(file)?.addJavacExecutions(module, kotlinPlugin)
        }
    }

    private class FixAddStdlibLocalFix(val pomFile: XmlFile, val id: String, val version: String?) : LocalQuickFix {
        override fun getName() = "Add $id dependency"
        override fun getFamilyName() = "Add dependency"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            PomFile.forFileOrNull(pomFile)?.addDependency(MavenId(KotlinMavenConfigurator.GROUP_ID, id, version), MavenArtifactScope.COMPILE)
        }
    }

    private class ConfigurePluginExecutionLocalFix(val module: Module, val xmlFile: XmlFile, val goal: String, val version: String?) : LocalQuickFix {
        override fun getName() = "Create $goal execution of kotlin-maven-compiler"
        override fun getFamilyName() = "Create kotlin execution"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            PomFile.forFileOrNull(xmlFile)?.let { pom ->
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
