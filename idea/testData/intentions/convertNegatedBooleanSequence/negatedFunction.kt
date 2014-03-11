fun bar1() {
    return true
}

fun bar2() {
    return false
}

fun foo() {
    return <caret>!bar1() && !bar2()
}