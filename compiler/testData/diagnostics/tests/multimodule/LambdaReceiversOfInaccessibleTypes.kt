// FIR_IDENTICAL

// MODULE: start
// FILE: start.kt

interface InaccessibleConcreteType
interface InaccessibleGenericType<T>

// MODULE: middle(start)
// FILE: middle.kt

fun withConcreteReceiver(arg: InaccessibleConcreteType.() -> Unit) {}
fun withGenericReceiver(arg: InaccessibleGenericType<*>.() -> Unit) {}

// MODULE: end(middle)
// FILE: end.kt

fun test() {
    // a MISSING_DEPENDENCY_CLASS-like error should be reported here
    withConcreteReceiver {}
    // a MISSING_DEPENDENCY_CLASS-like error should be reported here
    withGenericReceiver {}
}
