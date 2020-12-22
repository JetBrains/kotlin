package test

actual class <!LINE_MARKER("descr='Has declaration in common module'")!>Expect<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonFun<!>(): String = ""

    fun platformFun(): Int = 42
}

fun topLevelPlatformFun(): String = ""