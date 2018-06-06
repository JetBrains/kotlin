/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Ignore
import org.junit.Test

class GradleConfiguratorTest : GradleImportingTestCase() {

    @Test
    fun testProjectWithModule() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile(
                "app/build.gradle", """
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
            compile "org.jetbrains.kotlin:kotlin-stdlib-0.0"   // intentionally invalid version
        }
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                // Create not configured build.gradle for project
                myProject.baseDir.createChildData(null, "build.gradle")

                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val moduleGroup = module.toModuleGroup()
                // We have a Kotlin runtime in build.gradle but not in the classpath, so it doesn't make sense
                // to suggest configuring it
                assertEquals(ConfigureKotlinStatus.BROKEN, findGradleModuleConfigurator().getStatus(moduleGroup))
                // Don't offer the JS configurator if the JVM configuration exists but is broken
                assertEquals(ConfigureKotlinStatus.BROKEN, findJsGradleModuleConfigurator().getStatus(moduleGroup))
            }
        }
    }

    @Test
    fun testConfigure10() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile(
                "app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.0.6", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals(
                        """
                buildscript {
                    ext.kotlin_version = '1.0.6'
                    repositories {
                        jcenter()
                        mavenCentral()
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlin_version"
                    }
                }
                apply plugin: 'kotlin'
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib:${"$"}kotlin_version"
                }
                """.trimIndent(), content
                )
            }
        }
    }

    @Test
    fun testConfigureKotlinWithPluginsBlock() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile(
                "app/build.gradle", """
        plugins {
            id 'java'
        }

        group 'testgroup'
        version '1.0-SNAPSHOT'

        sourceCompatibility = 1.8

        repositories {
            mavenCentral()
        }

        dependencies {
            testCompile group: 'junit', name: 'junit', version: '4.12'
        }
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.0.6", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals(
                        """
                buildscript {
                    ext.kotlin_version = '1.0.6'
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlin_version"
                    }
                }
                plugins {
                    id 'java'
                }
                apply plugin: 'kotlin'

                group 'testgroup'
                version '1.0-SNAPSHOT'

                sourceCompatibility = 1.8

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testCompile group: 'junit', name: 'junit', version: '4.12'
                    compile "org.jetbrains.kotlin:kotlin-stdlib:${"$"}kotlin_version"
                }
                """.trimIndent(), content
                )
            }
        }
    }

    @Test
    fun testConfigureKotlinDevVersion() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile(
                "app/build.gradle", """
        group 'testgroup'
        version '1.0-SNAPSHOT'
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.60-dev-286", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals(
                        """
                group 'testgroup'
                version '1.0-SNAPSHOT'
                buildscript {
                    ext.kotlin_version = '1.2.60-dev-286'
                    repositories {
                        maven {
                            url 'https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:1.2.60-dev-286,branch:default:any/artifacts/content/maven/'
                        }
                        mavenCentral()
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlin_version"
                    }
                }
                apply plugin: 'kotlin'
                repositories {
                    maven {
                        url 'https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:1.2.60-dev-286,branch:default:any/artifacts/content/maven/'
                    }
                    mavenCentral()
                }
                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${"$"}kotlin_version"
                }
                compileKotlin {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
                compileTestKotlin {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
                """.trimIndent(), content
                )
            }
        }
    }

    @Test
    fun testConfigureGradleKtsKotlinDevVersion() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile(
                "app/build.gradle.kts", """
        group = "testgroup"
        version = "1.0-SNAPSHOT"
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.60-dev-286", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals(
                        """
                import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

                val kotlin_version: String by extra
                buildscript {
                    var kotlin_version: String by extra
                    kotlin_version = "1.2.60-dev-286"
                    repositories {
                        maven {
                            setUrl("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:1.2.60-dev-286,branch:default:any/artifacts/content/maven/")
                        }
                        mavenCentral()
                    }
                    dependencies {
                        classpath(kotlinModule("gradle-plugin", kotlin_version))
                    }
                }
                group = "testgroup"
                version = "1.0-SNAPSHOT"
                apply {
                    plugin("kotlin")
                }
                dependencies {
                    compile(kotlinModule("stdlib-jdk8", kotlin_version))
                }
                repositories {
                    maven {
                        setUrl("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:1.2.60-dev-286,branch:default:any/artifacts/content/maven/")
                    }
                    mavenCentral()
                }
                val compileKotlin: KotlinCompile by tasks
                compileKotlin.kotlinOptions {
                    jvmTarget = "1.8"
                }
                val compileTestKotlin: KotlinCompile by tasks
                compileTestKotlin.kotlinOptions {
                    jvmTarget = "1.8"
                }
                """.trimIndent(), content
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJvmWithBuildGradle() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle",
                """
                    plugins {
                        id 'java'
                    }

                    group 'testgroup'
                    version '1.0-SNAPSHOT'

                    sourceCompatibility = 1.8

                    repositories {
                        mavenCentral()
                    }

                    dependencies {
                        testCompile group: 'junit', name: 'junit', version: '4.12'
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id 'java'
                                id 'org.jetbrains.kotlin.jvm' version '1.2.40'
                            }

                            group 'testgroup'
                            version '1.0-SNAPSHOT'

                            sourceCompatibility = 1.8

                            repositories {
                                mavenCentral()
                            }

                            dependencies {
                                testCompile group: 'junit', name: 'junit', version: '4.12'
                                compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
                            }
                            compileKotlin {
                                kotlinOptions {
                                    jvmTarget = "1.8"
                                }
                            }
                            compileTestKotlin {
                                kotlinOptions {
                                    jvmTarget = "1.8"
                                }
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJvmWithBuildGradleKts() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle.kts",
                """
                    plugins {
                        java
                    }

                    group = "testgroup"
                    version = "1.0-SNAPSHOT"

                    repositories {
                        mavenCentral()
                    }

                    dependencies {
                        testCompile("junit", "junit", "4.12")
                    }

                    configure<JavaPluginConvention> {
                        sourceCompatibility = JavaVersion.VERSION_1_8
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

                            plugins {
                                java
                                kotlin("jvm") version "1.2.40"
                            }

                            group = "testgroup"
                            version = "1.0-SNAPSHOT"

                            repositories {
                                mavenCentral()
                            }

                            dependencies {
                                testCompile("junit", "junit", "4.12")
                                compile(kotlin("stdlib-jdk8"))
                            }

                            configure<JavaPluginConvention> {
                                sourceCompatibility = JavaVersion.VERSION_1_8
                            }
                            val compileKotlin: KotlinCompile by tasks
                            compileKotlin.kotlinOptions {
                                jvmTarget = "1.8"
                            }
                            val compileTestKotlin: KotlinCompile by tasks
                            compileTestKotlin.kotlinOptions {
                                jvmTarget = "1.8"
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJvmEAPWithBuildGradle() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle",
                """
                    plugins {
                        id 'java'
                    }

                    group 'testgroup'
                    version '1.0-SNAPSHOT'

                    sourceCompatibility = 1.8

                    repositories {
                        mavenCentral()
                    }

                    dependencies {
                        testCompile group: 'junit', name: 'junit', version: '4.12'
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40-eap-62", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id 'java'
                                id 'org.jetbrains.kotlin.jvm' version '1.2.40-eap-62'
                            }

                            group 'testgroup'
                            version '1.0-SNAPSHOT'

                            sourceCompatibility = 1.8

                            repositories {
                                mavenCentral()
                                maven {
                                    url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                }
                            }

                            dependencies {
                                testCompile group: 'junit', name: 'junit', version: '4.12'
                                compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
                            }
                            compileKotlin {
                                kotlinOptions {
                                    jvmTarget = "1.8"
                                }
                            }
                            compileTestKotlin {
                                kotlinOptions {
                                    jvmTarget = "1.8"
                                }
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                repositories {
                                    maven {
                                        url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                    }
                                    mavenCentral()
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJvmEAPWithBuildGradleKts() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle.kts",
                """
                    plugins {
                        java
                    }

                    group = "testgroup"
                    version = "1.0-SNAPSHOT"

                    repositories {
                        mavenCentral()
                    }

                    dependencies {
                        testCompile("junit", "junit", "4.12")
                    }

                    configure<JavaPluginConvention> {
                        sourceCompatibility = JavaVersion.VERSION_1_8
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40-eap-62", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

                            plugins {
                                java
                                kotlin("jvm") version "1.2.40-eap-62"
                            }

                            group = "testgroup"
                            version = "1.0-SNAPSHOT"

                            repositories {
                                mavenCentral()
                                maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
                            }

                            dependencies {
                                testCompile("junit", "junit", "4.12")
                                compile(kotlin("stdlib-jdk8"))
                            }

                            configure<JavaPluginConvention> {
                                sourceCompatibility = JavaVersion.VERSION_1_8
                            }
                            val compileKotlin: KotlinCompile by tasks
                            compileKotlin.kotlinOptions {
                                jvmTarget = "1.8"
                            }
                            val compileTestKotlin: KotlinCompile by tasks
                            compileTestKotlin.kotlinOptions {
                                jvmTarget = "1.8"
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                repositories {
                                    maven {
                                        url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                    }
                                    mavenCentral()
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJsWithBuildGradle() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle",
                """
                    group 'testgroup'
                    version '1.0-SNAPSHOT'

                    repositories {
                        mavenCentral()
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findJsGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id 'kotlin2js' version '1.2.40'
                            }
                            group 'testgroup'
                            version '1.0-SNAPSHOT'

                            repositories {
                                mavenCentral()
                            }
                            dependencies {
                                compile "org.jetbrains.kotlin:kotlin-stdlib-js"
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                resolutionStrategy {
                                    eachPlugin {
                                        if (requested.id.id == "kotlin2js") {
                                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                                        }
                                    }
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJsWithBuildGradleKts() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle.kts",
                """
                    group = "testgroup"
                    version = "1.0-SNAPSHOT"

                    repositories {
                        mavenCentral()
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findJsGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id("kotlin2js") version "1.2.40"
                            }
                            group = "testgroup"
                            version = "1.0-SNAPSHOT"

                            repositories {
                                mavenCentral()
                            }
                            dependencies {
                                compile(kotlin("stdlib-js"))
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                resolutionStrategy {
                                    eachPlugin {
                                        if (requested.id.id == "kotlin2js") {
                                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                                        }
                                    }
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJsEAPWithBuildGradle() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle",
                """
                    group 'testgroup'
                    version '1.0-SNAPSHOT'

                    repositories {
                        mavenCentral()
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findJsGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40-eap-62", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id 'kotlin2js' version '1.2.40-eap-62'
                            }
                            group 'testgroup'
                            version '1.0-SNAPSHOT'

                            repositories {
                                mavenCentral()
                                maven {
                                    url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                }
                            }
                            dependencies {
                                compile "org.jetbrains.kotlin:kotlin-stdlib-js"
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                resolutionStrategy {
                                    eachPlugin {
                                        if (requested.id.id == "kotlin2js") {
                                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                                        }
                                    }
                                }
                                repositories {
                                    maven {
                                        url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                    }
                                    mavenCentral()
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    @Test
    @Ignore // Enable in Gradle 4.4+
    fun testConfigureJsEAPWithBuildGradleKts() {
        val settingsFile = createProjectSubFile("settings.gradle", "include ':app'")
        val buildFile = createProjectSubFile(
                "app/build.gradle.kts",
                """
                    group = "testgroup"
                    version = "1.0-SNAPSHOT"

                    repositories {
                        mavenCentral()
                    }
                """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findJsGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.2.40-eap-62", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                assertFileContent(
                        buildFile,
                        """
                            plugins {
                                id("kotlin2js") version "1.2.40-eap-62"
                            }
                            group = "testgroup"
                            version = "1.0-SNAPSHOT"

                            repositories {
                                mavenCentral()
                                maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
                            }
                            dependencies {
                                compile(kotlin("stdlib-js"))
                            }
                        """.trimIndent()
                )
                assertFileContent(
                        settingsFile,
                        """
                            pluginManagement {
                                resolutionStrategy {
                                    eachPlugin {
                                        if (requested.id.id == "kotlin2js") {
                                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                                        }
                                    }
                                }
                                repositories {
                                    maven {
                                        url 'http://dl.bintray.com/kotlin/kotlin-eap'
                                    }
                                    mavenCentral()
                                }
                            }
                            include ':app'
                        """.trimIndent()
                )
            }
        }
    }

    private fun assertFileContent(file: VirtualFile, expected: String) {
        assertEquals(expected, LoadTextUtil.loadText(file).toString())
    }

    private fun findGradleModuleConfigurator() = Extensions.findExtension(
            KotlinProjectConfigurator.EP_NAME,
            KotlinGradleModuleConfigurator::class.java
    )

    private fun findJsGradleModuleConfigurator() = Extensions.findExtension(
            KotlinProjectConfigurator.EP_NAME,
            KotlinJsGradleModuleConfigurator::class.java
    )

    @Test
    fun testConfigureGSK() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile(
                "app/build.gradle.kts", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.1.2", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals(
                        """
                    import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

                    val kotlin_version: String by extra
                    buildscript {
                        var kotlin_version: String by extra
                        kotlin_version = "1.1.2"
                        repositories {
                            jcenter()
                            mavenCentral()
                        }
                        dependencies {
                            classpath(kotlinModule("gradle-plugin", kotlin_version))
                        }
                    }
                    apply {
                        plugin("kotlin")
                    }
                    dependencies {
                        compile(kotlinModule("stdlib-jre8", kotlin_version))
                    }
                    repositories {
                        mavenCentral()
                    }
                    val compileKotlin: KotlinCompile by tasks
                    compileKotlin.kotlinOptions {
                        jvmTarget = "1.8"
                    }
                    val compileTestKotlin: KotlinCompile by tasks
                    compileTestKotlin.kotlinOptions {
                        jvmTarget = "1.8"
                    }
                """.trimIndent(), content
                )
            }
        }
    }

    @Test
    fun testListNonConfiguredModules() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile(
                "app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'
        """.trimIndent()
        )
        createProjectSubFile("app/src/main/java/foo.kt", "")

        importProject()

        runReadAction {
            val configurator = findGradleModuleConfigurator()

            val (modules, ableToRunConfigurators) = getConfigurationPossibilitiesForConfigureNotification(myProject)
            assertTrue(ableToRunConfigurators.any { it is KotlinGradleModuleConfigurator })
            assertTrue(ableToRunConfigurators.any { it is KotlinJsGradleModuleConfigurator })
            val moduleNames = modules.map { it.baseModule.name }
            assertSameElements(moduleNames, "app")

            val moduleNamesFromConfigurator = getCanBeConfiguredModules(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesFromConfigurator, "app")

            val moduleNamesWithKotlinFiles = getCanBeConfiguredModulesWithKotlinFiles(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesWithKotlinFiles, "app")
        }
    }

    @Test
    fun testListNonConfiguredModules_Configured() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile(
                "app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'

        repositories {
            jcenter()
            mavenCentral()
        }

        dependencies {
            compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.3"
        }
        """.trimIndent()
        )
        createProjectSubFile("app/src/main/java/foo.kt", "")

        importProject()

        runReadAction {
            assertEmpty(getConfigurationPossibilitiesForConfigureNotification(myProject).first)
        }
    }

    @Test
    fun testListNonConfiguredModules_ConfiguredWithImplementation() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile(
                "app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'

        repositories {
            jcenter()
            mavenCentral()
        }

        dependencies {
            implementation "org.jetbrains.kotlin:kotlin-stdlib:1.1.3"
        }
        """.trimIndent()
        )
        createProjectSubFile("app/src/main/java/foo.kt", "")

        importProject()

        runReadAction {
            assertEmpty(getConfigurationPossibilitiesForConfigureNotification(myProject).first)
        }
    }

    @Test
    fun testListNonConfiguredModules_ConfiguredOnlyTest() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile(
                "app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'

        repositories {
            jcenter()
            mavenCentral()
        }

        dependencies {
            testCompile "org.jetbrains.kotlin:kotlin-stdlib:1.1.3"
        }
        """.trimIndent()
        )
        createProjectSubFile("app/src/test/java/foo.kt", "")

        importProject()

        runReadAction {
            assertEmpty(getConfigurationPossibilitiesForConfigureNotification(myProject).first)
        }
    }

    @Test
    fun testAddNonKotlinLibraryGSK() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object : ExternalLibraryDescriptor("org.a.b", "lib", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    compile("org.a.b:lib:1.0.0")
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddLibraryGSK_WithKotlinVersion() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """
                                                     |val kotlin_version: String by extra
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8", kotlin_version))
                                                     |}
                                                     |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                val stdLibVersion = KotlinWithGradleConfigurator.getKotlinStdlibVersion(myTestFixture.module)
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", stdLibVersion, stdLibVersion) {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |val kotlin_version: String by extra
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8", kotlin_version))
                 |    compile(kotlinModule("reflect", kotlin_version))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddTestLibraryGSK() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """
                                                     |dependencies {
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.TEST,
                        object : ExternalLibraryDescriptor("junit", "junit", "4.12", "4.12") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })

                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.TEST,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test", "1.1.2", "1.1.2") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    testCompile("junit:junit:4.12")
                 |    testCompile(kotlinModule("test", "1.1.2"))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddLibraryGSK() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    compile(kotlinModule("reflect", "1.0.0"))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddCoroutinesSupport() {
        val buildScript = createProjectSubFile(
                "build.gradle", """
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
            compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
        }
        """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
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
                    compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
                }
                kotlin {
                    experimental {
                        coroutines "enable"
                    }
                }
                """.trimIndent(),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddCoroutinesSupportGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts", "")

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.ENABLE
                  |}""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testChangeCoroutinesSupport() {
        val buildScript = createProjectSubFile(
                "build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            kotlin {
                experimental {
                      coroutines "error"
                }
            }
            """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            kotlin {
                experimental {
                      coroutines "enable"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testChangeCoroutinesSupportGSK() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.DISABLE
                  |}
                  |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.ENABLE
                  |}
                  |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddLanguageVersion() {
        val buildScript = createProjectSubFile(
                "build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.1"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddLanguageVersionGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts", "")

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                 |
                 |val compileKotlin: KotlinCompile by tasks
                 |compileKotlin.kotlinOptions {
                 |    languageVersion = "1.1"
                 |}""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testChangeLanguageVersion() {
        val buildScript = createProjectSubFile(
                "build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.0"
                }
            }
            """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.1"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testChangeLanguageVersionGSK() {
        val buildScript = createProjectSubFile(
                "build.gradle.kts",
                """val compileKotlin: KotlinCompile by tasks
                                                     |compileKotlin.kotlinOptions {
                                                     |   languageVersion = "1.0"
                                                     |}
                                                     |""".trimMargin("|")
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """val compileKotlin: KotlinCompile by tasks
                 |compileKotlin.kotlinOptions {
                 |    languageVersion = "1.1"
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString()
        )
    }

    @Test
    fun testAddLibrary() {
        val buildScript = createProjectSubFile(
                "build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            """.trimIndent()
        )

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
                compile "org.jetbrains.kotlin:kotlin-reflect:1.0.0"
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString()
        )
    }
}
