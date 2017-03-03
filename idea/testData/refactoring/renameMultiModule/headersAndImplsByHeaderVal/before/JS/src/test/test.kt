package test

impl var foo: Int
    get() = 1
    set(value) {}

impl var bar: Int
    get() = 1
    set(value) {}

fun test() {
    foo
    foo = 1
    bar
    bar = 1
}