/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.konan.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder.assertIsCallOf
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.Test

@OptIn(KtAnalysisApiInternals::class)
class NativeStandaloneSessionBuilderTest {
    @Test
    fun testResolveAgainstCommonKlib() {
        lateinit var sourceModule: KtSourceModule
        val currentArchitectureTarget = HostManager.host
        val nativePlatform = NativePlatforms.nativePlatformByTargets(listOf(currentArchitectureTarget))
        val session = buildStandaloneAnalysisAPISession {
            registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())

            buildKtModuleProvider {
                platform = nativePlatform
                val kLib = addModule(
                    buildKtLibraryModule {
                        val compiledKLibRoot = compileToNativeKLib(testDataPath("resolveAgainstNativeKLib/klibSrc"))
                        addBinaryRoot(compiledKLibRoot)
                        platform = nativePlatform
                        libraryName = "klib"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("resolveAgainstNativeKLib/src"))
                        addRegularDependency(kLib)
                        platform = nativePlatform
                        moduleName = "source"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile

        val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
        ktCallExpression.assertIsCallOf(CallableId(FqName("nativeKLib"), Name.identifier("nativeKLibFunction")))
    }
}