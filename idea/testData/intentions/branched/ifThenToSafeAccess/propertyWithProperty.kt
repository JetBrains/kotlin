// IS_APPLICABLE: false

interface Foo
interface Bar : Foo {
    val x: String
}

data class Data(val foo: Foo)

fun handle(data: Data) {
    // Not available yet (possible in principle)
    val bar = <caret>if (data.foo is Bar) data.foo.x else null
}