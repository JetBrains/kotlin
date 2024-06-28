// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.test.assertEquals

annotation class Anno(
        val b: Byte,
        val c: Char,
        val d: Double,
        val f: Float,
        val i: Int,
        val j: Long,
        val s: Short,
        val z: Boolean,
        val ba: ByteArray,
        val ca: CharArray,
        val da: DoubleArray,
        val fa: FloatArray,
        val ia: IntArray,
        val ja: LongArray,
        val sa: ShortArray,
        val za: BooleanArray,
        val str: String,
        val stra: Array<String>
)

@Anno(
        1.toByte(),
        'x',
        3.14,
        -2.72f,
        42424242,
        239239239239239L,
        42.toShort(),
        true,
        byteArrayOf((-1).toByte()),
        charArrayOf('y'),
        doubleArrayOf(-3.14159),
        floatArrayOf(2.7218f),
        intArrayOf(424242),
        longArrayOf(239239239239L),
        shortArrayOf((-43).toShort()),
        booleanArrayOf(false, true),
        "lol",
        arrayOf("rofl")
)
fun foo() {}

fun box(): String {
    // Construct an annotation with exactly the same parameters, check that the proxy created by Kotlin and by Java reflection are the same and have the same hash code
    val a1 = Anno::class.constructors.single().call(
        1.toByte(),
        'x',
        3.14,
        -2.72f,
        42424242,
        239239239239239L,
        42.toShort(),
        true,
        byteArrayOf((-1).toByte()),
        charArrayOf('y'),
        doubleArrayOf(-3.14159),
        floatArrayOf(2.7218f),
        intArrayOf(424242),
        longArrayOf(239239239239L),
        shortArrayOf((-43).toShort()),
        booleanArrayOf(false, true),
        "lol",
        arrayOf("rofl")
    )

    val a2 = ::foo.annotations.single() as Anno

    assertEquals(a1, a2)
    assertEquals(a2, a1)
    assertEquals(a1.hashCode(), a2.hashCode())

    assertEquals("@test.Anno(b=1, c=x, d=3.14, f=-2.72, i=42424242, j=239239239239239, s=42, z=true, " +
                 "ba=[-1], ca=[y], da=[-3.14159], fa=[2.7218], ia=[424242], ja=[239239239239], sa=[-43], za=[false, true], " +
                 "str=lol, stra=[rofl])", a1.toString())

    return "OK"
}
