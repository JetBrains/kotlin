// IGNORE_BACKEND: JS_IR JS_IR_ES6 NATIVE
// ^ KT-61141: absent enum fake_overrides: finalize (K1), getDeclaringClass (K1), clone (K2)

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
