// IGNORE_BACKEND: JVM, JS, JS_IR
// KT-30080

data class A(
    val z: BooleanArray,
    var c: CharArray,
    val b: ByteArray,
    val s: ShortArray,
    val i: IntArray,
    val f: FloatArray,
    val j: LongArray,
    val d: DoubleArray,
)

fun box(): String {
    val a = A(
        booleanArrayOf(true),
        charArrayOf('a'),
        byteArrayOf(1),
        shortArrayOf(2),
        intArrayOf(3),
        floatArrayOf(4f),
        longArrayOf(5),
        doubleArrayOf(6.0),
    )
    return if (a.toString() == "A(z=[true], c=[a], b=[1], s=[2], i=[3], f=[4.0], j=[5], d=[6.0])")
        "OK"
    else "Fail: $a"
}
