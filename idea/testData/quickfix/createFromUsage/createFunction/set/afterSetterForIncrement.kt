// "Create function 'set' from usage" "true"
class A {
    fun get(s: String): Int = 1

    fun set(s: String, value: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun foo() {
    var a = A()
    a["1"]++
}
