import Java

// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt
expect class A {
    fun foo(a: Int.() -> String): Int.() -> String
}

// MODULE: platform()()(common)
// FILE: Java.java
import kotlin.jvm.functions.Function1;

public class Java {
    public Function1<Integer, String> foo(Function1<Integer, String> a) {
        return a;
    };
}

// FILE: platform.kt
actual typealias A = Java

fun box() = A().foo{ "OK" }(1)