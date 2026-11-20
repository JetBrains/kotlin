// KT-75481
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK
// DUMP_IR_DIFFERENCE: JKLIB
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
