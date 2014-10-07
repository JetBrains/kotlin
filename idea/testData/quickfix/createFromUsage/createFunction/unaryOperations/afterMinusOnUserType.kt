// "Create function 'minus' from usage" "true"

class A<T>(val n: T) {
    fun minus(): Any {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a = -A(1)
}