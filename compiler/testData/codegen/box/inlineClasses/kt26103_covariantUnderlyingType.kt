// WITH_RUNTIME

@JvmInline
value class GList<T>(val xs: List<T>)
@JvmInline
value class GSList<T>(val ss: List<String>)
@JvmInline
value class SList(val ss: List<String>)
@JvmInline
value class IList(val ints: List<Int>)
@JvmInline
value class GIList<T>(val ints: List<Int>)

@JvmInline
value class II(val i: Int)
@JvmInline
value class IIList(val iis: List<II>)
@JvmInline
value class GIIList<T>(val iis: List<II>)

fun testGList(gl: GList<String>) {
    if (gl.xs[0] != "OK") throw AssertionError()
}

fun testGSList(sl: GSList<String>) {
    if (sl.ss[0] != "OK") throw AssertionError()
}

fun testSList(sl: SList) {
    if (sl.ss[0] != "OK") throw AssertionError()
}

fun testIList(il: IList) {
    if (il.ints[0] != 42) throw AssertionError()
}

fun testGIList(gil: GIList<Any>) {
    if (gil.ints[0] != 42) throw AssertionError()
}

fun testIIList(iil: IIList) {
    if (iil.iis[0].i != 42) throw AssertionError()
}

fun testGIIList(giil: GIIList<Any>) {
    if (giil.iis[0].i != 42) throw AssertionError()
}

fun box(): String {
    testGList(GList(listOf("OK")))
    testGSList(GSList(listOf("OK")))
    testSList(SList(listOf("OK")))
    testIList(IList(listOf(42)))
    testGIList(GIList<Any>(listOf(42)))
    testIIList(IIList(listOf(II(42))))
    testGIIList(GIIList<Any>(listOf(II(42))))

    return "OK"
}