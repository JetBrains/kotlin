// !DIAGNOSTICS:-USELESS_CAST
// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!>
    d.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!> = 1

    d?.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!>
    d?.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!> = 1

    run {
        d!!.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!>
    }
    run {
        d!!.<!DEBUG_INFO_DYNAMIC!>onAnyVal<!> = 1
    }

    d.<!DEBUG_INFO_DYNAMIC!>onNullableAnyVal<!> = 1

    d.<!DEBUG_INFO_DYNAMIC!>onStringVal<!> = 1

    <!VAL_REASSIGNMENT!>d.onDynamicVal<!> = 1

    (d as String).onStringVal
    (d as Any).onAnyVal
    (d as Any?).onNullableAnyVal
    (d as Any).onDynamicVal
}

fun testReassignmentWithSafeCall(d: dynamic) {
    <!VAL_REASSIGNMENT!>d?.onDynamicVal<!> = 1
}

fun testReassignmentWithStaticCalls(d: dynamic) {
    <!VAL_REASSIGNMENT!>(d as String).onStringVal<!> = 1
    <!VAL_REASSIGNMENT!>(d as Any).onAnyVal<!> = 1
    <!VAL_REASSIGNMENT!>(d as Any?).onNullableAnyVal<!> = 1
    <!VAL_REASSIGNMENT!>(d as Any).onDynamicVal<!> = 1
}

val Any.onAnyVal: Int get() = 1
val Any?.onNullableAnyVal: Int get() = 1
val String.onStringVal: Int get() = 1
val dynamic.onDynamicVal: Int get() = 1

class C {
    fun test(d: dynamic) {
        d.<!DEBUG_INFO_DYNAMIC!>memberVal<!>
        d.<!DEBUG_INFO_DYNAMIC!>memberVal<!> = 1

        d.<!DEBUG_INFO_DYNAMIC!>memberExtensionVal<!>
        d.<!DEBUG_INFO_DYNAMIC!>memberExtensionVal<!> = 1
    }

    val memberVal = 1
    val Any.memberExtensionVal: Int
        get() = 1
}