interface Foo {
    operator fun <T> invoke(t: T)
}

fun test(f: Foo) {
    <expr>f("")</expr>
}