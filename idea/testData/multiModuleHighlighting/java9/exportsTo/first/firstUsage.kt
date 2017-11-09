import dependency.Foo

fun firstUsage(): String {
    val foo: Foo = Foo()
    return foo.toString()
}
