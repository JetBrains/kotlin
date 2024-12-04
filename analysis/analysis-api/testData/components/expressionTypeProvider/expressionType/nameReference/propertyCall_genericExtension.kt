interface WithGeneric<T>

interface Foo<T> {
    val foo: WithGeneric<T>
}

val <T> Foo<T>.fooExt: WithGeneric<T> get() = foo

fun take(s: Foo<String>) {
    s.<expr>fooExt</expr>
}