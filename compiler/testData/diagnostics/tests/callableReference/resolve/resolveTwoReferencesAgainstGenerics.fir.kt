// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_RUNTIME


fun <T> List<Option<T>>.flatten(): List<T> = <!AMBIGUITY!>flatMap<!> { <!UNRESOLVED_REFERENCE!>it<!>.<!NONE_APPLICABLE!>fold<!>(::emptyList, <!UNRESOLVED_REFERENCE!>::listOf<!>) }

class Option<out T> {
    fun <R> fold(ifEmpty: () -> R, ifSome: (T) -> R): R = TODO()
}
