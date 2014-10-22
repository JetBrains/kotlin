// "Create function 'foo' from usage" "true"

fun test(): Int {
    return foo<Int>(2, "2")
}

fun <T> foo(arg: T, s: String): T {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}