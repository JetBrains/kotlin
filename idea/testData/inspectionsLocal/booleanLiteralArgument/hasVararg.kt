// PROBLEM: none
fun foo(a: Boolean, vararg b: Int) {}

fun test() {
    foo(true<caret>, 1, 2, 3)
}

