// API_VERSION: 1.5
// OPT_IN: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class C<T, U : Number>

fun box(): String {
    val v1 = returnTypeOf { J.raw() }.toString()
    if (v1 != "test.C<kotlin.Any?, kotlin.Number>") return "Fail 1: $v1"

    val v2 = returnTypeOf { J.rawNotNull() }.toString()
    if (v2 != "test.C<kotlin.Any?, kotlin.Number>") return "Fail 2: $v2"

    val v3 = returnTypeOf { J.rawList() }.toString()
    if (v3 != "kotlin.collections.List<kotlin.Any?>") return "Fail 3: $v3"

    val v4 = returnTypeOf { J.rawNotNullMap() }.toString()
    if (v4 != "kotlin.collections.Map<kotlin.Any?, kotlin.Any?>") return "Fail 4: $v4"

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
