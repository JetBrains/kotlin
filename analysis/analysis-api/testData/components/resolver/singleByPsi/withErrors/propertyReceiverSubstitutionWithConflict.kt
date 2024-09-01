class Foo<T>

val <A> Foo<A>.foo: Int get() = 0
val <B> Foo<B>.foo: String get() = "str"

fun m(f: Foo<String>) {
    <expr>f.foo</expr>
}
