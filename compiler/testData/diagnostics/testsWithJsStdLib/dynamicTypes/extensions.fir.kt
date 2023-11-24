// !DIAGNOSTICS:-USELESS_CAST
// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d.<!DEBUG_INFO_DYNAMIC!>onAny<!>()
    d?.<!DEBUG_INFO_DYNAMIC!>onAny<!>()
    run {
        d!!.<!DEBUG_INFO_DYNAMIC!>onAny<!>()
    }

    d.<!DEBUG_INFO_DYNAMIC!>onAny<!>(1)

    d.<!DEBUG_INFO_DYNAMIC!>onNullableAny<!>()
    d.<!DEBUG_INFO_DYNAMIC!>onString<!>()

    d.<!DEBUG_INFO_DYNAMIC!>onDynamic<!>()
    d?.<!DEBUG_INFO_DYNAMIC!>onDynamic<!>()

    (d as String).onString()
    (d as Any).onAny()
    (d as Any?).onNullableAny()
    (d as Any).<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamic<!>()
}

fun Any.onAny() {}
fun Any?.onNullableAny() {}
fun String.onString() {}
fun <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.onDynamic() {}

class C {
    fun test(d: dynamic) {
        d.<!DEBUG_INFO_DYNAMIC!>member<!>()
        d.<!DEBUG_INFO_DYNAMIC!>memberExtension<!>()
    }

    fun member() {}
    fun Any.memberExtension() {}
}
