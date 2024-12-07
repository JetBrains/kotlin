/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.Assertions
import java.nio.file.Path

abstract class AbstractStandaloneSessionBuilderAgainstStdlibTest : TestWithDisposable() {
    protected fun doTestKotlinStdLibResolve(
        targetPlatform: TargetPlatform, platformStdlibPath: Path,
        additionalStdlibRoots: List<Path> = emptyList(),
    ) {
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = targetPlatform
                val stdlib = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(platformStdlibPath)
                        addBinaryRoots(additionalStdlibRoots)
                        platform = targetPlatform
                        libraryName = "stdlib"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("stdlibFunctionUsage"))
                        addRegularDependency(stdlib)
                        platform = targetPlatform
                        moduleName = "source"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile

        // call
        val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
        ktCallExpression.assertIsSuccessfulCallOf(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf")))

        // builtin type
        val typeReference = ktFile.findDescendantOfType<KtNamedFunction>()!!.typeReference!!
        typeReference.assertIsReferenceTo(StandardClassIds.Float)
    }

    private fun KtTypeReference.assertIsReferenceTo(classId: ClassId) {
        analyze(this) {
            val actualClassId = type.expandedSymbol?.classId
            Assertions.assertEquals(classId, actualClassId)
        }
    }
}