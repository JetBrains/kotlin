open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "without companion"

    object W : N() {
        val qux = "this is in object"
    }

}

