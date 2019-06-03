// PROBLEM: none
fun foo(p: List<String?>) {
    val v = p[0]
    <caret>if (v == null) {
        bar()
        return
    }
}

fun bar(){}