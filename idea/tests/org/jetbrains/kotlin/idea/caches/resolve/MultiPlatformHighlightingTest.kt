/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.TestJdkKind

// Some tests are ignored below. They fail because <error> markers are not stripped correctly in multi-module highlighting tests.
// TODO: fix this in the test framework and unignore the tests
class MultiPlatformHighlightingTest : AbstractMultiModuleHighlightingTest() {
    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleHighlighting/multiplatform/"

    fun testBasic() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testDepends() {
        val commonModule = module("common", TestJdkKind.MOCK_JDK)
        commonModule.createFacet(TargetPlatformKind.Common)
        commonModule.enableMultiPlatform()

        val jvmPlatform = TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
        val jvmModule = module("jvm", TestJdkKind.MOCK_JDK)
        jvmModule.createFacet(jvmPlatform)
        jvmModule.enableMultiPlatform()
        jvmModule.addDependency(commonModule)

        checkHighlightingInAllFiles()
    }

    fun testHeaderClass() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testHeaderWithoutImplForBoth() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], TargetPlatformKind.JavaScript)
    }

    fun ignore_testHeaderPartiallyImplemented() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testHeaderFunctionProperty() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testInternal() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testSuppressHeaderWithoutImpl() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testCatchHeaderExceptionInPlatformModule() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], withStdlibCommon = true)
    }

    fun testWithOverrides() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testUseCorrectBuiltInsForCommonModule() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_8], TargetPlatformKind.JavaScript,
                            withStdlibCommon = true, jdk = TestJdkKind.FULL_JDK,
                            configureModule = { module, platform ->
                                if (platform == TargetPlatformKind.JavaScript) {
                                    module.addLibrary(ForTestCompileRuntime.stdlibJsForTests(), kind = JSLibraryKind)
                                    module.addLibrary(ForTestCompileRuntime.stdlibCommonForTests())
                                }
                            })
    }

    fun testHeaderClassImplTypealias() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testHeaderFunUsesStdlibInSignature() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], withStdlibCommon = true, configureModule = { module, platform ->
            if (platform is TargetPlatformKind.Jvm) {
                module.addLibrary(ForTestCompileRuntime.runtimeJarForTests())
            }
        })
    }

    fun ignore_testNestedClassWithoutImpl() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testTransitive() {
        val commonModule = module("common", TestJdkKind.MOCK_JDK)
        commonModule.createFacet(TargetPlatformKind.Common, false)
        val jvmPlatform = TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]

        val baseModule = module("jvm_base", TestJdkKind.MOCK_JDK)
        baseModule.createFacet(jvmPlatform, implementedModuleName = "common")
        baseModule.enableMultiPlatform()
        baseModule.addDependency(commonModule)

        val userModule = module("jvm_user", TestJdkKind.MOCK_JDK)
        userModule.createFacet(jvmPlatform)
        userModule.enableMultiPlatform()
        userModule.addDependency(commonModule)
        userModule.addDependency(baseModule)

        checkHighlightingInAllFiles()
    }

    fun testTriangle() {
        val commonModule = module("common_base", TestJdkKind.MOCK_JDK)
        commonModule.createFacet(TargetPlatformKind.Common, false)

        val derivedModule = module("common_derived", TestJdkKind.MOCK_JDK)
        derivedModule.createFacet(TargetPlatformKind.Common)
        derivedModule.enableMultiPlatform()

        val jvmPlatform = TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
        val jvmModule = module("jvm_derived", TestJdkKind.MOCK_JDK)
        jvmModule.createFacet(jvmPlatform, implementedModuleName = "common_derived")
        jvmModule.enableMultiPlatform()
        jvmModule.addDependency(commonModule)
        jvmModule.addDependency(derivedModule)

        checkHighlightingInAllFiles()
    }

    fun testTriangleWithDependency() {
        val commonModule = module("common_base", TestJdkKind.MOCK_JDK)
        commonModule.createFacet(TargetPlatformKind.Common, false)

        val derivedModule = module("common_derived", TestJdkKind.MOCK_JDK)
        derivedModule.createFacet(TargetPlatformKind.Common)
        derivedModule.enableMultiPlatform()
        derivedModule.addDependency(commonModule)

        val jvmPlatform = TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
        val jvmModule = module("jvm_derived", TestJdkKind.MOCK_JDK)
        jvmModule.createFacet(jvmPlatform, implementedModuleName = "common_derived")
        jvmModule.enableMultiPlatform()
        jvmModule.addDependency(commonModule)
        jvmModule.addDependency(derivedModule)

        checkHighlightingInAllFiles()
    }
}