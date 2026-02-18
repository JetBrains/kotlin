/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPrimaryConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest("testData/surface") {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun supertypeIteration(mainFile: KtFile) {
        val implClass = mainFile.declarations.first { it is KtClass && it.name == "Impl" } as KtClass
        analyze(implClass) {
            val defaultClassType = implClass.classSymbol!!.defaultType

            val allSupertypeSequence = defaultClassType.allSupertypes
            val directSupertypeSequence = defaultClassType.directSupertypes

            // Iterate through the sequence multiple times
            assertEquals(allSupertypeSequence.toList(), allSupertypeSequence.toList())
            assertEquals(directSupertypeSequence.toList(), directSupertypeSequence.toList())
        }
    }

    @Test
    fun codeFragmentCopy(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        val simpleClass = mainFile.declarations.single() as KtClass
        val method = simpleClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        val context = simpleClass.declarations.last() as KtNamedFunction
        assertions.assertEquals("context", context.name)

        val codeFragment = KtPsiFactory(mainFile.project).createExpressionCodeFragment("method()", context)
        val codeFragmentExpression = codeFragment.getContentElement() as KtCallExpression

        val codeFragmentCopy = codeFragment.copy() as KtExpressionCodeFragment
        val codeFragmentCopyExpression = codeFragmentCopy.getContentElement() as KtCallExpression

        analyze(codeFragmentExpression) {
            assertions.assertTrue(useSiteModule::class.isSubclassOf(KaDanglingFileModule::class))

            val callableSymbol = codeFragmentExpression.resolveSymbol()
            // Check whether the code fragment is configured correctly
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the original file are analyzable
            assertions.assertTrue(simpleClass.canBeAnalysed())

            // Elements from the fragment copy are not expected to be analyzable in the context of original
            // since each code fragment has its own session
            assertions.assertFalse(codeFragmentCopyExpression.canBeAnalysed())
        }

        analyze(codeFragmentCopyExpression) {
            assertions.assertTrue(useSiteModule::class.isSubclassOf(KaDanglingFileModule::class))

            val callableSymbol = codeFragmentCopyExpression.resolveSymbol()

            // Check whether the code fragment copy is configured correctly
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the original file are analyzable
            assertions.assertTrue(simpleClass.canBeAnalysed())

            // Elements from the original fragment should be analyzable in the context of copy
            assertions.assertTrue(codeFragmentExpression.canBeAnalysed())
        }
    }

    @Test
    fun enumEntryWithBodyConstructorPointerInIgnoreSelfMode(mainFile: KtFile) {
        // Create a file copy to set up resolution in IGNORE_SELF mode
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val fileCopy = ktPsiFactory.createFile("copy.kt", mainFile.text)
        fileCopy.originalFile = mainFile

        val enumClass = fileCopy.declarations.filterIsInstance<KtClass>().first()
        analyzeCopy(enumClass, KaDanglingFileResolutionMode.IGNORE_SELF) {
            val enumSymbol = enumClass.classSymbol as KaNamedClassSymbol
            val enumEntrySymbol = enumSymbol.staticMemberScope.callables
                .filterIsInstance<KaEnumEntrySymbol>()
                .single()

            // Get the implicit primary constructor
            val constructor = enumEntrySymbol.enumEntryInitializer!!.memberScope.constructors.toList().single()

            val pointer = constructor.createPointer()
            assertIs<KaFirPrimaryConstructorSymbolPointer>(pointer, "Expected primary constructor pointer")

            val restored = pointer.restoreSymbol()
            assertEquals(constructor, restored, "Constructor pointer should be restored to original constructor")
        }
    }
}
