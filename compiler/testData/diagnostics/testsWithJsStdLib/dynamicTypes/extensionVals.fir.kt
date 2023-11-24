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

    d.<!DEBUG_INFO_DYNAMIC!>onDynamicVal<!> = 1

    (d as String).onStringVal
    (d as Any).onAnyVal
    (d as Any?).onNullableAnyVal
    (d as Any).<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamicVal<!>
}

fun testReassignmentWithSafeCall(d: dynamic) {
    d?.<!DEBUG_INFO_DYNAMIC!>onDynamicVal<!> = 1
}

fun testReassignmentWithStaticCalls(d: dynamic) {
    (d as String).<!VAL_REASSIGNMENT!>onStringVal<!> = 1
    (d as Any).<!VAL_REASSIGNMENT!>onAnyVal<!> = 1
    (d as Any?).<!VAL_REASSIGNMENT!>onNullableAnyVal<!> = 1
    (d as Any).<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC, VAL_REASSIGNMENT!>onDynamicVal<!> = 1
}

val Any.onAnyVal: Int get() = 1
val Any?.onNullableAnyVal: Int get() = 1
val String.onStringVal: Int get() = 1
val <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.onDynamicVal: Int get() = 1

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
