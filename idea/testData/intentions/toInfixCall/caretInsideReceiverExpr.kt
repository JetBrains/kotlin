// IS_APPLICABLE: false
interface X {
    infix fun infix(p: Int): X
}

fun foo(num: X) {
    n<caret>um.infix(1)
}