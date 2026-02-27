// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FORCE_STDLIB_ONLY_REFLECTION
// FILE: lib.kt

package lib

class A

val lightClasses = listOf(A::class, Any::class, Nothing::class, Unit::class, List::class, MutableList::class, Int::class, String::class)

// MODULE: main(lib)
// FILE: main.kt

import lib.*

val fullClasses = listOf(A::class, Any::class, Nothing::class, Unit::class, List::class, MutableList::class, Int::class, String::class)

fun box(): String {
    for ((light, full) in lightClasses.zip(fullClasses)) {
        if (light != full) return "Failed equals(light, full) for ${full}"
        if (full != light) return "Failed equals(full, light) for ${full}"
    }
    return "OK"
}