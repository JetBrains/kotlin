import dependency.<error>Foo</error>

fun unnamedUsage(): String {
    val foo: <error>Foo</error> = <error>Foo</error>()
    return foo.<error>toString</error>()
}
