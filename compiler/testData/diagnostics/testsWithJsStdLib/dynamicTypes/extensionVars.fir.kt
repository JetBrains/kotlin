// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS:-USELESS_CAST

fun test(d: dynamic) {
    d.onAnyVar
    d.onAnyVar = 1

    d?.onAnyVar
    d?.onAnyVar = 1

    run {
        d!!.onAnyVar

    }
    run {
        d!!.<!UNRESOLVED_REFERENCE!>onAnyVar<!> = 1
    }

    d.onNullableAnyVar = 1

    d.onStringVar = 1

    d.onDynamicVar = 1

    d?.<!UNRESOLVED_REFERENCE!>onDynamicVar<!> = 1

    (d as String).onStringVar
    (d as Any).onAnyVar
    (d as Any?).onNullableAnyVar
    (d as Any).<!UNRESOLVED_REFERENCE!>onDynamicVar<!>

    (d as String).onStringVar = 1
    (d as Any).onAnyVar = 1
    (d as Any?).onNullableAnyVar = 1
    (d as Any).<!UNRESOLVED_REFERENCE!>onDynamicVar<!> = 1
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
        d.memberVar
        d.memberExtensionVar

        d.memberVar = 1
        d.memberExtensionVar = 1
    }

    var memberVar = 1
    var Any.memberExtensionVar: Int
        get() = 1
        set(v) {}
}
