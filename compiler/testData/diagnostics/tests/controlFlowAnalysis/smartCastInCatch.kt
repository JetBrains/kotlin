// !SKIP_TXT

fun exc(flag: Boolean) {
    if (flag) throw RuntimeException()
}

fun test(flag: Boolean) {
    var x: Any?
    x = ""
    try {
        <!UNUSED_VALUE!>x =<!> null
        exc(flag)
        <!UNUSED_VALUE!>x =<!> 1
        exc(!flag)
        x = ""
    } catch (e: Throwable) {
        // all bad - could come here from either call
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}
