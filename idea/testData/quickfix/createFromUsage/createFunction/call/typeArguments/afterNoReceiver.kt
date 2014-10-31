// "Create function 'foo'" "true"

fun test(): Int {
    return foo<String, Int>(2, "2")
}

fun <T, U> foo(arg: U, arg1: T): U {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}