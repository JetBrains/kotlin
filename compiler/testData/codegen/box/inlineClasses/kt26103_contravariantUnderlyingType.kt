// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GCmp<T>(val xc: Comparable<T>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GSCmp<T>(val sc: Comparable<String>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class SCmp(val sc: Comparable<String>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ICmp(val intc: Comparable<Int>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GICmp<T>(val intc: Comparable<Int>)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class II(val i: Int) : Comparable<II> {
    override fun compareTo(other: II): Int {
        return i.compareTo(other.i)
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IICmp(val iic: Comparable<II>)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class GIICmp<T>(val iic: Comparable<II>)

fun testGCmp(x: GCmp<String>) {
    if (x.xc.compareTo("OK") != 0) throw AssertionError()
}

fun testGSCmp(x: GSCmp<Any>) {
    if (x.sc.compareTo("OK") != 0) throw AssertionError()
}

fun testSCmp(x: SCmp) {
    if (x.sc.compareTo("OK") != 0) throw AssertionError()
}

fun testICmp(x: ICmp) {
    if (x.intc.compareTo(42) != 0) throw AssertionError()
}

fun testGICmp(x: GICmp<Any>) {
    if (x.intc.compareTo(42) != 0) throw AssertionError()
}

fun testIICmp(x: IICmp) {
    if (x.iic.compareTo(II(42)) != 0) throw AssertionError()
}

fun testGIICmp(x: GIICmp<Any>) {
    if (x.iic.compareTo(II(42)) != 0) throw AssertionError()
}

fun box(): String {
    testGCmp(GCmp("OK"))
    testGSCmp(GSCmp<Any>("OK"))
    testSCmp(SCmp("OK"))
    testICmp(ICmp(42))
    testGICmp(GICmp<Any>(42))
    testIICmp(IICmp(II(42)))
    testGIICmp(GIICmp<Any>(II(42)))

    return "OK"
}