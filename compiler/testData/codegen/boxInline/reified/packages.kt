// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

public abstract class A<T>

inline fun <reified T> foo1(): A<T> {
    return object : A<T>() {

    }
}

fun<T> bar(x: T, block: (T) -> Boolean): Boolean = block(x)

inline fun <reified T> foo2(x: Any): Boolean {
    return bar(x) { it is T }
}

inline fun <reified T> foo3(x: Any, y: Any): Boolean {
    return bar(x) { it is T && y is T }
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val x = foo1<Int>().javaClass.getGenericSuperclass()?.toString()
    if (x != "test.A<java.lang.Integer>") return "fail 1: " + x

    if (!foo2<String>("abc")) return "fail 2"
    if (foo2<Int>("abc")) return "fail 3"

    if (!foo3<String>("abc", "cde")) return "fail 4"
    if (foo3<String>("abc", 1)) return "fail 5"

    return "OK"
}
