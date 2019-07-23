fun println(s: String) {}

fun test(a: Boolean, b: Boolean) {
    <caret>if (a) {
        // comment
        // comment
        println("a")
    }
    else if (b) println("b")
    else println("none")
}