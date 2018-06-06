import dependency.Foo

fun unnamedUsage(): String {
    val foo: Foo = Foo()
    return foo.toString()
}
