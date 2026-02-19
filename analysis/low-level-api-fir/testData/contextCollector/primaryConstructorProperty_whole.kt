class Foo<TypeParameter>(
    val a: String = "foo",
    <expr>val b: Int = a.length</expr>,
    val c: Long = (a.length - 1).toLong()
) {
    class NestedClass
}