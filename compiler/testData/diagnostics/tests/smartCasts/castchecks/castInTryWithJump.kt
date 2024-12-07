// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56744
// DUMP_CFG

interface A {
    fun aaa()
}

interface B {
    fun bbb()
}

interface C {
    fun ccc()
}

fun breakInTry_withNestedFinally() {
    var x: Any? = null
    while (true) {
        try {
            x as A
            break
        } finally {
            try {
                x as B
            } finally {
                x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
                x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
            }
            x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
            x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be ok
        }
        <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>aaa<!>()<!> // should be error
        <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>bbb<!>()<!> // should be ok
    }
    x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be ok
    x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be ok
}

fun returnInCatch() {
    var x: Any? = null
    try {
        x as A
    } catch (e: Exception) {
        x as B
        return
    } finally {
        x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
        x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
    }
    x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be ok
    x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
}

fun returnInCatch_insideFinally() {
    var x: Any? = null
    try {
        x as C
    } finally {
        try {
            x as A
        } catch (e: Exception) {
            x as B
            return
        } finally {
            x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
            x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
            x.<!UNRESOLVED_REFERENCE!>ccc<!>() // should be error
        }
        x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be ok
        x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
        x.<!UNRESOLVED_REFERENCE!>ccc<!>() // should be error
    }
    x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be ok
    x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
    x.<!UNRESOLVED_REFERENCE!>ccc<!>() // should be ok
}

fun breakInCatch() {
    var x: Any? = null
    while (true) {
        try {
            x as A
        } catch (e: Exception) {
            x as B
            break
        } finally {
            x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
            x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
        }
        x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be ok
        x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
    }
    x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
    x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be ok
}

fun returnInFinally_insideTry_nonLocal() {
    var x: Any? = null
    run {
        try {
            x as B
            try {
                x as A
            } finally {
                x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
                <!DEBUG_INFO_SMARTCAST!>x<!>.bbb() // should be ok
                return
            }
        } finally {
            x.<!UNRESOLVED_REFERENCE!>aaa<!>() // should be error
            x.<!UNRESOLVED_REFERENCE!>bbb<!>() // should be error
        }
        <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>aaa<!>()<!> // should be error
        <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>bbb<!>()<!> // should be error
    }
    <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>aaa<!>()<!> // should be error
    <!UNREACHABLE_CODE!>x.<!UNRESOLVED_REFERENCE!>bbb<!>()<!> // should be error
}
