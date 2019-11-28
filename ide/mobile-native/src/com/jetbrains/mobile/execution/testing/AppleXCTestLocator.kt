/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution.testing

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.cidr.execution.testing.xctest.OCUnitTestLocator
import com.jetbrains.cidr.execution.testing.xctest.OCUnitTestObject

class AppleXCTestLocator : OCUnitTestLocator() {
    override fun collectTestObjects(
        pathToFind: String,
        project: Project,
        scope: GlobalSearchScope?
    ): Collection<OCUnitTestObject> =
        AppleXCTestFramework.instance.collectTestObjects(pathToFind, project, scope)

    override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
        AppleXCTestFramework.instance.getUpdater(project)
}