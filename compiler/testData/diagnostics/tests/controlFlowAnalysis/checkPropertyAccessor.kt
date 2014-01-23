package d

val a: Int
    get() {
        val b: Int
        val <!UNUSED_VARIABLE!>c<!>: Int
        <!UNUSED_EXPRESSION!>42<!>

        fun bar(): Int {
            val d: Int
            <!UNUSED_EXPRESSION!>42<!>
            return <!UNINITIALIZED_VARIABLE!>d<!>
        }

        return <!UNINITIALIZED_VARIABLE!>b<!>
    }

class A {
    val a: Int
        get() {
            val b: Int
            val <!UNUSED_VARIABLE!>c<!>: Int
            <!UNUSED_EXPRESSION!>42<!>
            return <!UNINITIALIZED_VARIABLE!>b<!>
        }

    fun foo() {
        class B {
            val a: Int
                get() {
                    val b: Int
                    val <!UNUSED_VARIABLE!>c<!>: Int
                    <!UNUSED_EXPRESSION!>42<!>
                    return <!UNINITIALIZED_VARIABLE!>b<!>
                }
        }
    }
}