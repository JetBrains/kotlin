package sample

expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>A<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFun<!>()
    val <!LINE_MARKER("descr='Has actuals in JVM'")!>x<!>: Int
    val <!LINE_MARKER("descr='Has actuals in JVM'")!>y<!>: Double
    val <!LINE_MARKER("descr='Has actuals in JVM'")!>z<!>: String
}

fun getCommonA(): A = null!!