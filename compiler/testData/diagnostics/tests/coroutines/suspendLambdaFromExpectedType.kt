// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

fun <T> runBlocking(block: suspend () -> T): T = TODO()

fun foo() = runBlocking<Unit> {
    val foo: suspend (String) -> Int = {
        it.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, suspend, typeParameter */
