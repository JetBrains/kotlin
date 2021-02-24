@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package aliases

expect interface <!LINE_MARKER("descr='Has actuals in JVM'")!>A<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFun<!>()
}

typealias A1 = A

expect interface <!LINE_MARKER("descr='Has actuals in JVM'")!>B<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFun<!>()
}

typealias B1 = B

class CommonInv<T>(val value: T)