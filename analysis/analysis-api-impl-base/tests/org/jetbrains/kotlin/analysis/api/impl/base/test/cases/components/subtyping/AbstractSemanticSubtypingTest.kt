/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.subtyping

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractSemanticSubtypingTest : AbstractAnalysisApiBasedTest() {
    protected abstract val resultDirective: StringDirective

    protected abstract fun KaSession.checkTypes(
        expectedResult: Boolean,
        type1: KaType,
        type2: KaType,
        testServices: TestServices,
    )

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(SemanticSubtypingTestDirectives)
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expectedResult = testServices.moduleStructure.allDirectives
            .singleValue(resultDirective)
            .toBooleanStrict()

        analyseForTest(mainFile) {
            val type1 = getTypeAtCaret("type1", mainFile, testServices)
            val type2 = getTypeAtCaret("type2", mainFile, testServices)

            checkTypes(expectedResult, type1, type2, testServices)
        }
    }

    private fun KaSession.getTypeAtCaret(caretTag: String, mainFile: KtFile, testServices: TestServices): KaType {
        val element = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(mainFile, caretTag)
        return when (element) {
            is KtProperty -> element.getVariableSymbol().returnType
            is KtExpression -> element.getKaType() ?: error("Expected the selected expression to have a type.")
            else -> error("Expected a property or an expression.")
        }
    }
}

private object SemanticSubtypingTestDirectives : SimpleDirectivesContainer() {
    val ARE_EQUAL by stringDirective(
        description = "Whether the two types are equal (`true` or `false`).",
    )

    val ARE_EQUAL_LENIENT by stringDirective(
        description = "Whether the two types are equal (`true` or `false`) when using the `${KaSubtypingErrorTypePolicy.LENIENT}` error" +
                " type policy.",
    )

    val IS_SUBTYPE by stringDirective(
        description = "Whether `type1` is a subtype of `type2` (`true` or `false`).",
    )

    val IS_SUBTYPE_LENIENT by stringDirective(
        description = "Whether `type1` is a subtype of `type2` (`true` or `false`) when using the `${KaSubtypingErrorTypePolicy.LENIENT}`" +
                " error type policy.",
    )
}

abstract class AbstractTypeEqualityTest : AbstractSemanticSubtypingTest() {
    override val resultDirective = SemanticSubtypingTestDirectives.ARE_EQUAL

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isEqualTo(type2),
        ) {
            "Expected `$type1` and `$type2` to be ${if (!expectedResult) "un" else ""}equal (`$resultDirective`)."
        }
    }
}

abstract class AbstractLenientTypeEqualityTest : AbstractSemanticSubtypingTest() {
    override val resultDirective = SemanticSubtypingTestDirectives.ARE_EQUAL_LENIENT

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isEqualTo(type2, KaSubtypingErrorTypePolicy.LENIENT),
        ) {
            "Expected `$type1` and `$type2` to be ${if (!expectedResult) "un" else ""}equal with error type leniency (`$resultDirective`)."
        }
    }
}

abstract class AbstractSubtypingTest : AbstractSemanticSubtypingTest() {
    override val resultDirective = SemanticSubtypingTestDirectives.IS_SUBTYPE

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isSubTypeOf(type2),
        ) {
            "Expected `$type1` to ${if (!expectedResult) "not " else ""}be a subtype of `$type2` (`$resultDirective`)."
        }
    }
}

abstract class AbstractLenientSubtypingTest : AbstractSemanticSubtypingTest() {
    override val resultDirective = SemanticSubtypingTestDirectives.IS_SUBTYPE_LENIENT

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isSubTypeOf(type2, KaSubtypingErrorTypePolicy.LENIENT),
        ) {
            "Expected `$type1` to ${if (!expectedResult) "not " else ""}be a subtype of `$type2` with error type leniency" +
                    " (`$resultDirective`)."
        }
    }
}
