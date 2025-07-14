/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_TESTDATA_ROOTS

class SystemPropertyTestDataRootConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    private val testDataRoots = System.getProperty(KOTLIN_TESTDATA_ROOTS)?.let { property ->
        buildMap {
            val roots = property.split(";")
            for (root in roots) {
                val (from, to) = root.trim().split("=")
                put(from, to)
            }
        }
    }

    override fun transformTestDataPath(testDataFileName: String): String {
        if (testDataRoots != null) {
            for ((from, to) in testDataRoots) {
                if (testDataFileName.startsWith(from)) return to + testDataFileName.removePrefix(from)
            }
        }
        return testDataFileName
    }
}
