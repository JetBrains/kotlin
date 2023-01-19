// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +ValueClasses

@JvmInline
value class IcInt(val x: Int)

@JvmInline
value class Point(val x: Int, val y: Int)

@JvmInline
value class IcPoint(val x: Point)

@JvmInline
value class IcIcPoint(val x: IcPoint)

inline fun <reified T> singletonVArray(x: T) {
    val x = Array(1) { x }
}

// OK:

fun fooInt(a: VArray<Int>) {}
fun fooUByte(a: VArray<UByte>) {}
fun fooUByteN(a: VArray<UByte?>) {}
fun fooStr(a: VArray<String>) {}
fun fooInInt(a: VArray<in Int>) {}
fun fooInInt(a: VArray<out UByte>) {}
inline fun <reified T> fooReifiedT(a: VArray<T>) {}
inline fun <reified T> fooReifiedInT(a: VArray<in T>) {}
inline fun <reified T> fooReifiedOutT(a: VArray<out T>) {}
inline fun <reified T : Number> fooReifiedTNumber(a: VArray<T>) {}
inline fun <reified T : Number> fooReifiedTNumberN(a: Array<T?>) {}

// Error:

fun <T> barT(a: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>) {}

fun barStar(a: VArray<<!ILLEGAL_PROJECTION_USAGE!>*<!>>) {}

class A<T>(val x: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>)

fun barMFVC(a: VArray<Point>) {}

fun barMFVCInferred() {
    val x = Point(1, 2)
    singletonVArray(x)
}

fun barIcIcMFVCInferred() {
    val x = IcIcPoint(IcPoint(Point(0, 1)))
    singletonVArray(x)
}

