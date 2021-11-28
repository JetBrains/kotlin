// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB

class A<T>(val b: T)

fun box(): String {
    val test = A(1).b::javaClass.get().simpleName
    if (test != "Integer") throw Exception("test: $test")

    return "OK"
}