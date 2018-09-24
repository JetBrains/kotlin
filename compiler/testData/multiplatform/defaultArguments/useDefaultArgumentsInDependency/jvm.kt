package lib

actual fun foo(x: Int, y: String) {}

actual class C actual constructor(x: Int, y: String) {}

actual annotation class Anno1(actual val x: Int, actual val y: String = "OK")

actual annotation class Anno2(actual val x: Int, actual val y: String)
