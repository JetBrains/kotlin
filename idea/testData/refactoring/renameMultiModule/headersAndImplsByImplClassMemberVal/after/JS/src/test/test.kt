package test

impl class C {
    impl var baz: Int
        get() = 1
        set(value) {}

    impl var bar: Int
        get() = 1
        set(value) {}
}

fun test(c: C) {
    c.baz
    c.baz = 1
    bar
    bar = 1
}