/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Fails a generated managed test outside the packages its module declares in `testDataManager { testPackages }`: the test
 * data manager discovers the managed tests only there, so it would skip this one without a word. It checks the tests run
 * with the packages, as the `test-data-manager` plugin passes them to the `test` task of the module.
 */
internal class ManagedTestPackageCheck : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        val packages = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES) ?: return
        // The class JUnit discovered, which a nested class, even one inherited from another class, runs in
        val discovered = generateSequence(context) { it.parent.orElse(null) }.mapNotNull { it.testClass.orElse(null) }.last()
        testPackageError(discovered.name, TestDataManagerRunner.parseTestPackages(packages))?.let(::error)
    }

    companion object {
        /**
         * Why the test data manager doesn't discover the class named [className], one JUnit discovers, in [packages], or `null`
         * if it does or doesn't look for it: it looks for the classes whose name ends with
         * [TestDataManagerRunner.GENERATED_TEST_CLASS_SUFFIX], like `FooTestGenerated` or `Holder${'$'}FooTestGenerated`.
         */
        fun testPackageError(className: String, packages: List<String>): String? {
            if (!className.endsWith(TestDataManagerRunner.GENERATED_TEST_CLASS_SUFFIX)) return null
            val packageName = className.substringBeforeLast('.', missingDelimiterValue = "")
            if (packageName.isEmpty()) {
                return "$className is a managed test in the default package, where the test data manager doesn't look for tests: " +
                        "move it into a package of `testDataManager { testPackages }` of its module (${packages.joinToString()})"
            }

            if (packages.any { packageName == it || packageName.startsWith("$it.") }) return null
            return "$className is a managed test outside the packages of `testDataManager { testPackages }` of its module " +
                    "(${packages.joinToString()}), so the test data manager doesn't find it: add its package there"
        }
    }
}
