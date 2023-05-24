// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class C<T, U : Number>

fun box(): String {
    val v1 = returnTypeOf { J.raw() }.toString()
    if (v1 != "(test.C<kotlin.Any?, kotlin.Number>..test.C<*, *>?)") return "Fail 1: $v1"

    val v2 = returnTypeOf { J.rawNotNull() }.toString()
    if (v2 != "(test.C<kotlin.Any?, kotlin.Number>..test.C<*, *>)") return "Fail 2: $v2"

    val v3 = returnTypeOf { J.rawList() }.toString()
    if (v3 != "(kotlin.collections.MutableList<kotlin.Any?>..kotlin.collections.List<*>?)") return "Fail 3: $v3"

    val v4 = returnTypeOf { J.rawNotNullMap() }.toString()
    if (v4 != "(kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>..kotlin.collections.Map<*, *>)") return "Fail 4: $v4"

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
