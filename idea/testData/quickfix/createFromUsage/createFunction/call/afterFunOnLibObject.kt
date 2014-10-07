// "Create function 'foo' from usage" "true"

fun test() {
    val a: Int = Unit.foo(2)
}

fun Unit.foo(i: Int): Int {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}