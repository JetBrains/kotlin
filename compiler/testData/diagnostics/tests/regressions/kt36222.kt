// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: () -> String) {}
fun <K> select(x: K, y: K): K = x

fun test() {
    foo { <!TYPE_MISMATCH!>select("non-null", null)<!> } // inferred String? but String is expected
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, stringLiteral, typeParameter */
