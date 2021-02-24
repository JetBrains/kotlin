actual class <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonMember<!>() { }

    fun platformMember() { }
}

fun test() {
    A().commonMember()
    A().platformMember()
}