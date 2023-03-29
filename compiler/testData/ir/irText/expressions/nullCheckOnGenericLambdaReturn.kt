// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// FILE: nullCheckOnGenericLambdaReturn.kt
fun checkAny(fn: () -> Any) = fn()

fun checkAnyN(fn: () -> Any?) = fn()

fun <T> checkT(fn: () -> T) = fn()

fun <T : Any> checkTAny(fn: () -> T) = fn()

fun <T> id(x: T) = x

fun test1() = checkT { J.foo() }

fun test2() = checkT { J.nnFoo() }

fun test3() = checkTAny { J.foo() }

fun test4() = checkTAny { J.nnFoo() }

// FILE: J.java
import org.jetbrains.annotations.*;

public class J {
    public static String foo() { return null; }

    public static @NotNull String nnFoo() { return null; }
}
