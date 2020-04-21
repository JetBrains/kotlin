/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.kotlinCodeStyleDefaults
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.oldFashionedDescription
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.Assert
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinMavenImporterTest : MavenImportingTestCase() {
    private val kotlinVersion = "1.1.3"

    override fun setUp() {
        super.setUp()
        repositoryPath = File(myDir, "repo").path
        createStdProjectFolders()
    }

    fun testSimpleKotlinProject() {
        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()
        assertSources("project", "src/main/java")
    }

    fun testWithSpecifiedSourceRoot() {
        createProjectSubDir("src/main/kotlin")

        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()
        assertSources("project", "src/main/kotlin")
    }

    fun testWithCustomSourceDirs() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    fun testWithKapt() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                <plugins>
                    <plugin>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-plugin</artifactId>

                        <executions>
                            <execution>
                                <id>kapt</id>
                                <goals>
                                    <goal>kapt</goal>
                                </goals>
                                <configuration>
                                    <sourceDirs>
                                        <sourceDir>src/main/kotlin</sourceDir>
                                        <sourceDir>src/main/java</sourceDir>
                                    </sourceDirs>
                                </configuration>
                            </execution>

                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                                <configuration>
                                    <sourceDirs>
                                        <sourceDir>src/main/kotlin</sourceDir>
                                        <sourceDir>src/main/java</sourceDir>
                                    </sourceDirs>
                                </configuration>
                            </execution>

                            <execution>
                                <id>test-kapt</id>
                                <goals>
                                    <goal>test-kapt</goal>
                                </goals>
                                <configuration>
                                    <sourceDirs>
                                        <sourceDir>src/test/kotlin</sourceDir>
                                        <sourceDir>src/test/java</sourceDir>
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
                                        <sourceDir>src/test/kotlin</sourceDir>
                                        <sourceDir>src/test/java</sourceDir>
                                        <sourceDir>target/generated-sources/kapt/test</sourceDir>
                                    </sourceDirs>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/java", "src/main/kotlin")
        assertTestSources("project", "src/test/java", "src/test/kotlin")
    }

    fun testImportObsoleteCodeStyle() {
        Assert.assertNull(CodeStyle.getSettings(myProject).kotlinCodeStyleDefaults())

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <properties>
                <kotlin.code.style>obsolete</kotlin.code.style>
            </properties>
            """
        )

        Assert.assertEquals(
            KotlinObsoleteCodeStyle.CODE_STYLE_ID,
            CodeStyle.getSettings(myProject).kotlinCodeStyleDefaults()
        )
    }

    fun testImportOfficialCodeStyle() {
        Assert.assertNull(CodeStyle.getSettings(myProject).kotlinCodeStyleDefaults())

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <properties>
                <kotlin.code.style>official</kotlin.code.style>
            </properties>
            """
        )

        Assert.assertEquals(
            KotlinStyleGuideCodeStyle.CODE_STYLE_ID,
            CodeStyle.getSettings(myProject).kotlinCodeStyleDefaults()
        )
    }

    fun testReImportRemoveDir() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")

        // reimport
        importProject(
            """
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
            """
        )

        assertSources("project", "src/main/kotlin")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    fun testReImportAddDir() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        assertSources("project", "src/main/kotlin")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")

        // reimport
        importProject(
            """
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
            """
        )

        assertSources("project", "src/main/kotlin", "src/main/kotlin.jvm")
        assertTestSources("project", "src/test/java", "src/test/kotlin", "src/test/kotlin.jvm")
    }

    fun testJvmFacetConfiguration() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                            <classpath>foobar.jar</classpath>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerArguments!!.apiVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertEquals(true, compilerArguments!!.suppressWarnings)
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("foobar.jar", (compilerArguments as K2JVMCompilerArguments).classpath)
            Assert.assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
        }

        assertContentFolders("project", JavaSourceRootType.SOURCE, "src/main/kotlin")
        assertContentFolders("project", JavaSourceRootType.TEST_SOURCE, "src/test/java")
        assertContentFolders("project", JavaResourceRootType.RESOURCE, "src/main/resources")
        assertContentFolders("project", JavaResourceRootType.TEST_RESOURCE, "src/test/resources")
    }

    fun testJavaParameters() {
        createProjectSubDirs("src/main/kotlin")

        importProject(
            """
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
                            <javaParameters>true</javaParameters>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("-java-parameters", compilerSettings!!.additionalArguments)
            Assert.assertTrue((mergedCompilerArguments as K2JVMCompilerArguments).javaParameters)
        }
    }

    fun testJvmFacetConfigurationFromProperties() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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

            <properties>
                <kotlin.compiler.languageVersion>1.0</kotlin.compiler.languageVersion>
                <kotlin.compiler.apiVersion>1.0</kotlin.compiler.apiVersion>
                <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget>
            </properties>

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
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("1.0", languageLevel!!.versionString)
            Assert.assertEquals("1.0", compilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerArguments!!.apiVersion)
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
        }

        assertContentFolders("project", JavaSourceRootType.SOURCE, "src/main/kotlin")
        assertContentFolders("project", JavaSourceRootType.TEST_SOURCE, "src/test/java")
        assertContentFolders("project", JavaResourceRootType.RESOURCE, "src/main/resources")
        assertContentFolders("project", JavaResourceRootType.TEST_RESOURCE, "src/test/resources")
    }

    fun testJsFacetConfiguration() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerArguments!!.apiVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertEquals(true, compilerArguments!!.suppressWarnings)
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
            Assert.assertTrue(targetPlatform.isJs())
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(true, sourceMap)
                Assert.assertEquals("commonjs", moduleKind)
            }
            Assert.assertEquals(
                "-meta-info -output test.js",
                compilerSettings!!.additionalArguments
            )
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJsCustomOutputPaths() {
        createProjectSubDirs("src/main/kotlin", "src/test/kotlin")
        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
                    <version>$kotlinVersion</version>
                </dependency>
            </dependencies>

            <build>
                <sourceDirectory>src/main/kotlin</sourceDirectory>

                <plugins>
                    <plugin>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <version>$kotlinVersion</version>

                        <executions>
                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <goals>
                                    <goal>js</goal>
                                </goals>
                                <configuration>
                                    <outputFile>${'$'}{project.basedir}/prod/main.js</outputFile>
                                </configuration>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <phase>test-compile</phase>
                                <goals>
                                    <goal>test-js</goal>
                                </goals>
                                <configuration>
                                    <outputFile>${'$'}{project.basedir}/test/test.js</outputFile>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        val projectBasePath = myProjectsManager.projects.first().file.parent.path

        with(facetSettings) {
            Assert.assertEquals("$projectBasePath/prod/main.js", PathUtil.toSystemIndependentName(productionOutputPath))
            Assert.assertEquals("$projectBasePath/test/test.js", PathUtil.toSystemIndependentName(testOutputPath))
        }

        with(CompilerModuleExtension.getInstance(getModule("project"))!!) {
            Assert.assertEquals("$projectBasePath/prod", PathUtil.toSystemIndependentName(compilerOutputUrl))
            Assert.assertEquals("$projectBasePath/test", PathUtil.toSystemIndependentName(compilerOutputUrlForTests))
        }
    }

    fun testFacetSplitConfiguration() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", compilerArguments!!.languageVersion)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals("1.0", compilerArguments!!.apiVersion)
            Assert.assertEquals(true, compilerArguments!!.suppressWarnings)
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("foobar.jar", (compilerArguments as K2JVMCompilerArguments).classpath)
            Assert.assertEquals("-version", compilerSettings!!.additionalArguments)
        }
    }

    fun testArgsInFacet() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <arg>-classpath</arg>
                                <arg>c:\program files\jdk1.8</arg>
                            </args>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
            Assert.assertEquals("c:/program files/jdk1.8", (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    fun testArgsInFacetInSingleElement() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                -jvm-target 1.8 -Xcoroutines=enable -classpath "c:\program files\jdk1.8"
                            </args>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
            Assert.assertEquals("c:/program files/jdk1.8", (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    fun testJvmDetectionByGoalWithJvmStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertEquals(JvmPlatforms.jvm16, facetSettings.targetPlatform)

        assertContentFolders("project", JavaSourceRootType.SOURCE, "src/main/kotlin")
        assertContentFolders("project", JavaSourceRootType.TEST_SOURCE, "src/test/java")
        assertContentFolders("project", JavaResourceRootType.RESOURCE, "src/main/resources")
        assertContentFolders("project", JavaResourceRootType.TEST_RESOURCE, "src/test/resources")
    }

    fun testJvmDetectionByGoalWithJsStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertEquals(JvmPlatforms.jvm16, facetSettings.targetPlatform)
    }

    fun testJvmDetectionByGoalWithCommonStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-common</artifactId>
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
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertEquals(JvmPlatforms.jvm16, facetSettings.targetPlatform)

        assertContentFolders("project", JavaSourceRootType.SOURCE, "src/main/kotlin")
        assertContentFolders("project", JavaSourceRootType.TEST_SOURCE, "src/test/java")
        assertContentFolders("project", JavaResourceRootType.RESOURCE, "src/main/resources")
        assertContentFolders("project", JavaResourceRootType.TEST_RESOURCE, "src/test/resources")
    }

    fun testJsDetectionByGoalWithJvmStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-js</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isJs())

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJsDetectionByGoalWithJsStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-js</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isJs())

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJsDetectionByGoalWithCommonStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-common</artifactId>
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-js</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isJs())

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJsAndCommonStdlibKinds() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-common</artifactId>
                    <version>$kotlinVersion</version>
                </dependency>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-js</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isJs())

        val rootManager = ModuleRootManager.getInstance(getModule("project"))
        val libraries = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().map { it.library as LibraryEx }
        assertEquals(JSLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-js") == true }.kind)
        assertEquals(CommonLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-common") == true }.kind)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testCommonDetectionByGoalWithJvmStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>metadata</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isCommon())

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testCommonDetectionByGoalWithJsStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
                                <goals>
                                    <goal>metadata</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isCommon())

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testCommonDetectionByGoalWithCommonStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>0
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-common</artifactId>
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
                                <goals>
                                    <goal>metadata</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isCommon())

        val rootManager = ModuleRootManager.getInstance(getModule("project"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        Assert.assertTrue(ModuleRootManager.getInstance(getModule("project")).sdk!!.sdkType is KotlinSdkType)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJvmDetectionByConflictingGoalsAndJvmStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertEquals(JvmPlatforms.jvm16, facetSettings.targetPlatform)

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testJsDetectionByConflictingGoalsAndJsStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isJs())

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testCommonDetectionByConflictingGoalsAndCommonStdlib() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-common</artifactId>
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        Assert.assertTrue(facetSettings.targetPlatform.isCommon())

        assertContentFolders("project", SourceKotlinRootType, "src/main/kotlin")
        assertContentFolders("project", TestSourceKotlinRootType, "src/test/java")
        assertContentFolders("project", ResourceKotlinRootType, "src/main/resources")
        assertContentFolders("project", TestResourceKotlinRootType, "src/test/resources")
    }

    fun testNoPluginsInAdditionalArgs() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                        </executions>

                        <dependencies>
                            <dependency>
                                <groupId>org.jetbrains.kotlin</groupId>
                                <artifactId>kotlin-maven-allopen</artifactId>
                                <version>$kotlinVersion</version>
                            </dependency>
                        </dependencies>

                        <configuration>
                            <compilerPlugins>
                                <plugin>spring</plugin>
                            </compilerPlugins>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
            Assert.assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.stereotype.Component",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.transaction.annotation.Transactional",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.scheduling.annotation.Async",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.cache.annotation.Cacheable",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.boot.test.context.SpringBootTest",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.validation.annotation.Validated"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    fun testNoArgInvokeInitializers() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <goals>
                                    <goal>js</goal>
                                </goals>
                            </execution>
                        </executions>

                        <dependencies>
                            <dependency>
                                <groupId>org.jetbrains.kotlin</groupId>
                                <artifactId>kotlin-maven-noarg</artifactId>
                                <version>$kotlinVersion</version>
                            </dependency>
                        </dependencies>

                        <configuration>
                            <compilerPlugins>
                                <plugin>no-arg</plugin>
                            </compilerPlugins>

                            <pluginOptions>
                                <option>no-arg:annotation=NoArg</option>
                                <option>no-arg:invokeInitializers=true</option>
                            </pluginOptions>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
            Assert.assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.noarg:annotation=NoArg",
                    "plugin:org.jetbrains.kotlin.noarg:invokeInitializers=true"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    fun testArgsOverridingInFacet() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                            <jvmTarget>1.6</jvmTarget>
                            <languageVersion>1.0</languageVersion>
                            <apiVersion>1.0</apiVersion>
                            <args>
                                <arg>-jvm-target</arg>
                                <arg>1.8</arg>
                                <arg>-language-version</arg>
                                <arg>1.1</arg>
                                <arg>-api-version</arg>
                                <arg>1.1</arg>
                            </args>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_1.description, languageLevel!!.description)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_1.description, apiLevel!!.description)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
        }
    }

    fun testSubmoduleArgsInheritance() {
        createProjectSubDirs("src/main/kotlin", "myModule1/src/main/kotlin", "myModule2/src/main/kotlin", "myModule3/src/main/kotlin")

        val mainPom = createProjectPom(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>
            <packaging>pom</packaging>

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
                            <jvmTarget>1.7</jvmTarget>
                            <languageVersion>1.1</languageVersion>
                            <apiVersion>1.0</apiVersion>
                            <args>
                                <arg>-java-parameters</arg>
                                <arg>-Xdump-declarations-to=dumpDir</arg>
                                <arg>-kotlin-home</arg>
                                <arg>temp</arg>
                            </args>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        val modulePom1 = createModulePom(
            "myModule1",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>myModule1</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                </dependencies>

                <build>
                    <sourceDirectory>myModule1/src/main/kotlin</sourceDirectory>

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
                                <jvmTarget>1.8</jvmTarget>
                                <args>
                                    <arg>-Xdump-declarations-to=dumpDir2</arg>
                                </args>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        val modulePom2 = createModulePom(
            "myModule2",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>myModule2</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                </dependencies>

                <build>
                    <sourceDirectory>myModule2/src/main/kotlin</sourceDirectory>

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
                                <jvmTarget>1.8</jvmTarget>
                                <args combine.children="append">
                                    <arg>-kotlin-home</arg>
                                    <arg>temp2</arg>
                                </args>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        val modulePom3 = createModulePom(
            "myModule3",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>myModule3</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                </dependencies>

                <build>
                    <sourceDirectory>myModule3/src/main/kotlin</sourceDirectory>

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

                            <configuration combine.self="override">
                                <jvmTarget>1.8</jvmTarget>
                                <args>
                                    <arg>-kotlin-home</arg>
                                    <arg>temp2</arg>
                                </args>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        importProjects(mainPom, modulePom1, modulePom2, modulePom3)

        assertModules("project", "myModule1", "myModule2", "myModule3")
        assertImporterStatePresent()

        with(facetSettings("myModule1")) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_1, languageLevel!!)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_0, apiLevel!!)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                listOf("-Xdump-declarations-to=dumpDir2"),
                compilerSettings!!.additionalArgumentsAsList
            )
        }

        with(facetSettings("myModule2")) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_1, languageLevel!!)
            Assert.assertEquals(LanguageVersion.KOTLIN_1_0, apiLevel!!)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                listOf("-Xdump-declarations-to=dumpDir", "-java-parameters", "-kotlin-home", "temp2"),
                compilerSettings!!.additionalArgumentsAsList
            )
        }

        with(facetSettings("myModule3")) {
            Assert.assertEquals("JVM 1.8", targetPlatform!!.oldFashionedDescription)
            Assert.assertEquals(LanguageVersion.LATEST_STABLE, languageLevel)
            Assert.assertEquals(LanguageVersion.LATEST_STABLE, apiLevel)
            Assert.assertEquals("1.8", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                listOf("-kotlin-home", "temp2"),
                compilerSettings!!.additionalArgumentsAsList
            )
        }
    }

    fun testMultiModuleImport() {
        createProjectSubDirs(
            "src/main/kotlin",
            "my-common-module/src/main/kotlin",
            "my-jvm-module/src/main/kotlin",
            "my-js-module/src/main/kotlin"
        )

        val mainPom = createProjectPom(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>
            <packaging>pom</packaging>

            <modules>
                <module>my-common-module</module>
                <module>my-jvm-module</module>
                <module>my-js-module</module>
            </modules>

            <build>
                <sourceDirectory>src/main/kotlin</sourceDirectory>

                <plugins>
                    <plugin>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <version>$kotlinVersion</version>
                    </plugin>
                </plugins>
            </build>
            """
        )

        val commonModule1 = createModulePom(
            "my-common-module1",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>my-common-module1</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib-common</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-maven-plugin</artifactId>

                            <executions>
                                <execution>
                                    <id>meta</id>
                                    <phase>compile</phase>
                                    <goals>
                                        <goal>metadata</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        val commonModule2 = createModulePom(
            "my-common-module2",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>my-common-module2</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib-common</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-maven-plugin</artifactId>

                            <executions>
                                <execution>
                                    <id>meta</id>
                                    <phase>compile</phase>
                                    <goals>
                                        <goal>metadata</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        val jvmModule = createModulePom(
            "my-jvm-module",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>my-jvm-module</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                    <dependency>
                        <groupId>test</groupId>
                        <artifactId>my-common-module1</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>test</groupId>
                        <artifactId>my-common-module2</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>

                <build>
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
                        </plugin>
                    </plugins>
                </build>
                """
        )

        val jsModule = createModulePom(
            "my-js-module",
            """

                <parent>
                    <groupId>test</groupId>
                    <artifactId>project</artifactId>
                    <version>1.0.0</version>
                </parent>

                <groupId>test</groupId>
                <artifactId>my-js-module</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib-js</artifactId>
                        <version>$kotlinVersion</version>
                    </dependency>
                    <dependency>
                        <groupId>test</groupId>
                        <artifactId>my-common-module1</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-maven-plugin</artifactId>

                            <executions>
                                <execution>
                                    <id>js</id>
                                    <phase>compile</phase>
                                    <goals>
                                        <goal>js</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
                """
        )

        importProjects(mainPom, commonModule1, commonModule2, jvmModule, jsModule)

        assertModules("project", "my-common-module1", "my-common-module2", "my-jvm-module", "my-js-module")
        assertImporterStatePresent()

        with(facetSettings("my-common-module1")) {
            Assert.assertEquals(CommonPlatforms.defaultCommonPlatform, targetPlatform)
        }

        with(facetSettings("my-common-module2")) {
            Assert.assertEquals(CommonPlatforms.defaultCommonPlatform, targetPlatform)
        }

        with(facetSettings("my-jvm-module")) {
            Assert.assertEquals(JvmPlatforms.jvm16, targetPlatform)
            Assert.assertEquals(listOf("my-common-module1", "my-common-module2"), implementedModuleNames)
        }

        with(facetSettings("my-js-module")) {
            Assert.assertEquals(JsPlatforms.defaultJsPlatform, targetPlatform)
            Assert.assertEquals(listOf("my-common-module1"), implementedModuleNames)
        }
    }

    fun testJDKImport() {
        object : WriteAction<Unit>() {
            override fun run(result: Result<Unit>) {
                val jdk = JavaSdk.getInstance().createJdk("myJDK", "my/path/to/jdk")
                getProjectJdkTableSafe().addJdk(jdk)
            }
        }.execute()

        try {
            createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

            importProject(
                """
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
                                <jdkHome>my/path/to/jdk</jdkHome>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
                """
            )

            assertModules("project")
            assertImporterStatePresent()

            val moduleSDK = ModuleRootManager.getInstance(getModule("project")).sdk!!
            Assert.assertTrue(moduleSDK.sdkType is JavaSdk)
            Assert.assertEquals("myJDK", moduleSDK.name)
            Assert.assertEquals("my/path/to/jdk", moduleSDK.homePath)
        } finally {
            object : WriteAction<Unit>() {
                override fun run(result: Result<Unit>) {
                    val jdkTable = getProjectJdkTableSafe()
                    jdkTable.removeJdk(jdkTable.findJdk("myJDK")!!)
                }
            }.execute()
        }
    }

    fun testProductionOnTestDependency() {
        createProjectSubDirs(
            "module-with-java/src/main/java",
            "module-with-java/src/test/java",
            "module-with-kotlin/src/main/kotlin",
            "module-with-kotlin/src/test/kotlin"
        )

        val dummyFile = createProjectSubFile(
            "module-with-kotlin/src/main/kotlin/foo/dummy.kt",
            """
                    package foo

                    fun dummy() {
                    }

                """.trimIndent()
        )

        val pomA = createModulePom(
            "module-with-java",
            """
                <parent>
                    <groupId>test-group</groupId>
                    <artifactId>mvnktest</artifactId>
                    <version>0.0.0.0-SNAPSHOT</version>
                </parent>

                <artifactId>module-with-java</artifactId>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-jar-plugin</artifactId>
                            <version>2.6</version>
                            <executions>
                                <execution>
                                    <goals>
                                        <goal>test-jar</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
                """.trimIndent()
        )

        val pomB = createModulePom(
            "module-with-kotlin",
            """
                <parent>
                    <groupId>test-group</groupId>
                    <artifactId>mvnktest</artifactId>
                    <version>0.0.0.0-SNAPSHOT</version>
                </parent>

                <artifactId>module-with-kotlin</artifactId>

                <properties>
                    <kotlin.version>1.1.4</kotlin.version>
                    <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget>
                    <kotlin.compiler.incremental>true</kotlin.compiler.incremental>
                </properties>

                <dependencies>

                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-stdlib</artifactId>
                        <version>${"$"}{kotlin.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-runtime</artifactId>
                        <version>${"$"}{kotlin.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-reflect</artifactId>
                        <version>${"$"}{kotlin.version}</version>
                    </dependency>

                    <dependency>
                        <groupId>test-group</groupId>
                        <artifactId>module-with-java</artifactId>
                    </dependency>

                    <dependency>
                        <groupId>test-group</groupId>
                        <artifactId>module-with-java</artifactId>
                        <type>test-jar</type>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <artifactId>kotlin-maven-plugin</artifactId>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <version>${"$"}{kotlin.version}</version>
                            <executions>
                                <execution>
                                    <id>compile</id>
                                    <goals> <goal>compile</goal> </goals>
                                    <configuration>
                                        <sourceDirs>
                                            <sourceDir>${"$"}{project.basedir}/src/main/kotlin</sourceDir>
                                            <sourceDir>${"$"}{project.basedir}/src/main/java</sourceDir>
                                        </sourceDirs>
                                    </configuration>
                                </execution>
                                <execution>
                                    <id>test-compile</id>
                                    <goals> <goal>test-compile</goal> </goals>
                                    <configuration>
                                        <sourceDirs>
                                            <sourceDir>${"$"}{project.basedir}/src/test/kotlin</sourceDir>
                                            <sourceDir>${"$"}{project.basedir}/src/test/java</sourceDir>
                                        </sourceDirs>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.5.1</version>
                            <executions>
                                <!-- Replacing default-compile as it is treated specially by maven -->
                                <execution>
                                    <id>default-compile</id>
                                    <phase>none</phase>
                                </execution>
                                <!-- Replacing default-testCompile as it is treated specially by maven -->
                                <execution>
                                    <id>default-testCompile</id>
                                    <phase>none</phase>
                                </execution>
                                <execution>
                                    <id>java-compile</id>
                                    <phase>compile</phase>
                                    <goals> <goal>compile</goal> </goals>
                                </execution>
                                <execution>
                                    <id>java-test-compile</id>
                                    <phase>test-compile</phase>
                                    <goals> <goal>testCompile</goal> </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
                """.trimIndent()
        )

        val pomMain = createModulePom(
            "",
            """
                <groupId>test-group</groupId>
                <artifactId>mvnktest</artifactId>
                <version>0.0.0.0-SNAPSHOT</version>

                <packaging>pom</packaging>

                <properties>
                    <kotlin.version>1.1.4</kotlin.version>
                    <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget>
                    <kotlin.compiler.incremental>true</kotlin.compiler.incremental>
                </properties>

                <modules>
                    <module>module-with-java</module>
                    <module>module-with-kotlin</module>
                </modules>

                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>test-group</groupId>
                            <artifactId>module-with-kotlin</artifactId>
                            <version>${"$"}{project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>test-group</groupId>
                            <artifactId>module-with-java</artifactId>
                            <version>${"$"}{project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>test-group</groupId>
                            <artifactId>module-with-java</artifactId>
                            <version>${"$"}{project.version}</version>
                            <type>test-jar</type>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """.trimIndent()
        )

        importProjects(pomMain, pomA, pomB)

        assertModules("module-with-kotlin", "module-with-java", "mvnktest")

        val dependencies = (dummyFile.toPsiFile(myProject) as KtFile).analyzeAndGetResult().moduleDescriptor.allDependencyModules
        TestCase.assertTrue(dependencies.any { it.name.asString() == "<production sources for module module-with-java>" })
        TestCase.assertTrue(dependencies.any { it.name.asString() == "<test sources for module module-with-java>" })
    }

    fun testNoArgDuplication() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
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
                                <arg>-Xjsr305=strict</arg>
                            </args>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertModules("project")
        assertImporterStatePresent()

        with(facetSettings) {
            Assert.assertEquals("-Xjsr305=strict", compilerSettings!!.additionalArguments)
        }
    }

    fun testInternalArgumentsFacetImporting() {
        importProject(
            """
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
                            <languageVersion>1.2</languageVersion>
                            <args>
                                <arg>-XXLanguage:+InlineClasses</arg>
                            </args>
                            <jvmTarget>1.8</jvmTarget>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertImporterStatePresent()

        // Check that we haven't lost internal argument during importing to facet
        Assert.assertEquals("-XXLanguage:+InlineClasses", facetSettings.compilerSettings?.additionalArguments)

        // Check that internal argument influenced LanguageVersionSettings correctly
        Assert.assertEquals(
            LanguageFeature.State.ENABLED,
            getModule("project").languageVersionSettings.getFeatureSupport(LanguageFeature.InlineClasses)
        )
    }

    fun testStableModuleNameWhileUsingMaven_JVM() {
        createProjectSubDirs("src/main/kotlin")

        importProject(
            """
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
                            <languageVersion>1.2</languageVersion>
                            <jvmTarget>1.8</jvmTarget>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """
        )

        assertImporterStatePresent()

        checkStableModuleName("project", "project", JvmPlatforms.unspecifiedJvmPlatform, isProduction = true)
        checkStableModuleName("project", "project", JvmPlatforms.unspecifiedJvmPlatform, isProduction = false)
    }

    fun testStableModuleNameWhileUsngMaven_JS() {
        createProjectSubDirs("src/main/kotlin", "src/main/kotlin.jvm", "src/test/kotlin", "src/test/kotlin.jvm")

        importProject(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-stdlib-js</artifactId>
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
            """
        )

        assertImporterStatePresent()

        // Note that we check name induced by '-output-file' -- may be it's not the best
        // decision, but we don't have a better one
        checkStableModuleName("project", "test", JsPlatforms.defaultJsPlatform, isProduction = true)
        checkStableModuleName("project", "test", JsPlatforms.defaultJsPlatform, isProduction = false)
    }

    private fun checkStableModuleName(projectName: String, expectedName: String, platform: TargetPlatform, isProduction: Boolean) {
        val module = getModule(projectName)
        val moduleInfo = if (isProduction) module.productionSourceInfo() else module.testSourceInfo()

        val resolutionFacade = KotlinCacheService.getInstance(myProject).getResolutionFacadeByModuleInfo(moduleInfo!!, platform)!!
        val moduleDescriptor = resolutionFacade.moduleDescriptor

        Assert.assertEquals("<$expectedName>", moduleDescriptor.stableName?.asString())
    }

    private fun assertImporterStatePresent() {
        assertNotNull("Kotlin importer component is not present", myTestFixture.module.kotlinImporterComponent)
    }

    private fun facetSettings(moduleName: String) = KotlinFacet.get(getModule(moduleName))!!.configuration.settings

    private val facetSettings: KotlinFacetSettings
        get() = facetSettings("project")
}