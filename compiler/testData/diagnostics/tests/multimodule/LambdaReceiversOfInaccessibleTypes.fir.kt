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
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withConcreteReceiver<!> <!MISSING_DEPENDENCY_CLASS!>{}<!>
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>withGenericReceiver<!> <!MISSING_DEPENDENCY_CLASS!>{}<!>
}
