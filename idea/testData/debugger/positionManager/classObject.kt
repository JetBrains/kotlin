class A {
    class object {

        {
            1 + 1 // A
            val a = 1 // A
            fun foo() {
                1 // A\$object\$1
            }
        }

        val prop = 1 // A

        val prop2: Int
            get() {
                val a = 1 + 1  // A\$object
                return 1 // A\$object
            }

        val prop3: Int
            get() = 1 // A\$object

        fun foo() = 1 // A\$object

        fun foo2() {
            ""   // A\$object

            val o = object {
                val p = 1 // A\$object\$foo2\$o\$1
                val p2: Int
                    get() {
                        return 1 // A\$object\$foo2\$o\$1
                    }
            }
        }
    }
}

trait T {
    class object {
        val prop = 1 // T\$object
    }
}
