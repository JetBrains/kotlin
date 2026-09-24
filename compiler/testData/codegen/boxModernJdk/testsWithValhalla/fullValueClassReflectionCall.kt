// ISSUE: KT-89949
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

import kotlin.reflect.full.primaryConstructor

value class Full(val a: Int, val b: Int) {
    fun plus(other: Full): Full = Full(a + other.a, b + other.b)
}

value class FullSingle(val a: Int)

@JvmInline
value class KInline(val x: Int)

class WithFull(val f: Full)
class WithFullSingle(val s: FullSingle)

fun mixed(k: KInline, f: Full): Int = k.x + f.a

fun box(): String {
    val full = Full(1, 2)
    val withFull = WithFull::class.primaryConstructor!!
    if (withFull.call(full).f != full) return "Fail: WithFull.call"
    if (withFull.callBy(mapOf(withFull.parameters.single() to full)).f != full) return "Fail: WithFull.callBy"
    if (WithFullSingle::class.primaryConstructor!!.call(FullSingle(3)).s != FullSingle(3)) return "Fail: WithFullSingle"
    if (Full::class.primaryConstructor!!.call(4, 5) != Full(4, 5)) return "Fail: Full"
    if (Full::plus.call(full, full) != Full(2, 4)) return "Fail: plus"
    if (full::plus.call(full) != Full(2, 4)) return "Fail: bound plus"
    if (::mixed.call(KInline(1), full) != 2) return "Fail: mixed"
    return "OK"
}
