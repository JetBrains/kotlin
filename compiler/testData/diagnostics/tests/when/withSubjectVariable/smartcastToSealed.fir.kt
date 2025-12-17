// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -UNUSED_VARIABLE

sealed class Either
class Left : Either()
class Right : Either()

fun testSmartcastToSealedInSubjectInitializer1(x: Any?) {
    val y1 = <!WHEN_ON_SEALED_GEEN_ELSE!>when (val either = x as Either) {
        is Left -> "L"
        is Right -> "R"
    }<!>
}

fun testSmartcastToSealedInSubjectInitializer2(x: Any?) {
    val y2 = <!NO_ELSE_IN_WHEN!>when<!> (val either: Any = x as Either) { // NB explicit type annotation
        is Left -> "L"
        is Right -> "R"
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, isExpression, localProperty, nullableType,
propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
