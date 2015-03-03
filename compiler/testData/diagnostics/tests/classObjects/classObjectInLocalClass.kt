fun test() {
    class A {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>class object<!> {}
    }

    object {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>class object<!> {}
    }
}