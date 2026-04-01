// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

fun box(): String {
    check("kotlin.Int", returnTypeOf { J.primitive() })
    check("kotlin.Int!", returnTypeOf { J.wrapper() })
    check("kotlin.Int?", returnTypeOf { J.nullableWrapper() })
    check("kotlin.Int", returnTypeOf { J.notNullWrapper() })

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
