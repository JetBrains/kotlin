/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class HierarchicalMultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {
    //TODO support testing multiple gradle plugins versions

    override fun isApplicableTest(): Boolean {
        return !gradleVersion.startsWith("3.")
    }

    @Before
    fun saveSdksBeforeTest() {
        val kotlinSdks = sdkCreationChecker?.getKotlinSdks() ?: emptyList()
        if (kotlinSdks.isNotEmpty()) {
            fail("Found Kotlin SDK before importing test. Sdk list: $kotlinSdks")
        }
    }

    @After
    fun checkSdkCreated() {
        if (sdkCreationChecker?.isKotlinSdkCreated() == false) {
            fail("Kotlin SDK was not created during import of HMPP Project.")
        }
    }

    @Test
    fun testImportHMPPFlag() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            allModules {
                isHMPP(true)
            }
            module("project.my-app.commonMain")
            module("project.my-app.jvmAndJsMain")
        }
    }

    private fun checkProjectStructure(
        exhaustiveModuleList: Boolean = true,
        exhaustiveSourceSourceRootList: Boolean = true,
        exhaustiveDependencyList: Boolean = true,
        body: ProjectInfo.() -> Unit = {}
    ) {
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList,
            exhaustiveSourceSourceRootList,
            exhaustiveDependencyList,
            body)
    }

    override fun importProject() {
        val isUseQualifiedModuleNames = currentExternalProjectSettings.isUseQualifiedModuleNames
        currentExternalProjectSettings.isUseQualifiedModuleNames = true
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject()
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
            currentExternalProjectSettings.isUseQualifiedModuleNames = isUseQualifiedModuleNames
        }
    }

    override fun testDataDirName(): String {
        return "hierarchicalMultiplatformImport"
    }
}