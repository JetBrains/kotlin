//KT-2997 Automatically cast error (Array)

fun foo(a: Any): Int {
    if (a is IntArray) {
        a.get(0)
        a.set(0, 1)
        a.iterator()
        return a.size
    }
    if (a is ShortArray) {
        a.get(0)
        a.set(0, 1)
        a.iterator()
        return a.size
    }
    if (a is ByteArray) {
        a.get(0)
        a.set(0, 1)
        a.iterator()
        return a.size
    }
    if (a is FloatArray) {
        a.get(0)
        a.set(0, 1.toFloat())
        a.iterator()
        return a.size
    }
    if (a is DoubleArray) {
        a.get(0)
        a.set(0, 1.0)
        a.iterator()
        return a.size
    }
    if (a is BooleanArray) {
        a.get(0)
        a.set(0, false)
        a.iterator()
        return a.size
    }
    if (a is CharArray) {
        a.get(0)
        a.set(0, 'a')
        a.iterator()
        return a.size
    }
    if (a is Array<*>) {
        if (a.size > 0) a.get(0)
        a.iterator()
        return a.size
    }

    return 0
}

fun box(): String {
    // Only run this test if primitive array `is` checks work (KT-17137)
    if ((intArrayOf() as Any) is Array<*>) return "OK"

    val iA = IntArray(1)
    if (foo(iA) != 1) return "fail int[]"
    val sA = ShortArray(1)
    if (foo(sA) != 1) return "fail short[]"
    val bA = ByteArray(1)
    if (foo(bA) != 1) return "fail byte[]"
    val fA = FloatArray(1)
    if (foo(fA) != 1) return "fail float[]"
    val dA = DoubleArray(1)
    if (foo(dA) != 1) return "fail double[]"
    val boolA = BooleanArray(1)
    if (foo(boolA) != 1) return "fail boolean[]"
    val cA = CharArray(1)
    if (foo(cA) != 1) return "fail char[]"
    val oA = arrayOfNulls<Int>(1)
    if (foo(oA) != 1) return "fail Any[]"

    val sArray = arrayOfNulls<String>(0)
    if (foo(sArray) != 0) return "fail String[]"

    return "OK"
}
