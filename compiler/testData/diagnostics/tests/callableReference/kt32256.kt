// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-32256

fun main() {
    myMethod(::refA)
    myMethod(::refB)
    anotherMethod(::refA)
    anotherMethod(::refB)
}

suspend fun refA(input: String): String {
    return input
}

suspend fun refB(): String {
    return "Hello, World!"
}

fun myMethod(f: suspend (String) -> String) {}

fun myMethod(f: suspend () -> String) {}

fun <I, O> anotherMethod(f: suspend (I) -> O) {}

fun <O> anotherMethod(f: suspend () -> O) {}