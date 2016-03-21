fun foo() {
    var x: String
    class A {
        init {
            x = ""
        }
    }
    // Error! See KT-10042
    <!UNINITIALIZED_VARIABLE!>x<!>.length
}

fun bar() {
    var x: String
    object: Any() {
        init {
            x = ""
        }
    }
    // Ok
    x.length
}
