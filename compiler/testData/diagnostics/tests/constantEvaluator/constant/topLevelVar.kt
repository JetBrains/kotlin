package test

var a = 1

// val prop1: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop1 = a<!>

// val prop2: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop2 = a + 1<!>

class A {
    // val prop3: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop3 = a<!>

    // val prop4: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop4 = a + 1<!>

    val b = {
        // val prop11: null
        <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop11 = a<!>

        // val prop12: null
        <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop12 = a + 1<!>
    }

    val c = object: Foo {
        override fun f() {
            // val prop9: null
            <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop9 = a<!>

            // val prop10: null
            <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop10 = a + 1<!>
        }
    }
}

fun foo() {
    // val prop5: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop5 = a<!>

    // val prop6: null
    <!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop6 = a + 1<!>
}

interface Foo {
    fun f()
}
