class Foo<TypeParameter>(
    val a: String = "foo",
    val b: Int = <expr>a.length</expr>,
    val c: Long = (a.length - 1).toLong()
) {
    class NestedClass
}