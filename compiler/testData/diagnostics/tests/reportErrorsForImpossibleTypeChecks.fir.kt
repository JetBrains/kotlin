// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80222, KTLC-365

fun main() {
    val x: Int = 42
    <!USELESS_IS_CHECK!>x is Byte<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, isExpression, localProperty, propertyDeclaration */
