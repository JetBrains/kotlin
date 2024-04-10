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
    withConcreteParameter(fun(<!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>arg<!>) {})
    withGenericParameter(fun(<!MISSING_DEPENDENCY_CLASS!>arg<!>) {})

    withConcreteParameter(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleConcreteType<!>) {})
    withGenericParameter(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleGenericType<!><*>) {})
}
