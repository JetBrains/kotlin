// KT-75481
// SKIP_KOTLIN_REFLECT_K1_VS_K2_CHECK
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
