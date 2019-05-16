// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FILE: Caller.java

class Caller {
    public static void invoke(K<String> x) {
        x.f(null);
    }
}

// FILE: test.kt

class K<T>() where T : Any, T : CharSequence? {
    fun f(x: T) {}
}

fun box() =
    try {
        Caller.invoke(K<String>())
        "fail"
    } catch (e: IllegalArgumentException) {
        "OK"
    }
