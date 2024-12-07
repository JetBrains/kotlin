// TARGET_BACKEND: JVM
// NO_CHECK_SOURCE_VS_BINARY
//^ While compiling source with K1, we do not store annotation default values, but we load them when reading compiled files both in K1 and K2
// This test verifies exactly loading of default values

package test

import kotlin.reflect.KClass
enum class E { E0 }
annotation class Empty

annotation class A(
    val i: Int = 42,
    val s: String = "foo",
    val kClass: KClass<*> = Int::class,
    val kClassArray: Array<KClass<*>> = [A::class],
    val e: E = E.E0,
    val anno: Empty = Empty(),
    val aS: Array<String> = arrayOf("a", "b"),
    val aI: IntArray = intArrayOf(1, 2)
)

annotation class OtherArrays(
    val doublesArray: DoubleArray = [1.5],
    val enumArray: Array<kotlin.text.RegexOption> = [kotlin.text.RegexOption.IGNORE_CASE],
    val annotationsArray: Array<JvmStatic> = [],
    val namesArray: Array<JvmName> = [JvmName("foo")]
)

annotation class UnsignedValue(
    val uint: UInt = 2147483657U // Int.MAX_VALUE + 10
)
