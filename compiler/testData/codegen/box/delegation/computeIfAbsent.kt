// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)
// FULL_JDK

class MyMap : MutableMap<Int, String> by hashMapOf()

fun box(): String {
    val map = MyMap()
    return map.computeIfAbsent(42) { "OK" }
}
