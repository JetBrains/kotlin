// TARGET_BACKEND: JVM
// WITH_RUNTIME
// CONSTRUCTOR_CALL_NORMALIZATION_MODE: enable
open class A(val s: String)

inline fun test(crossinline z: () -> String): String {
    return object : A(listOf(1).map { it.toString() }.joinToString()) {
        val value = z()
    }.value
}

fun box(): String {
    return test { "OK" }
}
