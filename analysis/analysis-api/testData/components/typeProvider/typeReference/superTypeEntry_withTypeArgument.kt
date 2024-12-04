abstract class Base<T> {
    abstract foo(arg: T): T
}

class StringSub : B<caret>ase<String> {
    override fun foo(arg: String) = arg + " = 42"
}
