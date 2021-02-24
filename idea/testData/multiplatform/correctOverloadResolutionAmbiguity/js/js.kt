actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>A<!><T> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>foo<!>(x: T)
    fun foo(x: String)
}

fun main() {
    bar().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>("")
}
