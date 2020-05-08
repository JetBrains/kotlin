// WITH_RUNTIME
fun test(i: Int) {
    i.takeIf<caret> { it != 1 }
}