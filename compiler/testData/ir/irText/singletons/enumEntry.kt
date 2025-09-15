// KT-75481
// SKIP_NEW_KOTLIN_REFLECT_COMPAT_CHECK
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
