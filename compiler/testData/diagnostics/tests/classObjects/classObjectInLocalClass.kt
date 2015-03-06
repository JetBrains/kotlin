fun test() {
    class A {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object {}
    }

    object {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object {}
    }
}