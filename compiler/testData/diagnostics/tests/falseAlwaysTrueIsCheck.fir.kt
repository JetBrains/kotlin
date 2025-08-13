// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80109
// RENDER_DIAGNOSTICS_MESSAGES

fun main() {
    val x: Int? = 42

    if (<!USELESS_IS_CHECK("false")!>x is Byte?<!>) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, isExpression, localProperty, nullableType,
propertyDeclaration */
