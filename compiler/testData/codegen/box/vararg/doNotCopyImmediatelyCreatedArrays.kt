// IGNORE_BACKEND_FIR: JVM_IR
fun booleanVararg(vararg xs: Boolean) {
    if (xs.size != 1 && xs[0] != true) throw AssertionError()
}

fun byteVararg(vararg xs: Byte) {
    if (xs.size != 1 && xs[0] != 1.toByte()) throw AssertionError()
}

fun shortVararg(vararg xs: Short) {
    if (xs.size != 1 && xs[0] != 1.toShort()) throw AssertionError()
}

fun intVararg(vararg xs: Int) {
    if (xs.size != 1 && xs[0] != 1) throw AssertionError()
}

fun longVararg(vararg xs: Long) {
    if (xs.size != 1 && xs[0] != 1L) throw AssertionError()
}

fun floatVararg(vararg xs: Float) {
    if (xs.size != 1 && xs[0] != 1.0f) throw AssertionError()
}

fun doubleVararg(vararg xs: Double) {
    if (xs.size != 1 && xs[0] != 1.0) throw AssertionError()
}

fun anyVararg(vararg xs: Any?) {
    if (xs.size != 1 && (xs[0] != 1 && xs[0] != null)) throw AssertionError()
}

fun <T> genericVararg(vararg xs: T) {
    if (xs.size != 1 && (xs[0] != 1 && xs[0] != null)) throw AssertionError()
}

fun box(): String {
    booleanVararg(*booleanArrayOf(true))
    booleanVararg(*BooleanArray(1))
    booleanVararg(*BooleanArray(1) { true })
    booleanVararg(xs = *booleanArrayOf(true))
    booleanVararg(xs = *BooleanArray(1))
    booleanVararg(xs = *BooleanArray(1) { true })

    byteVararg(*byteArrayOf(1))
    byteVararg(*ByteArray(1))
    byteVararg(*ByteArray(1) { 1 })
    byteVararg(xs = *byteArrayOf(1))
    byteVararg(xs = *ByteArray(1))
    byteVararg(xs = *ByteArray(1) { 1 })

    shortVararg(*shortArrayOf(1))
    shortVararg(*ShortArray(1))
    shortVararg(*ShortArray(1) { 1 })
    shortVararg(xs = *shortArrayOf(1))
    shortVararg(xs = *ShortArray(1))
    shortVararg(xs = *ShortArray(1) { 1 })

    intVararg(*intArrayOf(1))
    intVararg(*IntArray(1))
    intVararg(*IntArray(1) { 1 })
    intVararg(xs = *intArrayOf(1))
    intVararg(xs = *IntArray(1))
    intVararg(xs = *IntArray(1) { 1 })

    longVararg(*longArrayOf(1L))
    longVararg(*LongArray(1))
    longVararg(*LongArray(1) { 1L })
    longVararg(xs = *longArrayOf(1L))
    longVararg(xs = *LongArray(1))
    longVararg(xs = *LongArray(1) { 1L })

    floatVararg(*floatArrayOf(1.0f))
    floatVararg(*FloatArray(1))
    floatVararg(*FloatArray(1) { 1.0f })
    floatVararg(xs = *floatArrayOf(1.0f))
    floatVararg(xs = *FloatArray(1))
    floatVararg(xs = *FloatArray(1) { 1.0f })

    doubleVararg(*doubleArrayOf(1.0))
    doubleVararg(*DoubleArray(1))
    doubleVararg(*DoubleArray(1) { 1.0 })
    doubleVararg(xs = *doubleArrayOf(1.0))
    doubleVararg(xs = *DoubleArray(1))
    doubleVararg(xs = *DoubleArray(1) { 1.0 })

    anyVararg(*arrayOf(1))
    anyVararg(*Array(1) { 1 })
    anyVararg(*arrayOfNulls(1))
    anyVararg(xs = *arrayOf(1))
    anyVararg(xs = *Array(1) { 1 })
    anyVararg(xs = *arrayOfNulls(1))

    genericVararg(*arrayOf(1))
    genericVararg(*Array(1) { 1 })
    genericVararg(*arrayOfNulls<Int>(1))
    genericVararg(xs = *arrayOf(1))
    genericVararg(xs = *Array(1) { 1 })
    genericVararg(xs = *arrayOfNulls<Int>(1))

    return "OK"
}