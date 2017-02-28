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

import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.junit.Assert
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

    fun testJvmFacetConfiguration() {
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
                        </execution>
                    </executions>
                    <configuration>
                        <languageVersion>1.1</languageVersion>
                        <apiVersion>1.0</apiVersion>
                        <multiPlatform>true</multiPlatform>
                        <nowarn>true</nowarn>
                        <args>
                            <arg>-Xcoroutines=enable</arg>
                        </args>
                        <jvmTarget>1.8</jvmTarget>
                        <jdkHome>JDK_HOME</jdkHome>
                        <classpath>foobar.jar</classpath>
                    </configuration>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerInfo.commonCompilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerInfo.commonCompilerArguments!!.apiVersion)
            Assert.assertEquals(true, compilerInfo.commonCompilerArguments!!.suppressWarnings)
            Assert.assertEquals("enable", compilerInfo.coroutineSupport.compilerArgument)
            Assert.assertEquals("JVM 1.8", versionInfo.targetPlatformKind!!.description)
            Assert.assertEquals("1.8", compilerInfo.k2jvmCompilerArguments!!.jvmTarget)
            Assert.assertEquals("-cp foobar.jar -jdk-home JDK_HOME -Xmulti-platform",
                                compilerInfo.compilerSettings!!.additionalArguments)
        }
    }

    fun testJsFacetConfiguration() {
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
                                <goal>js</goal>
                            </goals>
                        </execution>
                    </executions>
                    <configuration>
                        <languageVersion>1.1</languageVersion>
                        <apiVersion>1.0</apiVersion>
                        <multiPlatform>true</multiPlatform>
                        <nowarn>true</nowarn>
                        <args>
                            <arg>-Xcoroutines=enable</arg>
                        </args>
                        <sourceMap>true</sourceMap>
                        <outputFile>test.js</outputFile>
                        <metaInfo>true</metaInfo>
                        <moduleKind>commonjs</moduleKind>
                    </configuration>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerInfo.commonCompilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerInfo.commonCompilerArguments!!.apiVersion)
            Assert.assertEquals(true, compilerInfo.commonCompilerArguments!!.suppressWarnings)
            Assert.assertEquals("enable", compilerInfo.coroutineSupport.compilerArgument)
            Assert.assertTrue(versionInfo.targetPlatformKind is TargetPlatformKind.JavaScript)
            Assert.assertEquals(true, compilerInfo.k2jsCompilerArguments!!.sourceMap)
            Assert.assertEquals("commonjs", compilerInfo.k2jsCompilerArguments!!.moduleKind)
            Assert.assertEquals("-output test.js -meta-info -Xmulti-platform",
                                compilerInfo.compilerSettings!!.additionalArguments)
        }
    }

    fun testFacetSplitConfiguration() {
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
                                <languageVersion>1.1</languageVersion>
                                <multiPlatform>true</multiPlatform>
                                <args>
                                    <arg>-Xcoroutines=enable</arg>
                                </args>
                                <jdkHome>JDK_HOME</jdkHome>
                                <classpath>foobar.jar</classpath>
                            </configuration>
                        </execution>
                    </executions>
                    <configuration>
                        <apiVersion>1.0</apiVersion>
                        <nowarn>true</nowarn>
                        <jvmTarget>1.8</jvmTarget>
                    </configuration>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerInfo.commonCompilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerInfo.commonCompilerArguments!!.apiVersion)
            Assert.assertEquals(true, compilerInfo.commonCompilerArguments!!.suppressWarnings)
            Assert.assertEquals("enable", compilerInfo.coroutineSupport.compilerArgument)
            Assert.assertEquals("JVM 1.8", versionInfo.targetPlatformKind!!.description)
            Assert.assertEquals("1.8", compilerInfo.k2jvmCompilerArguments!!.jvmTarget)
            Assert.assertEquals("-cp foobar.jar -jdk-home JDK_HOME -Xmulti-platform",
                                compilerInfo.compilerSettings!!.additionalArguments)
        }
    }

    fun testArgsInFacet() {
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
                        </execution>
                    </executions>
                    <configuration>
                        <args>
                            <arg>-jvm-target</arg>
                            <arg>1.8</arg>
                            <arg>-Xcoroutines=enable</arg>
                        </args>
                    </configuration>
                </plugin>
            </plugins>
        </build>
        """)

        assertModules("project")
        assertImporterStatePresent()

        with (facetSettings) {
            Assert.assertEquals("JVM 1.8", versionInfo.targetPlatformKind!!.description)
            Assert.assertEquals("1.8", compilerInfo.k2jvmCompilerArguments!!.jvmTarget)
            Assert.assertEquals("enable", compilerInfo.coroutineSupport.compilerArgument)
        }
    }

    private fun assertImporterStatePresent() {
        assertNotNull("Kotlin importer component is not present", myTestFixture.module.getComponent(KotlinImporterComponent::class.java))
    }

    private val facetSettings: KotlinFacetSettings
        get() = KotlinFacet.get(getModule("project"))!!.configuration.settings
}