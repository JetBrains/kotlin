// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: java.lang.ClassNotFoundException: error.NonExistentClass (shouldn't work anyway due to MISSING_DEPENDENCY_CLASS)
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
