fun foo(a: List<Int>) {
    for (x in a) <caret>{
        val y = x
    }
}