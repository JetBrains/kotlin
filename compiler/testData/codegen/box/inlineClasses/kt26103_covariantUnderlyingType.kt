// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR, JS_IR

inline class GList<T>(val xs: List<T>)
inline class GSList<T>(val ss: List<String>)
inline class SList(val ss: List<String>)
inline class IList(val ints: List<Int>)
inline class GIList<T>(val ints: List<Int>)

inline class II(val i: Int)
inline class IIList(val iis: List<II>)
inline class GIIList<T>(val iis: List<II>)

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