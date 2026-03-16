// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION

import kotlin.reflect.KFunction

fun isLight(klass: KFunction<*>): Boolean =
    try {
        klass.parameters
        false
    } catch (e: KotlinReflectionNotSupportedError) {
        // expected
        true
    }

fun top() {}
suspend fun topSuspend() {}

class A(val value1: Int) {
    fun member(): Int = value1

    fun getLocalRef(value2: Int): KFunction<*> {
        fun local(): Int = value1 * value2
        return ::local
    }
}

fun String.extensionFunc() = length

fun box(): String {
    val a = A(1)
    if (!isLight(::top)) return "Failed for ::top"
    if (!isLight(::topSuspend)) return "Failed for ::topSuspend"
    if (!isLight(::A)) return "Failed for ::A"
    if (!isLight(A::member)) return "Failed for A::member"
    if (!isLight(a::member)) return "Failed for a::member"
//    if (!isLight(a.getLocalRef(2))) return "Failed for a.getLocalRef(2)" // KT-64873
    if (!isLight(String::extensionFunc)) return "Failed for String::extensionFunc"
    if (!isLight("OK"::extensionFunc)) return "Failed for \"OK\"::extensionFunc"

    return "OK"
}