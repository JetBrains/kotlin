// "Add 3rd parameter to function 'foo'" "true"
// DISABLE-ERRORS
fun foo(i1: Int, i2: Int, i3: Int, i4: Int) {
}

fun test() {
    foo(1, 2, <caret>"", 4, 5)
}