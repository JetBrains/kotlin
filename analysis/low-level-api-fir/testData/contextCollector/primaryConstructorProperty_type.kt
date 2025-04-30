class Foo<TypeParameter>(
    val a: String = "foo",
    val b: <expr>Int</expr> = a.length,
    val c: Long = (a.length - 1).toLong()
) {
    class NestedClass
}