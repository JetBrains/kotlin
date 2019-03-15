/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.FutureResult
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.KotlinMigrationProjectComponent
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.junit.Assert
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MavenMigrateTest : MavenImportingTestCase() {
    override fun setUp() {
        super.setUp()
        repositoryPath = File(myDir, "repo").path
        createStdProjectFolders()
    }

    fun testMigrateApiAndLanguageVersions() {
        val pomFile = createProjectPom(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <properties>
                <kotlin.version>1.2.50</kotlin.version>
            </properties>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib</artifactId>
                    <version>${'$'}{kotlin.version}</version>
                </dependency>
            </dependencies>

            <build>
                <plugins>
                    <plugin>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <version>${'$'}{kotlin.version}</version>
                    </plugin>
                </plugins>
            </build>
            """.trimIndent()
        )

        importProject()

        val document = runReadAction {
            val pomPsiFile = PsiManager.getInstance(myProject).findFile(pomFile) ?: error("Can't find psi file for pom file")
            PsiDocumentManager.getInstance(myProject).getDocument(pomPsiFile) ?: error("Can't find document for pom file")
        }

        runInEdtAndWait {
            runWriteAction {
                document.setText(
                    MavenTestCase.createPomXml(
                        """
                        <groupId>test</groupId>
                        <artifactId>project</artifactId>
                        <version>1.0.0</version>

                        <properties>
                            <kotlin.version>1.2.50</kotlin.version>
                            <kotlin.compiler.apiVersion>1.3</kotlin.compiler.apiVersion>
                            <kotlin.compiler.languageVersion>1.3</kotlin.compiler.languageVersion>
                        </properties>

                        <dependencies>
                            <dependency>
                                <groupId>org.jetbrains.kotlin</groupId>
                                <artifactId>kotlin-stdlib</artifactId>
                                <version>${'$'}{kotlin.version}</version>
                            </dependency>
                        </dependencies>

                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>kotlin-maven-plugin</artifactId>
                                    <groupId>org.jetbrains.kotlin</groupId>
                                    <version>${'$'}{kotlin.version}/version>
                                </plugin>
                            </plugins>
                        </build>
                        """.trimIndent()
                    )
                )
            }
        }

        val importResult = FutureResult<KotlinMigrationProjectComponent.MigrationTestState?>()
        val migrationProjectComponent = KotlinMigrationProjectComponent.getInstanceIfNotDisposed(myProject)
            ?: error("Disposed project")

        migrationProjectComponent.setImportFinishListener { migrationState ->
            importResult.set(migrationState)
        }

        importProject()

        val migrationTestState = try {
            importResult.get(5, TimeUnit.SECONDS)
        } catch (te: TimeoutException) {
            throw IllegalStateException("No reply with result from migration component")
        } finally {
            migrationProjectComponent.setImportFinishListener(null)
        }

        Assert.assertEquals(
            MigrationInfo.create(
                "1.2.50", ApiVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_2,
                newApiVersion = ApiVersion.KOTLIN_1_3, newLanguageVersion = LanguageVersion.KOTLIN_1_3
            ),
            migrationTestState?.migrationInfo
        )
    }
}