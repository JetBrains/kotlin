// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-63828

class MyMap : MutableMap<Int, String> by hashMapOf()

fun box(): String {
    val map = MyMap()
    return map.computeIfAbsent(42) { "OK" }
}
