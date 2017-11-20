package test

expect var /*rename*/foo: Int
expect var bar: Int

fun test() {
    foo
    foo = 1
    bar
    bar = 1
}