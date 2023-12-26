// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-63828


abstract class AMap1<K1, V1>(private val m: Map<K1, V1>) : Map<K1, V1> by m

interface Value2

abstract class AMap2<V2 : Value2>(m: Map<String, V2>) : AMap1<String, V2>(m)

class C(val value: String): Value2

class Map3(m: Map<String, C>) : AMap2<C>(m)
