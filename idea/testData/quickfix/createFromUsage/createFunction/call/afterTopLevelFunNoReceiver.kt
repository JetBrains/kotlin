// "Create function 'foo' from usage" "true"

fun test(): Int {
    return foo(2, "2")
}

fun foo(i: Int, s: String): Int {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}