package test

// val prop1: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop1 = A().a<!>

// val prop2: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop2 = A().a + 1<!>

class A() {
    var a = 1

    // val prop3: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop3 = a<!>

    // val prop4: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop4 = a + 1<!>

    fun foo() {
        // val prop5: null
        <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop5 = A().a<!>

        // val prop6: null
        <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop6 = A().a + 1<!>

        val b = {
            // val prop11: null
            <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop11 = A().a<!>

            // val prop12: null
            <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop12 = A().a + 1<!>
        }

        val c = object: Foo {
            override fun f() {
                // val prop9: null
                <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop9 = A().a<!>

                // val prop10: null
                <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop10 = A().a + 1<!>
            }
        }
    }

}

fun foo() {
    // val prop7: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop7 = A().a<!>

    // val prop8: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop8 = A().a + 1<!>
}

interface Foo {
    fun f()
}
