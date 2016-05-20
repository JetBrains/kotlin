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

package org.jetbrains.kotlin.idea.maven

import java.io.File

class KotlinMavenImporterTest : MavenImportingTestCase() {
    private val kotlinVersion = "1.0.0-beta-2423"

    override fun setUp() {
        super.setUp()
        repositoryPath = File(myDir, "repo").path
        createStdProjectFolders()
    }

    fun testSimpleKotlinProject() {
        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>
        """)

        assertModules("project")
        assertImporterStatePresent()
        assertSources("project", "src/main/java")
    }

    fun testWithSpecifiedSourceRoot() {
        createProjectSubDir("src/main/kotlin")

        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()
        assertSources("project", "src/main/kotlin")
    }

    fun testWithCustomSourceDirs() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>

                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/main/kotlin</dir>
                                    <dir>src/main/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>

                        <execution>
                            <id>test-compile</id>
                            <phase>test-compile</phase>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/test/kotlin</dir>
                                    <dir>src/test/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    fun testReImportRemoveDir() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>

                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/main/kotlin</dir>
                                    <dir>src/main/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>

                        <execution>
                            <id>test-compile</id>
                            <phase>test-compile</phase>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/test/kotlin</dir>
                                    <dir>src/test/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")

        // reimport
        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>

                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/main/kotlin</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>

                        <execution>
                            <id>test-compile</id>
                            <phase>test-compile</phase>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/test/kotlin</dir>
                                    <dir>src/test/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        """)

        assertSources("project", "src/main/kotlin")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    fun testReImportAddDir() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>

                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/main/kotlin</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>

                        <execution>
                            <id>test-compile</id>
                            <phase>test-compile</phase>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/test/kotlin</dir>
                                    <dir>src/test/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")

        // reimport
        importProject("""
        <groupId>test</groupId>
        <artifactId>project</artifactId>
        <version>1.0.0</version>

        <dependencies>
            <dependency>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-stdlib</artifactId>
                <version>$kotlinVersion</version>
            </dependency>
        </dependencies>

        <build>
            <sourceDirectory>src/main/kotlin</sourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>

                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/main/kotlin</dir>
                                    <dir>src/main/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>

                        <execution>
                            <id>test-compile</id>
                            <phase>test-compile</phase>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                            <configuration>
                                <sourceDirs>
                                    <dir>src/test/kotlin</dir>
                                    <dir>src/test/kotlin.jvm</dir>
                                </sourceDirs>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        """)

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    private fun assertImporterStatePresent() {
        assertNotNull("Kotlin importer component is not present", myTestFixture.module.getComponent(KotlinImporterComponent::class.java))
    }
}