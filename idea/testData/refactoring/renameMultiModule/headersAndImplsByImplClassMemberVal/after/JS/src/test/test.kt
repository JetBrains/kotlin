package test

actual class C {
    actual var baz: Int
        get() = 1
        set(value) {}

    actual var bar: Int
        get() = 1
        set(value) {}
}

fun test(c: C) {
    c.baz
    c.baz = 1
    bar
    bar = 1
}