enum class C(val i: Int) {
    ONE(<expr>C.foo()</expr>)
    ;

    companion object {
        fun foo() = 1
    }
}