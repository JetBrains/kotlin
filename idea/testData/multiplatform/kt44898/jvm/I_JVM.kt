package sample

public actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>I<!> {
    public actual suspend fun <A : <!FINAL_UPPER_BOUND!>Appendable<!>> <!LINE_MARKER("descr='Has declaration in common module'")!>readUTF8LineTo<!>(out: A, limit: Int): Boolean
}
