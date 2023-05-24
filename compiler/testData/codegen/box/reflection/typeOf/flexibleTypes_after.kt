// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun box(): String {
    val v1 = returnTypeOf { J.nullabilityFlexible() }.toString()
    if (v1 != "kotlin.String!") return "Fail 1: $v1"

    val v2 = returnTypeOf { J.mutabilityFlexible() }.toString()
    if (v2 != "kotlin.collections.(Mutable)List<kotlin.String!>") return "Fail 2: $v2"

    val v3 = returnTypeOf { J.bothFlexible() }.toString()
    if (v3 != "kotlin.collections.(Mutable)List<kotlin.String!>!") return "Fail 3: $v3"

    return "OK"
}

inline fun <reified T : Any> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

import org.jetbrains.annotations.NotNull;
import java.util.List;

public class J {
    public static String nullabilityFlexible() {
        return null;
    }

    @NotNull
    public static List<String> mutabilityFlexible() {
        return null;
    }

    public static List<String> bothFlexible() {
        return null;
    }
}
