fun test() {
    class A {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }

    object {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}