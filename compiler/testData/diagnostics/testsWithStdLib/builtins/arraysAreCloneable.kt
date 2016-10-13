// !CHECK_TYPE

fun foo(x: Cloneable) = x

fun test() {
    foo(arrayOf(""))
    foo(intArrayOf())
    foo(longArrayOf())
    foo(shortArrayOf())
    foo(byteArrayOf())
    foo(charArrayOf())
    foo(doubleArrayOf())
    foo(floatArrayOf())
    foo(booleanArrayOf())

    arrayOf("").clone() checkType { _<Array<String>>() }
    intArrayOf().clone() checkType { _<IntArray>() }
    longArrayOf().clone() checkType { _<LongArray>() }
    shortArrayOf().clone() checkType { _<ShortArray>() }
    byteArrayOf().clone() checkType { _<ByteArray>() }
    charArrayOf().clone() checkType { _<CharArray>() }
    doubleArrayOf().clone() checkType { _<DoubleArray>() }
    floatArrayOf().clone() checkType { _<FloatArray>() }
    booleanArrayOf().clone() checkType { _<BooleanArray>() }
}
