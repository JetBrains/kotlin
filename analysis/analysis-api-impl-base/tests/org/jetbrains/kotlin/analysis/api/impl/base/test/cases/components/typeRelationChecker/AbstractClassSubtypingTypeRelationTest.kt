/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test covers the following functions with strict and lenient error type policies:
 *
 * - [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf] for [ClassId]
 */
abstract class AbstractClassSubtypingTypeRelationTest : AbstractTypeRelationTest() {
    protected abstract val errorTypePolicy: KaSubtypingErrorTypePolicy

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(ClassSubtypingTestDirectives)
        }
    }

    override fun KaSession.checkExpectedResult(expectedResult: Boolean, mainFile: KtFile, testServices: TestServices) {
        val type = getTypeAtMarker(mainFile, testServices, caretTag = "type1")
        val classId = ClassId.fromString(
            testServices.moduleStructure.allDirectives.singleValue(ClassSubtypingTestDirectives.SUPERCLASS_ID)
        )

        testServices.assertions.assertEquals(
            expectedResult,
            checkIsSubtype(type, classId),
        ) {
            val leniencyMessage = if (errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT) " with error type leniency" else ""

            "Expected `$type`${if (!expectedResult) " not" else ""} to be a subtype of `$classId`$leniencyMessage (`$resultDirective`)."
        }
    }

    protected abstract fun KaSession.checkIsSubtype(type: KaType, classId: ClassId): Boolean
}

private object ClassSubtypingTestDirectives : SimpleDirectivesContainer() {
    val SUPERCLASS_ID by stringDirective(
        description = "The superclass ID to check against the specified type."
    )

    val IS_CLASS_SUBTYPE by stringDirective(
        description = "Whether `type` is a subtype of the specified class ID (`true` or `false`).",
    )

    val IS_CLASS_SUBTYPE_LENIENT by stringDirective(
        description = "Whether `type` is a subtype of the specified class ID (`true` or `false`) when using the" +
                " `${KaSubtypingErrorTypePolicy.LENIENT}` error type policy.",
    )
}

abstract class AbstractClassIdSubtypingTypeRelationTest : AbstractClassSubtypingTypeRelationTest() {
    override fun KaSession.checkIsSubtype(type: KaType, classId: ClassId): Boolean = type.isSubtypeOf(classId, errorTypePolicy)
}

abstract class AbstractNonLenientClassIdSubtypingTypeRelationTest : AbstractClassIdSubtypingTypeRelationTest() {
    override val resultDirective get() = ClassSubtypingTestDirectives.IS_CLASS_SUBTYPE

    override val errorTypePolicy: KaSubtypingErrorTypePolicy get() = KaSubtypingErrorTypePolicy.STRICT
}

abstract class AbstractLenientClassIdSubtypingTypeRelationTest : AbstractClassIdSubtypingTypeRelationTest() {
    override val resultDirective get() = ClassSubtypingTestDirectives.IS_CLASS_SUBTYPE_LENIENT

    override val errorTypePolicy: KaSubtypingErrorTypePolicy get() = KaSubtypingErrorTypePolicy.LENIENT
}

abstract class AbstractClassSymbolSubtypingTypeRelationTest : AbstractClassSubtypingTypeRelationTest() {
    override fun KaSession.checkIsSubtype(type: KaType, classId: ClassId): Boolean {
        val symbol = findClass(classId)
            ?: findTypeAlias(classId)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return type.isSubtypeOf(symbol, errorTypePolicy)
    }
}

abstract class AbstractNonLenientClassSymbolSubtypingTypeRelationTest : AbstractClassSymbolSubtypingTypeRelationTest() {
    override val resultDirective get() = ClassSubtypingTestDirectives.IS_CLASS_SUBTYPE

    override val errorTypePolicy: KaSubtypingErrorTypePolicy get() = KaSubtypingErrorTypePolicy.STRICT
}

abstract class AbstractLenientClassSymbolSubtypingTypeRelationTest : AbstractClassSymbolSubtypingTypeRelationTest() {
    override val resultDirective get() = ClassSubtypingTestDirectives.IS_CLASS_SUBTYPE_LENIENT

    override val errorTypePolicy: KaSubtypingErrorTypePolicy get() = KaSubtypingErrorTypePolicy.LENIENT
}
