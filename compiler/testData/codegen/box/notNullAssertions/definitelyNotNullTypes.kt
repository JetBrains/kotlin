// LANGUAGE: +DefinitelyNonNullableTypes +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// TARGET_BACKEND: JVM
// FILE: JClass.java

import org.jetbrains.annotations.*;

public abstract class JClass<T> {
    public void foo(@NotNull T x) {}

    public static void test(JClass<String> w) {
        w.foo(null);
    }
}

// FILE: main.kt
class KDerived<E> : JClass<E>() {
    override fun foo(e: E & Any) {
        throw RuntimeException("Should not be called")
    }
}

fun box(): String {
    try {
        JClass.test(KDerived())
    } catch (e: java.lang.NullPointerException) {
        return "OK"
    }
    return "fail"
}
