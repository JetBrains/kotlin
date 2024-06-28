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
    withConcreteParameter(fun(arg) {})
    withGenericParameter(fun(arg) {})

    withConcreteParameter(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleConcreteType<!>) {})
    withGenericParameter(fun(arg: <!UNRESOLVED_REFERENCE!>InaccessibleGenericType<!><*>) {})
}
