// "Add 4th parameter to function 'foo'" "true"
fun foo(i1: Int, i2: Int, i3: Int, i4: Int) {
}

fun test() {
    foo(1, 2, 3, <caret>"", 5)
}