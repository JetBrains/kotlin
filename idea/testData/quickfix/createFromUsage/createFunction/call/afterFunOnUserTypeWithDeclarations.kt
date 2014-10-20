// "Create function 'foo' from usage" "true"
class F {
    fun bar() {

    }

    fun foo(i: Int, s: String): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class X {
    val f: Int = F().foo(1, "2")
}