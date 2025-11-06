/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AbstractStandaloneTest
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

class StandaloneBehaviorTest : AbstractStandaloneTest() {
    override val suiteName: String
        get() = "behavior"

    @Test
    fun testStubbedAnnotationArguments() {
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                val stdlibModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(StandardLibrariesPathProviderForKotlinProject.runtimeJarForTests().toPath())
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "stdlib"
                    }
                )

                platform = JvmPlatforms.defaultJvmPlatform
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("stubbedAnnotationArguments"))
                        addRegularDependency(stdlibModule)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "source"
                    }
                )
            }
        }

        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        assert(ktFile.stub != null)

        val mainClass = ktFile.declarations.first { it is KtClass && it.name == "Main" } as KtClass
        val parameter = mainClass.primaryConstructor!!.valueParameters.first()

        analyze(parameter) {
            for (annotation in parameter.symbol.annotations) {
                for (argument in annotation.arguments) {
                    val argumentExpression = argument.expression
                    if (argumentExpression is KaAnnotationValue.ArrayValue) {
                        assertEquals(3, argumentExpression.values.size)
                        return
                    }
                }
            }
        }

        error("Annotation argument wasn't found")
    }
}