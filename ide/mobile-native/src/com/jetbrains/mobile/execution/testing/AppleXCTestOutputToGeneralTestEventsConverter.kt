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