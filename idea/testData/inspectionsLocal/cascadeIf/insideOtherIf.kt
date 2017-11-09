fun println(s: String) {}

fun test(x: Double, a: Boolean, b: Boolean) {
    if (x > 0.0) {
        <caret>if (a) {
            println("a")
        }
        else if (b) {
            println("b")
        }
        else {
            println("none")
        }
    }
}