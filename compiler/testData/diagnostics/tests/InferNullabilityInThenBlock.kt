// RUN_PIPELINE_TILL: BACKEND
fun ff(a: String) = 1

fun gg() {
    val a: String? = ""

    if (a != null) {
        ff(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, smartcast, stringLiteral */
