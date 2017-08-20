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

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForModuleComputationTracker
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.completion.test.withServiceRegistered
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.TestJdkKind.FULL_JDK

open class MultiModuleHighlightingTest : AbstractMultiModuleHighlightingTest() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/"

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

    fun testLazyResolvers() {
        val resolversComputed = mutableSetOf<Module>()

        val resolversTracker = object : ResolverForModuleComputationTracker {
            override fun onResolverComputed(moduleInfo: ModuleInfo) {
                (moduleInfo as IdeaModuleInfo).let {
                    if (it is ModuleSourceInfo) {
                        val module = it.module
                        resolversComputed.add(module)
                    }
                }
            }
        }

        project.withServiceRegistered<ResolverForModuleComputationTracker, Unit>(resolversTracker) {
            val module1 = module("m1")
            val module2 = module("m2")
            val module3 = module("m3")

            module3.addDependency(module2)
            module3.addDependency(module1)

            assertTrue(module1 !in resolversComputed)
            assertTrue(module2 !in resolversComputed)
            assertTrue(module3 !in resolversComputed)

            checkHighlightingInAllFiles { "m3" in file.name }

            assertTrue(module1 in resolversComputed)
            assertTrue(module2 !in resolversComputed)
            assertTrue(module3 in resolversComputed)
        }
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

    fun testLanguageVersionsViaFacets() {
        val m1 = module("m1", FULL_JDK).setupKotlinFacet {
            settings.languageLevel = LanguageVersion.KOTLIN_1_1
        }
        val m2 = module("m2", FULL_JDK).setupKotlinFacet {
            settings.languageLevel = LanguageVersion.KOTLIN_1_0
        }

        m1.addDependency(m2)
        m2.addDependency(m1)

        checkHighlightingInAllFiles()
    }

    private fun Module.setupKotlinFacet(configure: KotlinFacetConfiguration.() -> Unit) = apply {
        runWriteAction {
            val facet = FacetManager.getInstance(this).addFacet(KotlinFacetType.INSTANCE, KotlinFacetType.NAME, null)
            val configuration = facet.configuration

            // this is actually needed so facet settings object is in a valid state
            configuration.settings.compilerArguments = K2JVMCompilerArguments()
            // make sure module-specific settings are used
            configuration.settings.useProjectSettings = false

            configuration.configure()
        }
    }

    class MultiPlatform : AbstractMultiModuleHighlightingTest() {
        override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleHighlighting/multiplatform/"

        fun testBasic() {
            doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        }

        fun testHeaderClass() {
            doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        }

        fun testHeaderWithoutImplForBoth() {
            doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], TargetPlatformKind.JavaScript)
        }

        fun testHeaderPartiallyImplemented() {
            doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
        }

        fun testHeaderFunctionProperty() {
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
                                withStdlibCommon = true, jdk = FULL_JDK, configureModule = { module, platform ->
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
    }
}
