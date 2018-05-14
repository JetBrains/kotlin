// "Remove parameter 'bar'" "true"

fun foo(bar<caret>: Int) {}

fun test() {
    foo(1)
}