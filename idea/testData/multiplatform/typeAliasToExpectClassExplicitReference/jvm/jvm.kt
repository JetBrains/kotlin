actual class <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonMember<!>() { }

    fun platformMember() { }
}

fun test() {
    <!RESOLUTION_TO_CLASSIFIER!>TypealiasFromCommon<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>commonMember<!>()
    <!RESOLUTION_TO_CLASSIFIER!>TypealiasFromCommon<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>platformwMember<!>()
}