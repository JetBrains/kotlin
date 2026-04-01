// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: box.kt
fun <U> get(x: Sam<U>): U =
    x.get()

fun test() {
    get<Map<String, String>>(::mutableMapOf)
}

fun box(): String {
    test()
    return "OK"
}

// FILE: Sam.java
public interface Sam<T> {
    T get();

    static void run() {
        BoxKt.test();
    }
}
