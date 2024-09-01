class Foo<T>

fun <A> Foo<A>.foo(i: Int) {}
fun <B> Foo<B>.foo(s: String) {}

fun m(f: Foo<String>) {
    <expr>f.foo()</expr>
}
