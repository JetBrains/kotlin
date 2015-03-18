fun test() {
    class A {
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object {}
    }

    object {
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object {}
    }
}