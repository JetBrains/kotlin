// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80109
// RENDER_DIAGNOSTICS_MESSAGES

fun main() {
    val x: Int? = 42

    if (x is <!INCOMPATIBLE_TYPES!>Byte?<!>) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, isExpression, localProperty, nullableType,
propertyDeclaration */
