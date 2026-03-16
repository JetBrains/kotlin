// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FORCE_STDLIB_ONLY_REFLECTION
// FILE: lib.kt

package lib

import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "OK"
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}

val topVal: String = "OK"
var topVar: String = "OK"
val topDelegatedVal: String by Delegate
var topDelegatedVar: String by Delegate

class A(val memberVal: Int, var memberVar: Int) {
    val memberDelegatedVal: String by Delegate
    var memberDelegatedVar: String by Delegate
}

val sharedA = A(1, 2)
val sharedString = "OK"

val String.extensionVal: Int
    get() = length

var String.extensionVar: Int
    get() = length
    set(value) {
    }

val lightProperties: List<KCallable<*>> = listOf(
    ::topVal,
    ::topVar,
    ::topDelegatedVal,
    ::topDelegatedVar,
    A::memberVal,
    A::memberVar,
    A::memberDelegatedVal,
    A::memberDelegatedVar,
    sharedA::memberVal,
    sharedA::memberVar,
    sharedA::memberDelegatedVal,
    sharedA::memberDelegatedVar,
    String::extensionVal,
    String::extensionVar,
    sharedString::extensionVal,
    sharedString::extensionVar,
)

// MODULE: main(lib)
// FILE: main.kt

import lib.*
import kotlin.reflect.KCallable
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty

val fullProperties: List<KCallable<*>> = listOf(
    ::topVal,
    ::topVar,
    ::topDelegatedVal,
    ::topDelegatedVar,
    A::memberVal,
    A::memberVar,
    A::memberDelegatedVal,
    A::memberDelegatedVar,
    sharedA::memberVal,
    sharedA::memberVar,
    sharedA::memberDelegatedVal,
    sharedA::memberDelegatedVar,
    String::extensionVal,
    String::extensionVar,
    sharedString::extensionVal,
    sharedString::extensionVar,
)

val fullReflectedProperties: List<KCallable<*>?> = listOf(
    (::topVal).javaField!!.kotlinProperty,
    (::topVar).javaField!!.kotlinProperty,
    null, // reflected top-level delegated properties are not accessible from code: (A::memberDelegatedVal, A::memberDelegatedVar)
    null,
    A::class.declaredMemberProperties.single { it.name == "memberVal" },
    A::class.declaredMemberProperties.single { it.name == "memberVar" },
    A::class.declaredMemberProperties.single { it.name == "memberDelegatedVal" },
    A::class.declaredMemberProperties.single { it.name == "memberDelegatedVar" },
    // bound properties and top-level extension properties and not accessible from the code in "reflected" form
    null, // (String::extensionVal - sharedString::extensionVar)
)

fun box(): String {
    for ((light, full) in lightProperties.zip(fullProperties)) {
        if (light != full) return "Failed equals(light, full) for ${full}"
        if (full != light) return "Failed equals(full, light) for ${full}"
        if (light.hashCode() != full.hashCode()) return "Failed light/full hashCode equality for ${full}"
    }
    for ((light, full) in lightProperties.zip(fullReflectedProperties).filter { it.second != null }) {
        if (light != full) return "Failed equals(light, fullReflected) for ${full}"
        if (full != light) return "Failed equals(fullReflected, light) for ${full}"
        if (light.hashCode() != full.hashCode()) return "Failed light/fullReflected hashCode equality for ${full}"
    }
    return "OK"
}
