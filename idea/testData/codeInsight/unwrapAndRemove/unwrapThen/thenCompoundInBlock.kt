// OPTION: 1
fun foo(n: Int): Int {
    <caret>if (n > 0) {
        println("> 0")
        n + 10
    } else {
        println("<= 0")
        n - 10
    }

    return n
}
