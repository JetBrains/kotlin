fun foo(a: Int) {}

fun test() {
    consume(<expr>::foo</expr>)
}

fun consume(f: (Int) -> Unit) {}