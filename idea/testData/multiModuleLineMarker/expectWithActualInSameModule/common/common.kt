package sample

expect fun <!LINE_MARKER("descr='Has actuals in common'")!>sameFile<!>()

actual fun <!LINE_MARKER("descr='null'")!>sameFile<!>() = Unit

expect fun <!LINE_MARKER("descr='Has actuals in common'")!>sameModule<!>()

fun noExpectActualDeclaration() = Unit