//!LANGUAGE: +DefinitelyNonNullableTypes

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755

fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y

fun main() {
    elvisLike<String>("", "").length // OK
    elvisLike<String?>(null, "").length // OK

    elvisLike("", "").length // OK
    elvisLike(null, "").length // OK
}
