// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION

import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.KMutableProperty

object Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "OK"
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}

fun isLight(callable: KProperty<*>): Boolean =
    try {
        callable.parameters
        false
    } catch (e: KotlinReflectionNotSupportedError) {
        // expected
        true
    }

val topVal: String = "OK"
var topVar: String = "OK"
val topDelegatedVal: String by Delegate
var topDelegatedVar: String by Delegate

class A(val memberVal: Int, var memberVar: Int) {
    val memberDelegatedVal: String by Delegate
    var memberDelegatedVar: String by Delegate
}

val String.extensionVal: Int
    get() = length

var String.extensionVar: Int
    get() = length
    set(value) {
    }

fun box(): String {
    val a = A(1, 2)

    if (!isLight(::topVal)) return "Failed for ::topVal"
    if (!isLight(::topVar)) return "Failed for ::topVar"
    if (!isLight(::topDelegatedVal)) return "Failed for ::topDelegatedVal"
    if (!isLight(::topDelegatedVar)) return "Failed for ::topDelegatedVar"

    if (!isLight(A::memberVal)) return "Failed for A::memberVal"
    if (!isLight(A::memberVar)) return "Failed for A::memberVar"
    if (!isLight(A::memberDelegatedVal)) return "Failed for A::memberDelegatedVal"
    if (!isLight(A::memberDelegatedVar)) return "Failed for A::memberDelegatedVar"

    if (!isLight(a::memberVal)) return "Failed for a::memberVal"
    if (!isLight(a::memberVar)) return "Failed for a::memberVar"
    if (!isLight(a::memberDelegatedVal)) return "Failed for a::memberDelegatedVal"
    if (!isLight(a::memberDelegatedVar)) return "Failed for a::memberDelegatedVar"

    if (!isLight(String::extensionVal)) return "Failed for String::extensionVal"
    if (!isLight(String::extensionVar)) return "Failed for String::extensionVar"

    if (!isLight("OK"::extensionVal)) return "Failed for \"OK\"::extensionVal"
    if (!isLight("OK"::extensionVar)) return "Failed for \"OK\"::extensionVar"

    return "OK"
}
