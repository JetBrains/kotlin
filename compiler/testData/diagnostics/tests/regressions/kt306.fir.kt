// KT-306 Ambiguity when different this's have same-looking functions

fun test() {
    (fun Foo.() {
        <!UNRESOLVED_REFERENCE!>bar<!>()
        (fun Barr.() {
            this.<!UNRESOLVED_REFERENCE!>bar<!>()
            <!UNRESOLVED_REFERENCE!>bar<!>()
        })
    })
    (fun Barr.() {
        this.<!UNRESOLVED_REFERENCE!>bar<!>()
        <!UNRESOLVED_REFERENCE!>bar<!>()
    })
}

class Foo {
    fun bar() {}
}

class Barr {
    fun bar() {}
}
