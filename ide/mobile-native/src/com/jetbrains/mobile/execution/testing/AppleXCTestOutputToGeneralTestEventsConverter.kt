/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution.testing

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.testing.xctest.OCUnitConsoleProperties
import com.jetbrains.cidr.execution.testing.xctest.OCUnitOutputToGeneralTestEventsConverter
import com.jetbrains.cidr.execution.testing.xctest.OCUnitTestObject

class AppleXCTestOutputToGeneralTestEventsConverter(
    properties: OCUnitConsoleProperties,
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties
) : OCUnitOutputToGeneralTestEventsConverter(properties, testFrameworkName, consoleProperties) {

    override fun findTestObject(pathToFind: String, project: Project): OCUnitTestObject? =
        AppleXCTestFramework.instance.findTestObject(pathToFind, project, null)
}