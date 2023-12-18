// FIR_IDENTICAL
//!LANGUAGE: +DefinitelyNonNullableTypes

fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y

fun runMe() {
    elvisLike<String>("", "").length // OK
    elvisLike<String?>(null, "").length // OK

    elvisLike("", "").length // OK
    elvisLike(null, "").length // OK
}
