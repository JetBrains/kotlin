// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

import kotlin.test.assertEquals

fun test() {
    val u = when (true) {
        true -> 42
        else -> 1.0
    }

    assertEquals(42, u)
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, intersectionType, localProperty,
propertyDeclaration, whenExpression, whenWithSubject */
