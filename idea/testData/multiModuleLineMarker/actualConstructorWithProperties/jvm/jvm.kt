actual class <!LINE_MARKER("descr='Has declaration in common module'")!>WithConstructor<!> actual constructor(actual val x: Int, actual val s: String)

/*
LINEMARKER: Has declaration in common module
TARGETS:
common.kt
expect class <1>WithConstructor(x: Int, s: String) {
    val <3>x: Int

    val <2>s: String
*/
