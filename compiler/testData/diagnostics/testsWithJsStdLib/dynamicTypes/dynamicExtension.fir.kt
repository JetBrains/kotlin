// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -ERROR_SUPPRESSION
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.DynamicExtension
fun dynamic.onDynamicFun() = 1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.DynamicExtension
var dynamic.onDynamicProperty
    get() = ""
    set(value) {}


fun test(d: dynamic, a: Any?) {
    eatT<Int>(d.onDynamicFun())
    eatT<String>(d.onDynamicProperty)
    d.onDynamicProperty = ""
    eatT<Iterator<*>>(d.iterator())

    for (item in d) {
        println(item)
    }

    a.<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamicFun<!>()
    a.<!DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC!>onDynamicProperty<!>
}

fun <T> eatT(t: T) {}
