package test

expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>Expect<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>commonFun<!>(): String
}

fun topLevelCommonFun(): Int = 42