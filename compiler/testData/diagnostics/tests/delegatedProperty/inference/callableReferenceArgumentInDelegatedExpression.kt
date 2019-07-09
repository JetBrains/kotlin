// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class Inv2<T, K>

fun <T, K> create(g: (T) -> K): Inv2<T, K> = TODO()

operator fun <S1, V1> Inv2<S1, V1>.getValue(o: Sample, desc: KProperty<*>): V1 = TODO()
operator fun <S2, V2> Inv2<S2, V2>.setValue(o: Sample, desc: KProperty<*>, value: V2) {}

class Version(val version: Int)

class Sample {
    var version: Version by create(::Version)
}