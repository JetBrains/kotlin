class Foo<T>

fun <A> Foo<A>.foo(i: Int) {}

fun m(f: Foo<String>) {
    <expr>f.foo()</expr>
}
