// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

enum class TestEnum1 {
    TEST1, TEST2
}

enum class TestEnum2(val x: Int) {
    TEST1(1),
    TEST2(2),
    TEST3(3) {}
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

enum class TestEnum5(val x: Int = 0) {
    TEST1, TEST2(), TEST3(0)
}

fun f(): Int = 1

enum class TestEnum6(val x: Int, val y: Int) {
    TEST(y = f(), x = f())
}
