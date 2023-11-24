// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS:-USELESS_CAST

fun test(d: dynamic) {
    d.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!>
    d.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!> = 1

    d?.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!>
    d?.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!> = 1

    run {
        d!!.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!>

    }
    run {
        d!!.<!DEBUG_INFO_DYNAMIC!>onAnyVar<!> = 1
    }

    d.<!DEBUG_INFO_DYNAMIC!>onNullableAnyVar<!> = 1

    d.<!DEBUG_INFO_DYNAMIC!>onStringVar<!> = 1

    d.<!DEBUG_INFO_DYNAMIC!>onDynamicVar<!> = 1

    d?.<!DEBUG_INFO_DYNAMIC!>onDynamicVar<!> = 1

    (d as String).onStringVar
    (d as Any).onAnyVar
    (d as Any?).onNullableAnyVar
    (d as Any).<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamicVar<!>

    (d as String).onStringVar = 1
    (d as Any).onAnyVar = 1
    (d as Any?).onNullableAnyVar = 1
    (d as Any).<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamicVar<!> = 1
}

var Any.onAnyVar: Int
    get() = 1
    set(v) {}

var Any?.onNullableAnyVar: Int
    get() = 1
    set(v) {}

var String.onStringVar: Int
    get() = 1
    set(v) {}

var <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.onDynamicVar: Int
    get() = 1
    set(v) {}

class C {
    fun test(d: dynamic) {
        d.<!DEBUG_INFO_DYNAMIC!>memberVar<!>
        d.<!DEBUG_INFO_DYNAMIC!>memberExtensionVar<!>

        d.<!DEBUG_INFO_DYNAMIC!>memberVar<!> = 1
        d.<!DEBUG_INFO_DYNAMIC!>memberExtensionVar<!> = 1
    }

    var memberVar = 1
    var Any.memberExtensionVar: Int
        get() = 1
        set(v) {}
}
