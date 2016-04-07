package testing.rename

public open class Foo(public open val second: String)

public class Bar(public override val second: String) : Foo(second) {
}

fun usages(f: Foo, b: Bar): String {
    return f.second + b.second
}
