// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C<T, U : Number>

fun box(): String {
    check("(test.C<kotlin.Any?, kotlin.Number>..test.C<*, *>?)", returnTypeOf { J.raw() })
    check("(test.C<kotlin.Any?, kotlin.Number>..test.C<*, *>)", returnTypeOf { J.rawNotNull() })
    check("(kotlin.collections.MutableList<kotlin.Any?>..kotlin.collections.List<*>?)", returnTypeOf { J.rawList() })
    check("(kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>..kotlin.collections.Map<*, *>)", returnTypeOf { J.rawNotNullMap() })

    return "OK"
}

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

inline fun <reified T : Any> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

import test.C;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;

public class J {
    public static C raw() {
        return null;
    }

    @NotNull
    public static C rawNotNull() {
        return null;
    }

    public static List rawList() {
        return null;
    }

    @NotNull
    public static Map rawNotNullMap() {
        return null;
    }
}
