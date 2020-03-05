fun test(s: String) {
    when (val x = s.length) {
        1 -> println("one")
        2 -> println("two")
        else -> println(x<caret>)
    }
}