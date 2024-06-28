fun test1() {
    val x: Int

    fun func() {
        <!CAPTURED_VAL_INITIALIZATION!>x<!> = 0
    }

    println(<!UNINITIALIZED_VARIABLE!>x<!>)
}


fun test2() {
    val x: Int
    val y: Int
    val z: Int

    object {
        init {
            x = 0
        }

        fun localFunc() {
            <!CAPTURED_VAL_INITIALIZATION!>y<!> = 0
        }

        val prop: Int
            get() {
                <!CAPTURED_VAL_INITIALIZATION!>z<!> = 0
                return 2
            }
    }

    println(x)
    println(x)
}

fun test3() {
    val x: Int
    val y: Int

    class A {
        init {
            <!CAPTURED_VAL_INITIALIZATION!>x<!> = 0
        }

        fun localFunc() {
            <!CAPTURED_VAL_INITIALIZATION!>y<!> = 0
        }
    }

    println(<!UNINITIALIZED_VARIABLE!>x<!>)
    println(<!UNINITIALIZED_VARIABLE!>x<!>)
}
