// !LANGUAGE: +ContextReceivers

context(Int, String)
fun foo(): Int {
    return this@Int + 42
}

context(Int)
fun foo(): Int {
    return this@Int + 42
}

fun test() {
    with(42) {
        foo()
    }
}