// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun check(expected: String, actual: KType) {
    assertEquals(expected + " (Kotlin reflection is not available)", actual.toString())
}

fun box(): String {
    check("int", returnTypeOf { J.primitive() })
    check("java.lang.Integer!", returnTypeOf { J.wrapper() })
    check("java.lang.Integer?", returnTypeOf { J.nullableWrapper() })
    check("java.lang.Integer", returnTypeOf { J.notNullWrapper() })

    return "OK"
}

inline fun <reified T> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class J {
    public static int primitive() { return 0; }
    public static Integer wrapper() { return 0; }

    @Nullable
    public static Integer nullableWrapper() { return 0; }

    @NotNull
    public static Integer notNullWrapper() { return 0; }
}
