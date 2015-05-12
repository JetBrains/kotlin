fun foo() {
    val a = 1
    val b: Int
    b = 2
    42
}

fun bar(foo: Foo) {
    foo.c
    foo.c = 2
    42
}

interface Foo {
    var c: Int
}