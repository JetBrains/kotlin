package test

actual var foo: Int
    get() = 1
    set(value) {}

actual var bar: Int
    get() = 1
    set(value) {}

fun test() {
    foo
    foo = 1
    bar
    bar = 1
}