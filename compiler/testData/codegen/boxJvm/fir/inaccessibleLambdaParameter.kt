// TARGET_BACKEND: JVM
// ISSUE: KT-62525

// MODULE: start
// FILE: start.kt

interface InaccessibleGenericType<T>

// MODULE: middle(start)
// FILE: middle.kt

fun withGenericParameter(arg: (InaccessibleGenericType<*>) -> Unit) {}

// MODULE: end(middle)
// FILE: end.kt

fun box(): String {
    @Suppress("MISSING_DEPENDENCY_CLASS")
    withGenericParameter {}

    return "OK"
}
