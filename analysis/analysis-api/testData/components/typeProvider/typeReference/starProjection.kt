abstract class Base<T> {
    abstract foo(arg: T): T
}

class StringSub : Base<String> {
    override fun foo(arg: String) = arg + " = 42"
}

fun test() {
    val x : Base<<caret>*> = StringSub()
    x.foo("42")
}