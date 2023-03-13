// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

enum class Z {
    ENTRY {
        fun test() {}

        inner class A {
            fun test2() {
                test()
            }
        }
    }
}
