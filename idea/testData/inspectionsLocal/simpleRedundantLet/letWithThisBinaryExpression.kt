// PROBLEM: none
// WITH_RUNTIME


fun Int.foo() {
    let<caret> { it.dec() + 1 }
}