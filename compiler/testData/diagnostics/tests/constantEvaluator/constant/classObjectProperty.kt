package test

// val prop1: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop1 = A.a<!>

// val prop2: 2
<!DEBUG_INFO_CONSTANT_VALUE("2")!>val prop2 = A.a + 1<!>

class A {
    // val prop3: 1
    <!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop3 = A.a<!>

    // val prop4: 2
    <!DEBUG_INFO_CONSTANT_VALUE("2")!>val prop4 = A.a + 1<!>

    // val prop7: 0
    <!DEBUG_INFO_CONSTANT_VALUE("0")!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val prop7 = 0<!>

    // val prop8: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop8 = prop7<!>

    companion object {
        val a = 1
    }
}

fun foo() {
    // val prop5: 1
    <!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop5 = A.a<!>

    // val prop6: 2
    <!DEBUG_INFO_CONSTANT_VALUE("2")!>val prop6 = A.a + 1<!>

    val b = {
        // val prop11: 1
        <!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop11 = A.a<!>

        // val prop12: 2
        <!DEBUG_INFO_CONSTANT_VALUE("2")!>val prop12 = A.a + 1<!>
    }

    val c = object: Foo {
        override fun f() {
            // val prop9: 1
            <!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop9 = A.a<!>

            // val prop10: 2
            <!DEBUG_INFO_CONSTANT_VALUE("2")!>val prop10 = A.a + 1<!>
        }
    }
}

interface Foo {
    fun f()
}
