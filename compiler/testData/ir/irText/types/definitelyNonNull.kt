//!LANGUAGE: +DefinitelyNonNullableTypes

fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y

fun main() {
    elvisLike<String>("", "").length // OK
    elvisLike<String?>(null, "").length // OK

    elvisLike("", "").length // OK
    elvisLike(null, "").length // OK
}