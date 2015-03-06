fun test() {
    class A {
        default <!DEFAULT_OBJECT_NOT_ALLOWED!>object<!> {}
    }

    object {
        default <!DEFAULT_OBJECT_NOT_ALLOWED!>object<!> {}
    }
}