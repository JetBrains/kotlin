// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

<!CONTEXT_CLASS_OR_CONSTRUCTOR!>context<!>(T) class A<T>

<!CONTEXT_CLASS_OR_CONSTRUCTOR!>context<!>(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
    <!NO_CONTEXT_ARGUMENT!>A<!><String>()
}

fun Collection<Int>.bar() {
    B<Int>()
    <!NO_CONTEXT_ARGUMENT!>B<!><String>()
}
