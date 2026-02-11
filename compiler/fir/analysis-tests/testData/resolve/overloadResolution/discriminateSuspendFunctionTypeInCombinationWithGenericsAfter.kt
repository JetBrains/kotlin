// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ParseLambdaWithSuspendModifier, +DiscriminateSuspendInOverloadResolution

fun foo0(f: () -> Unit): String = ""
fun <T> foo0(f: () -> Unit): List<T> = TODO()
fun foo0(f: suspend () -> Unit): Int = 0

fun test() {
    accept<String>(foo0({}))
    accept<List<String>>(foo0<String>({}))
    accept<Int>(foo0(suspend {}))
}

fun <T> accept(t: T) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
suspend, typeParameter, typeWithExtension */
