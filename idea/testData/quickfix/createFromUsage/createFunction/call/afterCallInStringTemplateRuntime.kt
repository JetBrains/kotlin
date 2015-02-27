// "Create function 'foo'" "true"

fun test() {
    println("a = ${foo()}")
}

fun foo(): String {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
