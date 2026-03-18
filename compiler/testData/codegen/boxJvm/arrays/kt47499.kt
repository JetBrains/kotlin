// TARGET_BACKEND: JVM
//  In Kotlin/JVM arrays are cloneable
// WITH_STDLIB

import kotlin.test.*

fun <T> T.id() = this

fun Array<String>.test() = id().clone()
fun <T> Array<T>.testGeneric() = id().clone()

fun BooleanArray.test() = id().clone()
fun ByteArray.test() = id().clone()
fun ShortArray.test() = id().clone()
fun IntArray.test() = id().clone()
fun LongArray.test() = id().clone()
fun FloatArray.test() = id().clone()
fun DoubleArray.test() = id().clone()
fun CharArray.test() = id().clone()

fun <T> Array<Array<T>>.testGeneric2D() = id().clone()
fun Array<IntArray>.testInt2D() = id().clone()
fun Array<Array<String>>.testString2D() = id().clone()

fun box(): String {
    val a1 = arrayOf("a", "b", "c")
    val a2 = a1.test()
    assertEquals(a1.toList(), a2.toList())
    val a3 = a1.testGeneric()
    assertEquals(a1.toList(), a3.toList())

    val ba1 = booleanArrayOf(true)
    val ba2 = ba1.test()
    assertEquals(ba1.toList(), ba2.toList())

    val bya1 = byteArrayOf(1, 2, 3)
    val bya2 = bya1.test()
    assertEquals(bya1.toList(), bya2.toList())

    val sa1 = shortArrayOf(1, 2, 3)
    val sa2 = sa1.test()
    assertEquals(sa1.toList(), sa2.toList())

    val ia1 = intArrayOf(1, 3, 5, 7)
    val ia2 = ia1.test()
    assertEquals(ia1.toList(), ia2.toList())

    val la1 = longArrayOf(1, 2, 3, 4)
    val la2 = la1.test()
    assertEquals(la1.toList(), la2.toList())

    val fa1 = floatArrayOf(0.1f, 0.2f, 0.3f)
    val fa2 = fa1.test()
    assertEquals(fa1.toList(), fa2.toList())

    val da1 = doubleArrayOf(0.1, 0.2, 0.3)
    val da2 = da1.test()
    assertEquals(da1.toList(), da2.toList())

    val ca1 = charArrayOf('a', 'b', 'c')
    val ca2 = ca1.test()
    assertEquals(ca1.toList(), ca2.toList())

    val a2a1 = arrayOf(arrayOf(1, 2, 3))
    val a2a2 = a2a1.testGeneric()
    val a2a3 = a2a1.testGeneric2D()
    assertEquals(a2a1.toList(), a2a2.toList())
    assertEquals(a2a1.toList(), a2a3.toList())

    val ia21 = arrayOf(intArrayOf(1, 2, 3))
    val ia22 = ia21.testInt2D()
    assertEquals(ia21.toList(), ia22.toList())

    val sa21 = arrayOf(arrayOf("a", "b", "c"))
    val sa22 = sa21.testString2D()
    assertEquals(sa21.toList(), sa22.toList())

    return "OK"
}
