// "Create function 'foo'" "true"

fun test(s: String?) {
    if (s == null) return
    foo(s)
}

fun foo(s: String) {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
