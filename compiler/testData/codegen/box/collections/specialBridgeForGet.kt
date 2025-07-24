// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)
// FULL_JDK

abstract class AMap1<K1, V1>(private val m: Map<K1, V1>) : Map<K1, V1> by m

interface Value2

abstract class AMap2<V2 : Value2>(m: Map<String, V2>) : AMap1<String, V2>(m)

class C(val value: String): Value2

class CMap(m: Map<String, C>) : AMap2<C>(m)

fun box(): String {
    val cmap = CMap(mapOf("1" to C("OK")))
    return cmap["1"]!!.value
}
