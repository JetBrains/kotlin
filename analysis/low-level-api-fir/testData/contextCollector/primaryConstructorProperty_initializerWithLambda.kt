fun <T> myRun(action: () -> T): T = action()

class Foo<TypeParameter>(
    val a: String = "foo",
    val b: Int = myRun { <expr>a.length</expr> },
    val c: Long = (a.length - 1).toLong()
) {
    class NestedClass
}