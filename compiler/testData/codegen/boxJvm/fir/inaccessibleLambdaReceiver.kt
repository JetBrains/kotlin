// TARGET_BACKEND: JVM
// ISSUE: KT-66743

// MODULE: start
// FILE: start.kt

interface InaccessibleGenericType<T>

// MODULE: middle(start)
// FILE: middle.kt

fun withGenericReceiver(arg: InaccessibleGenericType<*>.() -> Unit) {}

// MODULE: end(middle)
// FILE: end.kt

fun box(): String {
    @Suppress("MISSING_DEPENDENCY_CLASS")
    withGenericReceiver {}

    return "OK"
}
