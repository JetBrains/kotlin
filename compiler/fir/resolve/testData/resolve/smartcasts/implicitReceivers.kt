class A {
    fun foo() {}
}

fun <T> T.with(block: T.() -> Unit) {}

fun Any?.test_1() {
    if (this is A) {
        this.foo()
        foo()
    } else {
        this.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    }
    this.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!UNRESOLVED_REFERENCE!>foo<!>()
}

fun Any?.test_2() {
    if (this !is A) {
        this.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    } else {
        this.foo()
        foo()
    }
    this.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!UNRESOLVED_REFERENCE!>foo<!>()
}


fun test_3(a: Any, b: Any, c: Any) {
    with(a) wa@{
        with(b) wb@{
            with(c) wc@{
                this@wb as A
                this@wb.foo()
                foo()
            }
            this.foo()
            foo()
        }
    }
}