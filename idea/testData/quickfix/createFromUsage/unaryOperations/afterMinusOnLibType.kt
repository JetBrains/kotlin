// "Create function 'minus' from usage" "true"

fun test() {
    val a = -false
}

fun Boolean.minus(): Any {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}