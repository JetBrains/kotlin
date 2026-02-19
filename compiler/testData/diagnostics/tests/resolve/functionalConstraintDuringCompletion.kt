// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +CheckLambdaAgainstTypeVariableContradictionInResolution

interface In<in U> {}

fun <T> foo(
    i: In<T>,
    y: T,
    b: () -> In<T>
) {}

fun bar(a: In<Any>, value: In<suspend () -> String>) {
    foo(
        a,
        {
            baz()
            ""
        },
        {
            value
        }
    )
}

suspend fun baz() {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, in, interfaceDeclaration, lambdaLiteral, nullableType,
stringLiteral, suspend, typeParameter */
