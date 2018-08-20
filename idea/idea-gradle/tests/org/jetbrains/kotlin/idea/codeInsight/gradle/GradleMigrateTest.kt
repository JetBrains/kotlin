/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.KotlinMigrationProjectComponent
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.junit.Assert
import org.junit.Test

class GradleMigrateTest : GradleImportingTestCase() {
    @Test
    fun testMigrateStdlib() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val gradleFile = createProjectSubFile(
            "app/build.gradle",
            """
            buildscript {
                repositories {
                    jcenter()
                    mavenCentral()
                }
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0"
            }
            """.trimIndent()
        )

        importProject()

        val document = runReadAction {
            val gradlePsiFile = PsiManager.getInstance(myProject).findFile(gradleFile) ?: error("Can't find psi file for gradle file")
            PsiDocumentManager.getInstance(myProject).getDocument(gradlePsiFile) ?: error("Can't find document for gradle file")
        }

        runInEdtAndWait {
            runWriteAction {
                document.setText(
                    """
                    buildscript {
                        repositories {
                            jcenter()
                            mavenCentral()
                        }
                        dependencies {
                            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.0"
                        }
                    }

                    apply plugin: 'kotlin'

                    dependencies {
                        compile "org.jetbrains.kotlin:kotlin-stdlib:1.2.0"
                    }
                    """.trimIndent()
                )
            }
        }

        importProject()

        val actualMigrationInfo = KotlinMigrationProjectComponent.getInstance(myProject).requestLastMigrationInfo()

        Assert.assertEquals(
            MigrationInfo.create("1.1.0", ApiVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_2, newStdlibVersion = "1.2.0"),
            actualMigrationInfo)
    }
}