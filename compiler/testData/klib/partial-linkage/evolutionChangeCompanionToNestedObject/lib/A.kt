open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "with companion"

    companion object : N() {
        val qux = "this is in companion object"
    }
}

