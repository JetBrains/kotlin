/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

abstract class AbstractGradleFacetImportTest : GradleImportingTestCase() {
    private var isCreateEmptyContentRootDirectories = true

    override fun setUp() {
        super.setUp()
        isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
    }

    override fun tearDown() {
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        super.tearDown()
    }

    open fun doTest(projectDirPath: String) {
        loadProject(projectDirPath)


    }
}