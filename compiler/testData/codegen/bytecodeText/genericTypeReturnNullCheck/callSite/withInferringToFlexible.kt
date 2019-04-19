// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM

// FILE: J.java
public class J<T> {
    public T bar() { return null; }
}

// FILE: K.kt
fun <T> foo(): T = null as T

fun test() {
    var y = J<Int>().bar()
    y = foo()
}

// 0 IFNONNULL
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe
