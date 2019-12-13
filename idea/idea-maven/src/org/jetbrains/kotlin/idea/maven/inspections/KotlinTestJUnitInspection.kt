/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomDependency
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavaMavenConfigurator
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator

class KotlinTestJUnitInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        val module = domFileElement.module ?: return
        val manager = MavenProjectsManager.getInstance(module.project) ?: return
        val mavenProject = manager.findProject(module) ?: return

        val hasJunit = mavenProject.dependencies.any { it.groupId == "junit" && it.artifactId == "junit" }
        if (!hasJunit) {
            return
        }

        val kotlinTestDependencies = domFileElement.rootElement.dependencies.dependencies.filter {
            it.groupId.rawText == KotlinMavenConfigurator.GROUP_ID && it.artifactId.rawText == KotlinJavaMavenConfigurator.TEST_LIB_ID
        }

        kotlinTestDependencies.forEach {
            holder.createProblem(
                it.artifactId,
                HighlightSeverity.WEAK_WARNING,
                "kotlin-test-junit is better with junit",
                ReplaceToKotlinTest(it)
            )
        }
    }

    private class ReplaceToKotlinTest(val dependency: MavenDomDependency) : LocalQuickFix {
        override fun getName() = "Replace with kotlin-test-junit"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (dependency.isValid) {
                dependency.artifactId.stringValue = "kotlin-test-junit"
            }
        }
    }
}