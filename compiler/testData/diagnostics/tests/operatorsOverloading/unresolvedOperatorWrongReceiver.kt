// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
operator fun String.plus(a: Any) {}

fun test() {
    val a = Any()

    a <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> a
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, incrementDecrementExpression, localProperty,
multiplicativeExpression, propertyDeclaration, rangeExpression, stringLiteral, unaryExpression */
