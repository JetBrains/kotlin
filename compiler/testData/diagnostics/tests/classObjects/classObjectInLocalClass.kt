fun test() {
    class A {
        <!CLASS_OBJECT_NOT_ALLOWED!>class object {}<!>
    }

    object {
        <!CLASS_OBJECT_NOT_ALLOWED!>class object {}<!>
    }
}