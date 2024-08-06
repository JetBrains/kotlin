// FIR_IDENTICAL
// ISSUE: KT-52315

enum class Foo(id: Int) {<!SYNTAX!><!>
    header<!SYNTAX!>(<!><!SYNTAX!>1<!><!SYNTAX!>)<!>
}

enum class Bar(id: Int) {<!SYNTAX!><!>
    impl<!SYNTAX!>(<!><!SYNTAX!>2<!><!SYNTAX!>)<!>
}
