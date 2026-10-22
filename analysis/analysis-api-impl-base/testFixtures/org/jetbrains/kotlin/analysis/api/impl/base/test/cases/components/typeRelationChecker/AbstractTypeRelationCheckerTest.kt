/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.expressions.expressionType
import org.jetbrains.kotlin.analysis.api.symbols.findClassLike
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.types.semanticallyEquals
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test covers the following functions with strict and lenient error type policies:
 *
 * - [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.semanticallyEquals]
 * - [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf] for [KaType], [ClassId], and
 *   [org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol]
 *
 * Each relation is checked against its own boolean directive. All relations share the same test data,
 * so they are checked in a single test to run the analysis only once per file.
 */
abstract class AbstractTypeRelationCheckerTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(TypeRelationCheckerTestDirectives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            checkRelations(contextFile, testServices)
        }
    }

    context(_: KaSession)
    private fun checkRelations(mainFile: KtFile, testServices: TestServices) {
        val directives = testServices.moduleStructure.allDirectives
        val type1 = getTypeAtMarker(mainFile, testServices, qualifier = "type1")
        val type2 = getTypeAtMarker(mainFile, testServices, qualifier = "type2")
        val superclassId = ClassId.fromString(directives.singleValue(TypeRelationCheckerTestDirectives.SUPERCLASS_ID))

        fun check(resultDirective: StringDirective, relation: String, actualResult: () -> Boolean): () -> Unit = {
            val expectedResult = directives.singleValue(resultDirective).toBooleanStrict()
            testServices.assertions.assertEquals(expectedResult, actualResult()) {
                "Expected $relation to be `$expectedResult` (`$resultDirective`)."
            }
        }

        val lenient = KaSubtypingErrorTypePolicy.LENIENT
        val strict = KaSubtypingErrorTypePolicy.STRICT

        testServices.assertions.assertAll(
            check(TypeRelationCheckerTestDirectives.ARE_EQUAL, "`$type1`.semanticallyEquals(`$type2`)") {
                type1.semanticallyEquals(type2)
            },
            check(TypeRelationCheckerTestDirectives.ARE_EQUAL_LENIENT, "`$type1`.semanticallyEquals(`$type2`, $lenient)") {
                type1.semanticallyEquals(type2, lenient)
            },
            check(TypeRelationCheckerTestDirectives.IS_SUBTYPE, "`$type1`.isSubtypeOf(`$type2`)") {
                type1.isSubtypeOf(type2)
            },
            check(TypeRelationCheckerTestDirectives.IS_SUBTYPE_LENIENT, "`$type1`.isSubtypeOf(`$type2`, $lenient)") {
                type1.isSubtypeOf(type2, lenient)
            },
            check(TypeRelationCheckerTestDirectives.IS_CLASS_SUBTYPE, "`$type1`.isSubtypeOf(ClassId `$superclassId`, $strict)") {
                type1.isSubtypeOf(superclassId, strict)
            },
            check(TypeRelationCheckerTestDirectives.IS_CLASS_SUBTYPE_LENIENT, "`$type1`.isSubtypeOf(ClassId `$superclassId`, $lenient)") {
                type1.isSubtypeOf(superclassId, lenient)
            },
            check(TypeRelationCheckerTestDirectives.IS_CLASS_SUBTYPE, "`$type1`.isSubtypeOf(symbol `$superclassId`, $strict)") {
                isSubtypeOfClassSymbol(type1, superclassId, strict)
            },
            check(TypeRelationCheckerTestDirectives.IS_CLASS_SUBTYPE_LENIENT, "`$type1`.isSubtypeOf(symbol `$superclassId`, $lenient)") {
                isSubtypeOfClassSymbol(type1, superclassId, lenient)
            },
        )
    }

    context(_: KaSession)
    private fun isSubtypeOfClassSymbol(type: KaType, classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        val symbol = findClassLike(classId) ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT
        return type.isSubtypeOf(symbol, errorTypePolicy)
    }

    context(_: KaSession)
    private fun getTypeAtMarker(mainFile: KtFile, testServices: TestServices, qualifier: String): KaType {
        val element = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtElement>(mainFile, qualifier)
            ?: testServices.expressionMarkerProvider.getTopmostSelectedElement(mainFile)

        return when (element) {
            is KtProperty -> element.symbol.returnType
            is KtExpression -> element.expressionType ?: error("Expected the selected expression to have a type.")
            else -> error("Expected a property or an expression.")
        }
    }
}

private object TypeRelationCheckerTestDirectives : SimpleDirectivesContainer() {
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

    val SUPERCLASS_ID by stringDirective(
        description = "The superclass ID to check `type1` against.",
    )

    val IS_CLASS_SUBTYPE by stringDirective(
        description = "Whether `type1` is a subtype of the specified class ID (`true` or `false`).",
    )

    val IS_CLASS_SUBTYPE_LENIENT by stringDirective(
        description = "Whether `type1` is a subtype of the specified class ID (`true` or `false`) when using the" +
                " `${KaSubtypingErrorTypePolicy.LENIENT}` error type policy.",
    )
}
