// TODO: new inference doesn't do SAM conversion in this case. KT-37149
// !LANGUAGE: -NewInference
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: Generic.java
class Generic<T> {
    T id(T x) { return x; }
}

// FILE: Specialized.java
class Specialized extends Generic<Runnable> {}

// FILE: use.kt
fun box(): String {
    var result = "fail"
    Specialized().id { result = "OK" }.run()
    return result
}
