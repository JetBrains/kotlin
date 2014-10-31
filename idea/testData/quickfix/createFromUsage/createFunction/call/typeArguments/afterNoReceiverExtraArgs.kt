// "Create function 'foo'" "true"

fun test(): Int {
    return foo<String, Int, Boolean>(2, "2")
}

fun <T, U, V> foo(arg: U, arg1: T): U {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}