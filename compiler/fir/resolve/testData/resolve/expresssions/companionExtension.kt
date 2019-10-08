class My {
    companion object {
        fun My.foo() {}
    }

    fun test() {
        foo()
    }
}