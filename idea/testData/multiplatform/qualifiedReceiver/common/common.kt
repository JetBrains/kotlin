@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package foo

expect interface <!LINE_MARKER("descr='Has actuals in JVM'")!>A<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFun<!>()
    val <!LINE_MARKER("descr='Has actuals in JVM'")!>b<!>: B
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>bFun<!>(): B
}

expect interface <!LINE_MARKER("descr='Has actuals in JVM'")!>B<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFunB<!>()
}

class Common {
    val a: A get() = null!!
    fun aFun(): A = null!!
}
