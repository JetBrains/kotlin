// TARGET_BACKEND: JVM
// LANGUAGE_VERSION: 1.0
// WITH_RUNTIME
// FULL_JDK
class A(val x: List<String>) : List<String> by x

fun box(): String {
    return A(listOf("OK"))[0]
}
