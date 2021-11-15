// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_STDLIB
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun check(expected: String, actual: KType) {
    assertEquals(expected + " (Kotlin reflection is not available)", actual.toString())
}

class C<T, U : Number>

fun box(): String {
    check("(test.C<java.lang.Object?, java.lang.Number>..test.C<*, *>?)", returnTypeOf { J.raw() })
    check("(test.C<java.lang.Object?, java.lang.Number>..test.C<*, *>)", returnTypeOf { J.rawNotNull() })
    check("(java.util.List<java.lang.Object?>..java.util.List<*>?)", returnTypeOf { J.rawList() })
    check("(java.util.Map<java.lang.Object?, java.lang.Object?>..java.util.Map<*, *>)", returnTypeOf { J.rawNotNullMap() })

    return "OK"
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
