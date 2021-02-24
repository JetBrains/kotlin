@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package aliases

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>commonFun<!>()
    fun platformFun()
}

typealias A2 = A1
typealias A3 = A

actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>B<!> = A

typealias B2 = B
typealias B3 = B1

class PlatformInv<T>(val value: T)