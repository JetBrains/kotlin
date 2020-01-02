inline fun foo(f: () -> Unit) {
    val ff = { f: () -> Unit ->
        f.invoke()
    }
    ff(f)
}
