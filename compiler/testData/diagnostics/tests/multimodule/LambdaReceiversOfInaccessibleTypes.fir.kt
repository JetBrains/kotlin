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
    withConcreteReceiver <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER!>{}<!>
    withGenericReceiver <!MISSING_DEPENDENCY_CLASS!>{}<!>
}
