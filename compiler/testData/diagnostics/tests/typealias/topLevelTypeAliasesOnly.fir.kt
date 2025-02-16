// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

typealias TopLevel = Any

interface A {
    <!UNSUPPORTED_FEATURE!>typealias Nested = Any<!>
}

class C {
    <!UNSUPPORTED_FEATURE!>typealias Nested = Any<!>
    class D {
        <!UNSUPPORTED_FEATURE!>typealias Nested = Any<!>
        fun foo() {
            <!UNSUPPORTED!>typealias LocalInMember = Any<!>
        }
    }
}

fun foo() {
    <!UNSUPPORTED!>typealias Local = Any<!>
}
