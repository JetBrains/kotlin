/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind

class MultiModuleHighlightingTest : AbstractMultiModuleHighlightingTest() {
    fun testVisibility() {
        val module1 = module("m1")
        val module2 = module("m2")

        module2.addDependency(module1)

        checkHighlightingInAllFiles()
    }

    fun testDependency() {
        val module1 = module("m1")
        val module2 = module("m2")
        val module3 = module("m3")
        val module4 = module("m4")

        module2.addDependency(module1)

        module1.addDependency(module2)

        module3.addDependency(module2)

        module4.addDependency(module1)
        module4.addDependency(module2)
        module4.addDependency(module3)

        checkHighlightingInAllFiles()
    }

    fun testTestRoot() {
        val module1 = module("m1", hasTestRoot = true)
        val module2 = module("m2", hasTestRoot = true)
        val module3 = module("m3", hasTestRoot = true)

        module3.addDependency(module1, dependencyScope = DependencyScope.TEST)
        module3.addDependency(module2, dependencyScope = DependencyScope.TEST)
        module2.addDependency(module1, dependencyScope = DependencyScope.COMPILE)

        checkHighlightingInAllFiles()
    }

    fun testPlatformBasic() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        checkHighlightingInAllFiles()
    }

    fun testPlatformClass() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        checkHighlightingInAllFiles()
    }

    fun testPlatformNotImplementedForBoth() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        val js = module("js")
        js.setPlatformKind(TargetPlatformKind.JavaScript)
        js.enableMultiPlatform()
        js.addDependency(header)

        checkHighlightingInAllFiles()
    }

    fun testPlatformPartiallyImplemented() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        checkHighlightingInAllFiles()
    }

    fun testPlatformFunctionProperty() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        checkHighlightingInAllFiles()
    }

    fun testPlatformSuppress() {
        val header = module("header")
        header.setPlatformKind(TargetPlatformKind.Common)

        val jvm = module("jvm")
        jvm.setPlatformKind(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        checkHighlightingInAllFiles()
    }
}
