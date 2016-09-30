class Outer {
    fun foo() {}

    inner class Inner {
        fun test() {
            foo()
        }

        inner class Inner2 {
            fun test2() {
                test()
                foo()
            }

            fun Outer.test3() {
                foo()
            }
        }
    }
}