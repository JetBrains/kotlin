inline fun foo(f: () -> Unit) {
    val ff = { f: () -> Unit ->

    }
    ff(<!USAGE_IS_NOT_INLINABLE!>f<!>)
}