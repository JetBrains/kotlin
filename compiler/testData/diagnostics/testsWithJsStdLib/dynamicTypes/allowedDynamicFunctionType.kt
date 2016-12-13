// !MARK_DYNAMIC_CALLS

fun withDynamicReceiver(<!UNUSED_PARAMETER!>d<!>: dynamic.() -> Unit) {}

fun test() = withDynamicReceiver {
    <!DEBUG_INFO_DYNAMIC!>foo<!>
    <!DEBUG_INFO_DYNAMIC!>bar<!> = 1
}

fun test2() = withDynamicReceiver(fun dynamic.() {})

val dynamicProperty: dynamic.() -> Unit = TODO()

fun test(d: dynamic, <!UNUSED_PARAMETER!>dynamicParameter<!>: dynamic.() -> Unit) {
    d.<!DEBUG_INFO_DYNAMIC!>dynamicProperty<!>()
    d.<!DEBUG_INFO_DYNAMIC!>dynamicParameter<!>()
}