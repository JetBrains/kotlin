/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.resolution

import com.intellij.util.asSafely
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AbstractStandaloneTest
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(KaExperimentalApi::class)
class StandaloneLazyResolutionTest : AbstractStandaloneTest() {
    override val suiteName: String
        get() = "lazyResolution"

    /**
     * The test reproduces KT-82945. Reproducing the same exception with the LL FIR and member scope test infrastructures was not possible
     * without upgrades to the test infrastructure.
     */
    @Test
    fun testActualTypeAlias() {
        lateinit var commonJvmSourceModule: KaSourceModule
        lateinit var jvmSourceModule: KaSourceModule

        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val jdk = addModule(
                    buildKtSdkModule {
                        addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = true)
                        addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = false)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "JDK"
                    }
                )
                val langSettings = LanguageVersionSettingsImpl(
                    LanguageVersion.LATEST_STABLE,
                    ApiVersion.LATEST_STABLE,
                    specificFeatures = hashMapOf(
                        LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED,
                        LanguageFeature.ExpectActualClasses to LanguageFeature.State.ENABLED,
                    )
                )
                commonJvmSourceModule = addModule(
                    buildKtSourceModule {
                        languageVersionSettings = langSettings
                        addSourceRoot(testDataPath("actualTypeAlias").resolve("commonJvm"))
                        addRegularDependency(jdk)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "commonJvm"
                    }
                )
                jvmSourceModule = addModule(
                    buildKtSourceModule {
                        languageVersionSettings = langSettings
                        addSourceRoot(testDataPath("actualTypeAlias").resolve("jvm"))
                        addDependsOnDependency(commonJvmSourceModule)
                        addRegularDependency(jdk)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "jvm"
                    }
                )
            }
        }

        // Visit some interfaces in commonJvm module, and get the memberScope.
        for (file in session.modulesWithFiles[commonJvmSourceModule]!!.filterIsInstance<KtFile>()) {
            analyze(file) {
                val ktClass = file.findDescendantOfType<KtClass>()
                ktClass!!.symbol.asSafely<KaNamedClassSymbol>()!!.memberScope
            }
        }

        // Now visit MyClass in the jvm module, and get the memberScope ->
        // KotlinIllegalArgumentExceptionWithAttachments
        val ktFile = session.modulesWithFiles.getValue(jvmSourceModule).first { it.name == "MyClass.kt" } as KtFile
        analyze(ktFile) {
            ktFile.findDescendantOfType<KtClass>()!!.symbol.asSafely<KaNamedClassSymbol>()!!.memberScope
        }
    }
}
