class Foo(
    a: String = "foo",
    b: Int = <expr>a.length</expr>,
    c: Long = (a.length - 1).toLong()
)