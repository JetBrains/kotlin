class Foo<T : Foo<T>>(t: T) {
    val t2 = <expr>t</expr>
}