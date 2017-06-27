// PROBLEM: none
fun test(s: String): Int {
    var n: Int = 1;

    <caret>if (s.equals("add")) {
        n += 1
    } else {
        n -= 1
    }

    return n
}