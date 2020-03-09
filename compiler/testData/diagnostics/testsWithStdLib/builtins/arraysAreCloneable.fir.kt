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

    arrayOf("").clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<String>>() }
    intArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><IntArray>() }
    longArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><LongArray>() }
    shortArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><ShortArray>() }
    byteArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><ByteArray>() }
    charArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><CharArray>() }
    doubleArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><DoubleArray>() }
    floatArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><FloatArray>() }
    booleanArrayOf().clone() checkType { <!UNRESOLVED_REFERENCE!>_<!><BooleanArray>() }
}
