fun println(s: String) {}

fun foo(x: Boolean, y: Boolean) {
    if (x) {
        <caret>if (!y) return
        println("no1")
        return
    }
    println("no2")
}