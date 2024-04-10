// FIR_IDENTICAL

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
    // a MISSING_DEPENDENCY_CLASS-like error should be reported here
    withConcreteParameter(fun(arg) {})
    // a MISSING_DEPENDENCY_CLASS-like error should be reported here
    withGenericParameter(fun(arg) {})
}
