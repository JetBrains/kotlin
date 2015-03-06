class A {
    default object {

        {
            1 + 1 // A
            val a = 1 // A
            fun foo() {
                1 // A\$Default\$1
            }
        }

        val prop = 1 // A

        val prop2: Int
            get() {
                val a = 1 + 1  // A\$Default
                return 1 // A\$Default
            }

        val prop3: Int
            get() = 1 // A\$Default

        fun foo() = 1 // A\$Default

        fun foo2() {
            ""   // A\$Default

            val o = object {
                val p = 1 // A\$Default\$foo2\$o\$1
                val p2: Int
                    get() {
                        return 1 // A\$Default\$foo2\$o\$1
                    }
            }
        }
    }
}

trait T {
    default object {
        val prop = 1 // T\$Default
    }
}
