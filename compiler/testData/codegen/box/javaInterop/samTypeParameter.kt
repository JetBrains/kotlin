// TARGET_BACKEND: JVM
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
