// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB


fun <T> List<Option<T>>.flatten(): List<T> = flatMap { it.fold(::emptyList, ::listOf) }

class Option<out T> {
    fun <R> fold(ifEmpty: () -> R, ifSome: (T) -> R): R = TODO()
}