// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun foo(x: List<String>) {}
fun <K> materialize(): K = TODO()

fun test() {
    val x: Sequence<String> = sequence {
        foo(materialize())
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
