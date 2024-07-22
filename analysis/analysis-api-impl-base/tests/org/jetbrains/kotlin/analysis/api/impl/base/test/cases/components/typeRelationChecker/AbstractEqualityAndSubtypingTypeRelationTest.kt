/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * This test covers the following functions with strict and lenient error type policies:
 *
 * - [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.semanticallyEquals]
 * - [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf] for [KaType]
 */
abstract class AbstractEqualityAndSubtypingTypeRelationTest : AbstractTypeRelationTest() {
    protected abstract fun KaSession.checkTypes(
        expectedResult: Boolean,
        type1: KaType,
        type2: KaType,
        testServices: TestServices,
    )

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(EqualityAndSubtypingTestDirectives)
        }
    }

    override fun KaSession.checkExpectedResult(expectedResult: Boolean, mainFile: KtFile, testServices: TestServices) {
        val type1 = getTypeAtCaret(mainFile, testServices, caretTag = "type1")
        val type2 = getTypeAtCaret(mainFile, testServices, caretTag = "type2")

        checkTypes(expectedResult, type1, type2, testServices)
    }
}

private object EqualityAndSubtypingTestDirectives : SimpleDirectivesContainer() {
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

abstract class AbstractTypeEqualityTest : AbstractEqualityAndSubtypingTypeRelationTest() {
    override val resultDirective = EqualityAndSubtypingTestDirectives.ARE_EQUAL

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.semanticallyEquals(type2),
        ) {
            "Expected `$type1` and `$type2` to be ${if (!expectedResult) "un" else ""}equal (`$resultDirective`)."
        }
    }
}

abstract class AbstractLenientTypeEqualityTest : AbstractEqualityAndSubtypingTypeRelationTest() {
    override val resultDirective = EqualityAndSubtypingTestDirectives.ARE_EQUAL_LENIENT

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.semanticallyEquals(type2, KaSubtypingErrorTypePolicy.LENIENT),
        ) {
            "Expected `$type1` and `$type2` to be ${if (!expectedResult) "un" else ""}equal with error type leniency (`$resultDirective`)."
        }
    }
}

abstract class AbstractSubtypingTest : AbstractEqualityAndSubtypingTypeRelationTest() {
    override val resultDirective = EqualityAndSubtypingTestDirectives.IS_SUBTYPE

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isSubtypeOf(type2),
        ) {
            "Expected `$type1` to ${if (!expectedResult) "not " else ""}be a subtype of `$type2` (`$resultDirective`)."
        }
    }
}

abstract class AbstractLenientSubtypingTest : AbstractEqualityAndSubtypingTypeRelationTest() {
    override val resultDirective = EqualityAndSubtypingTestDirectives.IS_SUBTYPE_LENIENT

    override fun KaSession.checkTypes(expectedResult: Boolean, type1: KaType, type2: KaType, testServices: TestServices) {
        testServices.assertions.assertEquals(
            expectedResult,
            type1.isSubtypeOf(type2, KaSubtypingErrorTypePolicy.LENIENT),
        ) {
            "Expected `$type1` to ${if (!expectedResult) "not " else ""}be a subtype of `$type2` with error type leniency" +
                    " (`$resultDirective`)."
        }
    }
}
