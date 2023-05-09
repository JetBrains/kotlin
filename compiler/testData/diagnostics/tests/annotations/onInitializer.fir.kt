class A {
    @ann init {}
    @aaa init {}
}

interface T {
    @ann <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {}
    @aaa <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {}
}

annotation class ann
