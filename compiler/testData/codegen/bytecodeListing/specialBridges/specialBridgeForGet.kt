// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57301 K2: `getOrDefault` and bridges are not generated for certain Map subclasses

abstract class AMap1<K1, V1>(private val m: Map<K1, V1>) : Map<K1, V1> by m

interface Value2

abstract class AMap2<V2 : Value2>(m: Map<String, V2>) : AMap1<String, V2>(m)

class C(val value: String): Value2

class Map3(m: Map<String, C>) : AMap2<C>(m)
