// MODULE: start
// FILE: start.kt

interface InaccessibleConcreteType
interface InaccessibleGenericType<T>

// MODULE: middle(start)
// FILE: middle.kt

fun withConcreteParameter(arg: (InaccessibleConcreteType) -> Unit) {}
fun withGenericParameter(arg: (InaccessibleGenericType<*>) -> Unit) {}

// MODULE: end(middle)
// FILE: end.kt

fun test() {
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withConcreteParameter<!>(fun(<!MISSING_DEPENDENCY_CLASS!>arg<!>) {})
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withGenericParameter<!>(fun(<!MISSING_DEPENDENCY_CLASS!>arg<!>) {})

    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withConcreteParameter<!>(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleConcreteType<!>) {})
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withGenericParameter<!>(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleGenericType<!><*>) {})
}
