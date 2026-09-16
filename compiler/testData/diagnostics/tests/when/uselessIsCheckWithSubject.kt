// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-80652

fun foo(): String {
    when (val res = create()) {
        <!USELESS_IS_CHECK!>is String<!> -> {
            return res
        }
    }
    return ""
}

private fun create() = "abc"

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, localProperty, propertyDeclaration, stringLiteral,
whenExpression, whenWithSubject */
