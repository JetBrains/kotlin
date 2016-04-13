package testing.rename

public open class Foo(public open val second: String)

public class Bar : Foo("abc") {
    override val second = "xyzzy"
}

fun usages(f: Foo, b: Bar): String {
    return f.second + b.second
}
