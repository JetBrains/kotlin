abstract class Base<T> {
    abstract foo(arg: T): T
}

class StringSub : Base<Strin<caret>g> {
    override fun foo(arg: String) = arg + " = 42"
}
