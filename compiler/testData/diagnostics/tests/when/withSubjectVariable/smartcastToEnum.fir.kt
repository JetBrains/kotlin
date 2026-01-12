// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -UNUSED_VARIABLE

enum class E { FIRST, SECOND }

fun testSmartcastToEnumInSubjectInitializer1(e: E?) {
    val x1 = <!WHEN_ON_SEALED!>when (val ne = e!!) {
        E.FIRST -> "f"
        E.SECOND -> "s"
    }<!>
}

fun testSmartcastToEnumInSubjectInitializer2(e: E?) {
    val x2 = <!NO_ELSE_IN_WHEN!>when<!> (val ne: Any = e!!) { // NB explicit type annotation
        E.FIRST -> "f"
        E.SECOND -> "s"
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
