// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> areCopies(x: VArray<T>, y: VArray<T>): Boolean {
    if (x === y) return false
    if (x.size != y.size) return false
    for (i in 0 until x.size) if (x[i] != y[i]) return false
    return true
}

@JvmInline
value class IcInt(val x: Int)

inline fun <reified T> testArray(array: VArray<T>) = areCopies(array, array.clone())

fun box(): String {

    if (!testArray(VArray<Boolean>(2) { false })) return "Fail 1"
    if (!testArray(VArray<Byte>(2) { 0.toByte() })) return "Fail 2"
    if (!testArray(VArray<Short>(2) { 1.toShort() })) return "Fail 3"
    if (!testArray(VArray<Int>(2) { 2 })) return "Fail 4"
    if (!testArray(VArray<Long>(2) { 3.toLong() })) return "Fail 5"
    if (!testArray(VArray<Float>(2) { 4.toFloat() })) return "Fail 6"
    if (!testArray(VArray<Double>(2) { 5.toDouble() })) return "Fail 7"
    if (!testArray(VArray<Char>(2) { 'a' })) return "Fail 8"

    if (!testArray(VArray<UByte>(2) { 0.toUByte() })) return "Fail 9"
    if (!testArray(VArray<UShort>(2) { 1.toUShort() })) return "Fail 10"
    if (!testArray(VArray<UInt>(2) { 2.toUInt() })) return "Fail 11"
    if (!testArray(VArray<ULong>(2) { 3.toULong() })) return "Fail 12"

    if (!testArray(VArray(2) { IcInt(it) })) return "Fail 13"
    if (!testArray(VArray(2) { "aba" })) return "Fail 14"

    return "OK"
}