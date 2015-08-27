package testing.rename

public open class Foo(public open val first: String)

public class Bar(public override val /*rename*/first: String) : Foo(first) {
}

fun usages(f: Foo, b: Bar): String {
    return f.first + b.first
}
