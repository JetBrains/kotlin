class A {
    fun foo() {
        val a = {
            fun innerFoo() {
                val b = {}   // A$foo$a$1$1
            }
            innerFoo()
        }()
    }
}
