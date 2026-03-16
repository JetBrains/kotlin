// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FORCE_STDLIB_ONLY_REFLECTION
// FILE: lib.kt

package lib

import kotlin.reflect.KFunction

fun top() {}
suspend fun topSuspend() {}

class A(val value: Int) {
    fun member(): Int = value
}

val sharedA = A(1)
val sharedString = "OK"

fun String.extensionFunc() = length

val lightFunctions: List<KFunction<*>> = listOf(
    ::top,
    ::topSuspend,
    ::A,
    A::member,
    sharedA::member,
    String::extensionFunc,
    sharedString::extensionFunc,
)

// MODULE: main(lib)
// FILE: main.kt

import lib.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.kotlinFunction

val fullFunctions: List<KFunction<*>> = listOf(
    ::top,
    ::topSuspend,
    ::A,
    A::member,
    sharedA::member,
    String::extensionFunc,
    sharedString::extensionFunc,
)

val libClassTopReflectedFunctions = Class.forName("lib.LibKt").declaredMethods.mapNotNull { it.kotlinFunction }
val fullReflectedFunctions: List<KFunction<*>?> = listOf(
    libClassTopReflectedFunctions.single { it.name == "top" },
    libClassTopReflectedFunctions.single { it.name == "topSuspend" },
    A::class.constructors.single(),
    A::class.declaredFunctions.single(),
    null, // bound reflected functions are not accessible from the code
    libClassTopReflectedFunctions.single { it.name == "extensionFunc" },
    null, // bound reflected functions are not accessible from the code
)

fun box(): String {
    for ((light, full) in lightFunctions.zip(fullFunctions)) {
        if (light != full) return "Failed equals(light, full) for ${full}"
        if (full != light) return "Failed equals(full, light) for ${full}"
        if (light.hashCode() != full.hashCode()) return "Failed light/full hashCode equality for ${full}"
    }
    for ((light, full) in lightFunctions.zip(fullReflectedFunctions).filter { it.second != null }) {
        if (light != full) return "Failed equals(light, fullReflected) for ${full}"
        if (full != light) return "Failed equals(fullReflected, light) for ${full}"
        if (light.hashCode() != full.hashCode()) return "Failed light/fullReflected hashCode equality for ${full}"
    }
    return "OK"
}
