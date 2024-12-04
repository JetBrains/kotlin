// LANGUAGE: +KotlinFunInterfaceConstructorReference

// WITH_STDLIB
// TARGET_BACKEND: JVM

// DONT_TARGET_EXACT_BACKEND: JVM
//  ^ old JVM BE generates bogus code

// FILE: funInterfaceConstructorThrowsNpe.kt

fun interface KSupplier<T> {
    fun get(): T
}

val ks: (() -> String) -> KSupplier<String> =
    ::KSupplier

fun box(): String {
    try {
        ks(J.fn)
        return "ks(null) should throw NPE"
    } catch (e: NullPointerException) {
        return "OK"
    }
}

// FILE: J.java
import kotlin.jvm.functions.Function0;

public class J {
    public static Function0<String> fn = null;
}
