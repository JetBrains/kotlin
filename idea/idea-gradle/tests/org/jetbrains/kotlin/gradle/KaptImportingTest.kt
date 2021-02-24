/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.legacyMppImportTestMinVersionForMaster
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.Test

class KaptImportingTest : MultiplePluginVersionGradleImportingTestCase() {

    fun importProject(modulePerSourceSet: Boolean) {
        currentExternalProjectSettings.isResolveModulePerSourceSet = modulePerSourceSet
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject(true)
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testModulePerSourceSet() {
        // Disable testing import module per source set test in Android Studio as this mode is not supported in Android Studio
        if (isAndroidStudio()) {
            return
        }

        configureByFiles()
        importProject(true)

        checkProjectStructure(myProject, projectPath, true, true, true, false) {
            module("project")
            module("project_main") {
                sourceFolder("build/generated/source/kapt/main", JavaSourceRootType.SOURCE)
                sourceFolder("build/generated/source/kaptKotlin/main", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/resources", JavaResourceRootType.RESOURCE)
                libraryDependency("Gradle: kaptGeneratedClasses", DependencyScope.COMPILE)
            }
            module("project_test") {
                sourceFolder("build/generated/source/kapt/test", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("build/generated/source/kaptKotlin/test", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                moduleDependency("project_main", DependencyScope.COMPILE)
                libraryDependency("Gradle: kaptGeneratedClasses", DependencyScope.COMPILE)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testModulePerSourceSetDisabled() {
        configureByFiles()
        importProject(false)

        checkProjectStructure(myProject, projectPath, true, true, true, false) {
            module("project") {
                sourceFolder("build/generated/source/kapt/main", JavaSourceRootType.SOURCE)
                sourceFolder("build/generated/source/kaptKotlin/main", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("src/main/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("build/generated/source/kapt/test", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("build/generated/source/kaptKotlin/test", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                libraryDependency("Gradle: kaptGeneratedClasses", DependencyScope.COMPILE)
            }

        }
    }

    override fun testDataDirName(): String {
        return "kaptImportingTest"
    }

}