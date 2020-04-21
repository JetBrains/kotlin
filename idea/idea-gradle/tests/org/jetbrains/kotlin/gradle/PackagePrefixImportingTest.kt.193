/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.ModulesOrderEnumerator
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.junit.Test

class PackagePrefixImportingTest : MultiplePluginVersionGradleImportingTestCase() {

    @Test
    fun testPackagePrefixNonMPP() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            project,
            projectPath,
            exhaustiveModuleList = true,
            exhaustiveSourceSourceRootList = true,
            exhaustiveDependencyList = false,
            exhaustiveTestsList = false
        ) {
            module("project") {
            }
            module("project_main") {
                sourceFolder("src/main/java", JavaSourceRootType.SOURCE, "package.prefix.main")
                sourceFolder("src/main/kotlin", JavaSourceRootType.SOURCE, "package.prefix.main")
                sourceFolder("src/main/resources", JavaResourceRootType.RESOURCE)
            }
            module("project_test") {
                sourceFolder("src/test/java", JavaSourceRootType.TEST_SOURCE, "package.prefix.test")
                sourceFolder("src/test/kotlin", JavaSourceRootType.TEST_SOURCE, "package.prefix.test")
                sourceFolder("src/test/resources", JavaResourceRootType.TEST_RESOURCE)
            }
        }
    }

    override fun importProject() {
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject()
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        }
    }

    override fun testDataDirName(): String {
        return "packagePrefixImport"
    }
}