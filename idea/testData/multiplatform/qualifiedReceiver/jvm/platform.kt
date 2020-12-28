@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package foo

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonFun<!>()
    actual val <!LINE_MARKER("descr='Has declaration in common module'")!>b<!>: B
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>bFun<!>(): B
    fun platformFun()
}

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>B<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonFunB<!>()
    fun platformFunB()
}
