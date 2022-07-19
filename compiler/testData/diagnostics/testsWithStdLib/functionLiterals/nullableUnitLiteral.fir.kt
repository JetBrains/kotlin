// FULL_JDK

fun test() {
    val closeable: java.io.Closeable? = null
    val closeF = fun() { closeable?.close() }
    val closeFB = fun(): Unit = <!RETURN_TYPE_MISMATCH!>closeable?.close()<!>
    val closeFR = fun() { return <!RETURN_TYPE_MISMATCH!>closeable?.close()<!> }
    val closeL = { closeable?.close() }
    val closeLR = label@ { return@label closeable?.close() }
}
