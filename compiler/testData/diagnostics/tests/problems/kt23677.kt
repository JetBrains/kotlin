// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-23677

// KT-23677: Incorrect error diagnostic for return types that coerced to 'Unit' inside lambda
fun test(flag: Boolean) {
    foo {
        if (flag) {
            <!RETURN_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>return@foo<!>
        } else {
            return@foo true
        }
    }
}

fun foo(f: () -> Boolean) {}
/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, lambdaLiteral */