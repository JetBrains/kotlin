package test

expect class C {
    var foo: Int
    var bar: Int
}

fun test(c: C) {
    c.foo
    c.foo = 1
    c.bar
    c.bar = 1
}