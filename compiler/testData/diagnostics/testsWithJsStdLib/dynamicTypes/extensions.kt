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

    d.onDynamic()
    d?.onDynamic()

    (d: String).onString()
    (d: Any).onAny()
    (d: Any?).onNullableAny()
    (d: Any).onDynamic()
}

fun Any.onAny() {}
fun Any?.onNullableAny() {}
fun String.onString() {}
fun dynamic.onDynamic() {}

class C {
    fun test(d: dynamic) {
        d.<!DEBUG_INFO_DYNAMIC!>member<!>()
        d.<!DEBUG_INFO_DYNAMIC!>memberExtension<!>()
    }

    fun member() {}
    fun Any.memberExtension() {}
}