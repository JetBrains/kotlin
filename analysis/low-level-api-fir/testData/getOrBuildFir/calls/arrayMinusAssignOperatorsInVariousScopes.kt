class Foo {
    companion object {
        operator fun get(n: Int): Int = 42
    }
}

private operator fun Any.set(i: Int, value: Int) {}

fun usageFoo() {
    <expr>Foo[1] -= 5</expr>
}

