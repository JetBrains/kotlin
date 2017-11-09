import dependency.<error>Foo</error>

fun secondUsage(): String {
    val foo: <error>Foo</error> = <error>Foo</error>()
    return foo.<error>toString</error>()
}
