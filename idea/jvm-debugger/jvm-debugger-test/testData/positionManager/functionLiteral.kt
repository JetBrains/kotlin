class A {
    fun foo() {
        {
            fun innerFoo() {
                ""   // A\$foo\$1\$1
            }
            innerFoo()
        }()
    }
}
