// RUN_PIPELINE_TILL: SOURCE

fun test() {
    run {
        if (true)
            return@run false
        <!UNRESOLVED_REFERENCE!>unresolved<!>.toString()
    }
}
