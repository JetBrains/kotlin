/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class SymbolLightClassesCustomTest : AbstractAnalysisApiExecutionTest(
    testDirPathString = "analysis/symbol-light-classes/testData/custom",
) {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun fileModificationTracker(file: KtFile, testServices: TestServices) {
        val facadeLightClass = file.findFacadeClass() ?: error("Facade light class was not found")
        val classLightClass = (file.declarations.first() as KtClassOrObject).toLightClass() ?: error("Light class was not found")
        val fakeFilesWithModificationStamp = listOf(facadeLightClass, classLightClass).map { lightClass ->
            lightClass.containingFile to lightClass.containingFile.modificationStamp
        }

        // Emulate file modification
        file.clearCaches()

        for ((fakeFile, originalStamp) in fakeFilesWithModificationStamp) {
            val newStamp = fakeFile.modificationStamp
            testServices.assertions.assertTrue(originalStamp < newStamp) {
                "Expected that $fakeFile will have a modification stamp greater than $originalStamp, but $newStamp was found"
            }
        }
    }
}
