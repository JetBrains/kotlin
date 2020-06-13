// "Add 2nd parameter to function 'foo'" "true"
fun foo(i1: Int, i2: Int, i3: Int, i4: Int) {
}

fun test() {
    foo(1, <caret>"", 3, 4, 5)
}