// !DIAGNOSTICS:-USELESS_CAST
// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d.onAny()
    d?.onAny()
    run {
        d!!.onAny()
    }

    d.onAny(1)

    d.onNullableAny()
    d.onString()

    d.onDynamic()
    d?.<!UNRESOLVED_REFERENCE!>onDynamic<!>()

    (d as String).onString()
    (d as Any).onAny()
    (d as Any?).onNullableAny()
    (d as Any).<!UNRESOLVED_REFERENCE!>onDynamic<!>()
}

fun Any.onAny() {}
fun Any?.onNullableAny() {}
fun String.onString() {}
fun dynamic.onDynamic() {}

class C {
    fun test(d: dynamic) {
        d.member()
        d.memberExtension()
    }

    fun member() {}
    fun Any.memberExtension() {}
}
