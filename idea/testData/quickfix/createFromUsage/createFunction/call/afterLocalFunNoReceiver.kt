// "Create function 'foo' from usage" "true"

fun test() {
    fun foo(i: Int, s: String): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun nestedTest(): Int {
        return foo(2, "2")
    }
}