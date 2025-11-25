// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80109
// RENDER_DIAGNOSTIC_ARGUMENTS

fun main() {
    val x: Int? = 42

    if (<!IMPOSSIBLE_IS_CHECK_WARNING("false")!>x is Byte?<!>) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, isExpression, localProperty, nullableType,
propertyDeclaration */
