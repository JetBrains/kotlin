interface Foo {
    val foo: Int
}

interface Bar : Foo {
}

fun usage(f: Foo) {
    if (f is Bar) {
        <expr>f</expr>.foo
    }
}
