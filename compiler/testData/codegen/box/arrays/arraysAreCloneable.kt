// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

fun foo(x: Cloneable) = x

fun box(): String {
    foo(arrayOf(""))
    foo(intArrayOf())
    foo(longArrayOf())
    foo(shortArrayOf())
    foo(byteArrayOf())
    foo(charArrayOf())
    foo(doubleArrayOf())
    foo(floatArrayOf())
    foo(booleanArrayOf())

    arrayOf("").clone()
    intArrayOf().clone()
    longArrayOf().clone()
    shortArrayOf().clone()
    byteArrayOf().clone()
    charArrayOf().clone()
    doubleArrayOf().clone()
    floatArrayOf().clone()
    booleanArrayOf().clone()

    return "OK"
}
