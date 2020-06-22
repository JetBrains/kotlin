fun foo(f: (x: Int) -> Unit) {}

fun bar(x: Int, y: Int = 42) {}

fun test() {
    foo <caret>{ bar(it) }
}
