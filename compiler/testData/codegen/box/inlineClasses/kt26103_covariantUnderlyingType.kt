// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GList<T>(val xs: List<T>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GSList<T>(val ss: List<String>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class SList(val ss: List<String>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IList(val ints: List<Int>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GIList<T>(val ints: List<Int>)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class II(val i: Int)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IIList(val iis: List<II>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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