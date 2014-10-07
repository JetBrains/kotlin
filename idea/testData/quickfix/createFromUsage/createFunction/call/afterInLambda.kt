// "Create function 'foo' from usage" "true"

fun run<T>(f: () -> T) = f()

fun test() {
    run { foo() }
}

fun foo(): Any {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}