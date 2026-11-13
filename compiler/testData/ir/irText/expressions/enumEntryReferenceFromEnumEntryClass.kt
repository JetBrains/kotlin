// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
enum class MyEnum {
    Z {
        var counter = 0
        fun foo() {}

        fun bar() {
            counter = 1
            foo()
        }

        val aLambda = {
            counter = 1
            foo()
        }

        val anObject = object {
            init {
                counter = 1
                foo()
            }

            fun test() {
                counter = 1
                foo()
            }
        }
    }
}
