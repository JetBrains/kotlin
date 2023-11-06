// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

class MyMap : MutableMap<Int, String> by hashMapOf()

fun box(): String {
    val map = MyMap()
    return map.computeIfAbsent(42) { "OK" }
}
