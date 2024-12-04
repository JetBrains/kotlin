class Foo<T>

val <A> Foo<A>.foo: Int get() = 0

fun m(f: Foo<String>) {
    <expr>f.foo</expr>
}
