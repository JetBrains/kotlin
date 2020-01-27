// IGNORE_BACKEND_FIR: ANY
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