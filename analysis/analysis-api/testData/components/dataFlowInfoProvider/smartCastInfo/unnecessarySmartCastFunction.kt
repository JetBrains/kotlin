interface Foo {
    fun foo()
}

interface Bar : Foo {
}

fun usage(f: Foo) {
    if (f is Bar) {
        <expr>f</expr>.foo()
    }
}