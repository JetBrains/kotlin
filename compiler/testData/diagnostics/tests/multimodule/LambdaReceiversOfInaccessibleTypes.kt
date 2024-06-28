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
    withConcreteReceiver {}
    withGenericReceiver {}
}
