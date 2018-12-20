// WITH_RUNTIME
// TARGET_BACKEND: JVM

// FILE: First.java

import java.util.List;
import java.util.Iterator;

public class First {
    public static <A> List<A> from(List<A> var0) {
        return null;
    }
}

// FILE: second.kt

fun <T> List<T>.listFromJava() = First.from(this)
fun <T> List<T>.listFromKotlin() = fromKotlin(this)

fun <T> fromKotlin(var0: List<T?>): List<T?> = var0

fun test(a: List<Int>) {
    val b: List<Int>? = a.listFromJava()
    val c: List<Int?> = a.listFromKotlin()
}

fun box(): String {
    test(listOf(1))
    return "OK"
}