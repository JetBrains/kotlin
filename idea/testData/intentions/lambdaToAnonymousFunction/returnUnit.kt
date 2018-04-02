fun unit(f: (Int) -> Unit) {}

fun foo(i: Int) {}

fun test() {
    unit {
        foo(it)
        foo(it)
    }<caret>
}