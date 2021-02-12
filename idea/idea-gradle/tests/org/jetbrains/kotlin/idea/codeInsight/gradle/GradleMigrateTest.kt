/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.concurrency.FutureResult
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.KotlinMigrationProjectService
import org.jetbrains.kotlin.idea.configuration.KotlinMigrationProjectService.MigrationTestState
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class GradleMigrateTest : MultiplePluginVersionGradleImportingTestCase() {
    @Test
    @PluginTargetVersions(gradleVersion = "5.3+", pluginVersion = "1.4.0+")
    fun testMigrateStdlib() {
        val migrateComponentState = doMigrationTest(
            beforeText = """
            buildscript {
                repositories {
                    jcenter()
                    mavenCentral()
                }
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.40"
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.3.40"
            }
            """,
            afterText =
            """
            buildscript {
                repositories {
                    jcenter()
                    mavenCentral()
                    mavenLocal()
                }
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$gradleKotlinPluginVersion"
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:$gradleKotlinPluginVersion"
            }
            """
        )

        Assert.assertEquals(false, migrateComponentState?.hasApplicableTools)
        val newLanguageVersion = LanguageVersion.fromFullVersionString(gradleKotlinPluginVersion)!!
        Assert.assertEquals(
            MigrationInfo.create(
                oldStdlibVersion = "1.3.40",
                oldApiVersion = ApiVersion.KOTLIN_1_3,
                oldLanguageVersion = LanguageVersion.KOTLIN_1_3,
                newStdlibVersion = gradleKotlinPluginVersion,
                newApiVersion = ApiVersion.createByLanguageVersion(newLanguageVersion),
                newLanguageVersion = newLanguageVersion
            ),
            migrateComponentState?.migrationInfo
        )
    }

    private fun doMigrationTest(beforeText: String, afterText: String): MigrationTestState? {
        createProjectSubFile("settings.gradle", "include ':app'")
        val gradleFile = createProjectSubFile("app/build.gradle", beforeText.trimIndent())

        importProject()

        val document = runReadAction {
            val gradlePsiFile = PsiManager.getInstance(myProject).findFile(gradleFile) ?: error("Can't find psi file for gradle file")
            PsiDocumentManager.getInstance(myProject).getDocument(gradlePsiFile) ?: error("Can't find document for gradle file")
        }

        runInEdtAndWait {
            runWriteAction {
                document.setText(afterText.trimIndent())
            }
        }

        val importResult = FutureResult<MigrationTestState?>()
        val migrationProjectComponent = KotlinMigrationProjectService.getInstance(myProject)

        migrationProjectComponent.setImportFinishListener { migrationState ->
            importResult.set(migrationState)
        }

        importProject()

        return try {
            importResult.get(5, TimeUnit.SECONDS)
        } catch (te: TimeoutException) {
            throw IllegalStateException("No reply with result from migration component")
        } finally {
            migrationProjectComponent.setImportFinishListener(null)
        }
    }
}