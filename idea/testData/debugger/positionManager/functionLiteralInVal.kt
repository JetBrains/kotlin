class A {
    fun foo() {
        val a = {
            fun innerFoo() {
                val b = 1   // A\$foo\$a\$1\$1
            }
            innerFoo()
        }()
    }
}
