// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80222, KTLC-365

fun main() {
    val x: Int = 42
    <!IMPOSSIBLE_IS_CHECK_ERROR!>x is Byte<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, isExpression, localProperty, propertyDeclaration */
