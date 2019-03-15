// !LANGUAGE: +NormalizeConstructorCalls
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
open class A(val s: String)

inline fun test(crossinline z: () -> String): String {
    return object : A(listOf(1).map { it.toString() }.joinToString()) {
        val value = z()
    }.value
}

fun box(): String {
    return test { "OK" }
}