// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

interface A<T> {
    var x: T
}

fun test(a: A<*>) {
    a.x
}

fun box(): String {
    return "OK"
}