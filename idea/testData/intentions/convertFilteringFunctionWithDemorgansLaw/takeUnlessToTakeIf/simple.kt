// WITH_RUNTIME
fun test(i: Int) {
    i.takeUnless<caret> { it != 1 }
}