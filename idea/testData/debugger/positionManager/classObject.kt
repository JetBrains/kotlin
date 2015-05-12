class A {
    companion object {

        init {
            1 + 1 // A
            val a = 1 // A
            fun foo() {
                1 // A\$Companion\$1
            }
        }

        val prop = 1 // A

        val prop2: Int
            get() {
                val a = 1 + 1  // A\$Companion
                return 1 // A\$Companion
            }

        val prop3: Int
            get() = 1 // A\$Companion

        fun foo() = 1 // A\$Companion

        fun foo2() {
            ""   // A\$Companion

            val o = object {
                val p = 1 // A\$Companion\$foo2\$o\$1
                val p2: Int
                    get() {
                        return 1 // A\$Companion\$foo2\$o\$1
                    }
            }
        }
    }
}

interface T {
    companion object {
        val prop = 1 // T\$Companion
    }
}
