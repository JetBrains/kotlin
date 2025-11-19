/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api/testData/surface") {
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
    fun functionTypeContextParameterSymbolRestoration(mainFile: KtFile) {
        val testFunction = mainFile.declarations.firstIsInstance<KtFunction>().also { check(it.name == "test") }
        val targetTypeReference = testFunction.valueParameters.first { it.name == "block" }.typeReference!!

        val fooClassId = ClassId.fromString("/Foo")

        lateinit var functionTypeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>
        lateinit var contextParameterSymbolPointer: KaSymbolPointer<KaContextParameterSymbol>

        analyze(mainFile) {
            val contextParameterSymbol = (targetTypeReference.type as KaFunctionType).contextParameters.single()

            assert(contextParameterSymbol.name.isSpecial)
            assert(contextParameterSymbol.returnType.isClassType(fooClassId))

            functionTypeParameterSymbolPointer = testFunction.symbol.typeParameters[0].createPointer()
            contextParameterSymbolPointer = contextParameterSymbol.createPointer()
        }

        analyze(mainFile) {
            val functionTypeParameterSymbol = functionTypeParameterSymbolPointer.restoreSymbol()
                ?: error("Cannot restore the function type parameter symbol")

            val contextParameterSymbol = contextParameterSymbolPointer.restoreSymbol()
                ?: error("Cannot restore the context parameter symbol")

            val expectedType = typeCreator.classType(fooClassId) {
                invariantTypeArgument(typeParameterType(functionTypeParameterSymbol))
            }

            assert(contextParameterSymbol.name.isSpecial)
            assertEquals(expectedType, contextParameterSymbol.returnType)
        }
    }
}
