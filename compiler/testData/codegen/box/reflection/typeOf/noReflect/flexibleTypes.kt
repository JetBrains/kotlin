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
    check("java.lang.String!", returnTypeOf { J.nullabilityFlexible() })
    check("java.util.List<java.lang.String!>", returnTypeOf { J.mutabilityFlexible() })
    check("java.util.List<java.lang.String!>!", returnTypeOf { J.bothFlexible() })

    // Type flexibility affects equality! Platform type is neither nullable nor non-nullable.
    val platform = returnTypeOf { J.nullabilityFlexible() }
    if (platform == returnTypeOf { J.nullable() }) return "Fail: platform type should not be equal to nullable type"
    if (platform == returnTypeOf { J.notNull() }) return "Fail: platform type should not be equal to non-nullable type"

    return "OK"
}

inline fun <reified T> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class J {
    public static String nullabilityFlexible() { return null; }

    @NotNull
    public static List<String> mutabilityFlexible() { return null; }

    public static List<String> bothFlexible() { return null; }

    @Nullable
    public static String nullable() { return null; }

    @NotNull
    public static String notNull() { return null; }
}
