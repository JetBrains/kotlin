// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FORCE_STDLIB_ONLY_REFLECTION

// FILE: J.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class J {
    public static String nullabilityFlexible() { return null; }

    @NotNull
    public static List<String> mutabilityFlexible() { return null; }

    public static List<String> bothFlexible() { return null; }
}

// FILE: lib.kt

package lib

import kotlin.reflect.typeOf

open class A

inline fun <reified T> returnTypeOf(block: () -> T) =
    typeOf<T>()

val lightTypes = listOf(
    // simple
    typeOf<A>(),
    typeOf<Any>(),
    typeOf<List<Nothing>>().arguments[0].type!!,
    typeOf<Unit>(),
    typeOf<List<String>>(),
    typeOf<MutableList<String>>(),
    typeOf<Int>(),
    typeOf<String>(),
    // nullable
    typeOf<A?>(),
    typeOf<Any>(),
    typeOf<List<Nothing?>>().arguments[0].type!!,
    typeOf<Unit?>(),
    typeOf<List<String?>?>(),
    typeOf<MutableList<String?>?>(),
    typeOf<Int?>(),
    typeOf<String?>(),
    // variance
    typeOf<List<out A>>(),
    typeOf<List<*>>(),
    typeOf<MutableList<in A>>(),
    typeOf<MutableList<out A>>(),
    typeOf<MutableList<*>>(),
    // flexible
    returnTypeOf { J.nullabilityFlexible() },
    returnTypeOf { J.mutabilityFlexible() },
    returnTypeOf { J.bothFlexible() })


// MODULE: main(lib)
// FILE: main.kt

import lib.*
import kotlin.reflect.typeOf

val fullTypes = listOf(
    // simple
    typeOf<A>(),
    typeOf<Any>(),
    typeOf<List<Nothing>>().arguments[0].type!!,
    typeOf<Unit>(),
    typeOf<List<String>>(),
    typeOf<MutableList<String>>(),
    typeOf<Int>(),
    typeOf<String>(),
    // nullable
    typeOf<A?>(),
    typeOf<Any>(),
    typeOf<List<Nothing?>>().arguments[0].type!!,
    typeOf<Unit?>(),
    typeOf<List<String?>?>(),
    typeOf<MutableList<String?>?>(),
    typeOf<Int?>(),
    typeOf<String?>(),
    // variance
    typeOf<List<out A>>(),
    typeOf<List<*>>(),
    typeOf<MutableList<in A>>(),
    typeOf<MutableList<out A>>(),
    typeOf<MutableList<*>>(),
    // flexible
    returnTypeOf { J.nullabilityFlexible() },
    returnTypeOf { J.mutabilityFlexible() },
    returnTypeOf { J.bothFlexible() })

fun box(): String {
    for ((light, full) in lightTypes.zip(fullTypes)) {
        if (light != full) return "Failed equals(light, full) for ${full}"
        if (full != light) return "Failed equals(full, light) for ${full}"
        if (light.hashCode() != full.hashCode()) return "Failed light/full hashCode equality for ${full}"
    }
    return "OK"
}
