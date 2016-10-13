inline fun foo(f: () -> Unit) {
    val ff = { f: () -> Unit ->
        f.invoke()
    }
    ff(<!USAGE_IS_NOT_INLINABLE!>f<!>)
}
