// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(f: () -> T) {}
fun bar(g: () -> Unit) {}
fun <K> baz(): () -> K = TODO()

fun test() {
    foo { bar(baz()) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeParameter */
