// WITH_REFLECT
// TARGET_BACKEND: JVM

// KT-89068: exposing a secondary constructor must not change what Kotlin reflection sees.

@file:OptIn(ExperimentalStdlibApi::class)

package test

import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

@JvmInline
value class Exposed(val a: UInt) {
    @JvmExposeBoxed
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

@JvmInline
value class NonExposed(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

fun box(): String {
    // Same constructors as the non-exposed counterpart.
    if (Exposed::class.constructors.size != 2) return "FAIL 1: ${Exposed::class.constructors}"
    if (Exposed::class.constructors.size != NonExposed::class.constructors.size) {
        return "FAIL 2: ${Exposed::class.constructors} vs ${NonExposed::class.constructors}"
    }

    // Value class constructors keep having no Java counterpart, exposed or not.
    val primaryJava = Exposed::class.primaryConstructor!!.javaConstructor
    if (primaryJava != null) return "FAIL 3: $primaryJava"

    val secondary = Exposed::class.constructors.single { it.parameters.size == 2 }
    if (secondary.javaConstructor != null) return "FAIL 4: ${secondary.javaConstructor}"

    val nonExposedSecondary = NonExposed::class.constructors.single { it.parameters.size == 2 }
    if (nonExposedSecondary.javaConstructor != null) return "FAIL 5: ${nonExposedSecondary.javaConstructor}"

    // ... and both are still callable through Kotlin reflection.
    val fromSecondary = secondary.call(42u, "OK")
    if (fromSecondary.a != 44u) return "FAIL 6: ${fromSecondary.a}"

    val fromPrimary = Exposed::class.primaryConstructor!!.call(1u)
    if (fromPrimary.a != 1u) return "FAIL 7: ${fromPrimary.a}"

    return "OK"
}
