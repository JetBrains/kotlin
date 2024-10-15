// RUN_PIPELINE_TILL: SOURCE

fun test() {
    run {
        if (true)
            return@run false
        <!UNRESOLVED_REFERENCE!>unresolved<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>()
    }
}
