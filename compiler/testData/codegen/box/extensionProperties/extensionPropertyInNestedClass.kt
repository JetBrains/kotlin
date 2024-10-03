class A {
    val a: String = "O"

    class B {
        val b: String = "K"

        val A.foo: String
            get() {
                return this.a + this@B.b
            }

        fun test() = A().foo
    }
}

fun box(): String {
    return A.B().test()
}