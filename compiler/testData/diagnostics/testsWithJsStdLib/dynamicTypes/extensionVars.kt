// !MARK_DYNAMIC_CALLS

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

    d.onDynamicVar = 1

    d?.onDynamicVar = 1

    (d: String).onStringVar
    (d: Any).onAnyVar
    (d: Any?).onNullableAnyVar
    (d: Any).onDynamicVar

    (d: String).onStringVar = 1
    (d: Any).onAnyVar = 1
    (d: Any?).onNullableAnyVar = 1
    (d: Any).onDynamicVar = 1
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

var dynamic.onDynamicVar: Int
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