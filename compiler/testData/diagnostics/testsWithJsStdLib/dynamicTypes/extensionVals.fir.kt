// !DIAGNOSTICS:-USELESS_CAST
// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d.onAnyVal
    d.onAnyVal = 1

    d?.onAnyVal
    d?.onAnyVal = 1

    run {
        d!!.onAnyVal
    }
    run {
        d!!.<!UNRESOLVED_REFERENCE!>onAnyVal<!> = 1
    }

    d.onNullableAnyVal = 1

    d.onStringVal = 1

    d.onDynamicVal = 1

    (d as String).onStringVal
    (d as Any).onAnyVal
    (d as Any?).onNullableAnyVal
    (d as Any).<!UNRESOLVED_REFERENCE!>onDynamicVal<!>
}

fun testReassignmentWithSafeCall(d: dynamic) {
    d?.onDynamicVal = 1
}

fun testReassignmentWithStaticCalls(d: dynamic) {
    (d as String).<!VAL_REASSIGNMENT!>onStringVal<!> = 1
    (d as Any).<!VAL_REASSIGNMENT!>onAnyVal<!> = 1
    (d as Any?).<!VAL_REASSIGNMENT!>onNullableAnyVal<!> = 1
    (d as Any).<!UNRESOLVED_REFERENCE!>onDynamicVal<!> = 1
}

val Any.onAnyVal: Int get() = 1
val Any?.onNullableAnyVal: Int get() = 1
val String.onStringVal: Int get() = 1
val dynamic.onDynamicVal: Int get() = 1

class C {
    fun test(d: dynamic) {
        d.memberVal
        d.memberVal = 1

        d.memberExtensionVal
        d.memberExtensionVal = 1
    }

    val memberVal = 1
    val Any.memberExtensionVal: Int
        get() = 1
}
