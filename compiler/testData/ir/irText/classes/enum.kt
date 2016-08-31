enum class TestEnum1 {
    TEST1, TEST2
}

enum class TestEnum2(val x: Int) {
    TEST1(1),
    TEST2(2),
    TEST3(3)
}

enum class TestEnum3 {
    TEST {
        override fun foo() {
            println("Hello, world!")
        }
    }
    ;
    abstract fun foo()
}

enum class TestEnum4(val x: Int) {
    TEST1(1) {
        override fun foo() {
            println(TEST1)
        }
    },
    TEST2(2) {
        val z: Int
        init {
            z = x
        }
        override fun foo() {
            println(TEST2)
        }
    }
    ;
    abstract fun foo()
}