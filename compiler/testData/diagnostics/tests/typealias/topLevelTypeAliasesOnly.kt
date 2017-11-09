typealias TopLevel = Any

interface A {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
}

class C {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
    class D {
        <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
        fun foo() {
            <!TOPLEVEL_TYPEALIASES_ONLY!>typealias LocalInMember = Any<!>
        }
    }
}

fun foo() {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Local = Any<!>
}