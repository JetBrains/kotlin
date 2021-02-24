/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class JpsCompatiblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configurations.maybeCreate(EmbeddedComponents.CONFIGURATION_NAME)
        project.extensions.create("pill", PillExtension::class.java)

        // 'jpsTest' does not require the 'tests-jar' artifact
        project.configurations.create("jpsTest")

        if (project == project.rootProject) {
            project.tasks.create("pill") {
                dependsOn(":pill:pill-importer:pill")

                if (System.getProperty("pill.android.tests", "false") == "true") {
                    TaskUtils.useAndroidSdk(this)
                    TaskUtils.useAndroidJar(this)
                }
            }

            project.tasks.create("unpill") {
                dependsOn(":pill:pill-importer:unpill")
            }
        }
    }
}