fun foo(a: List<Int>) {
    for<caret> (x in a) {
        val y = x
    }
}