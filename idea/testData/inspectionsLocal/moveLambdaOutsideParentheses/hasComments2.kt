fun foo(i: Int, f: () -> Unit) {}

fun test() {
    foo(1, /* c1 */ /* c2 */ <caret>{ /* c3 */ } /* c4 */ /* c5 */)
}