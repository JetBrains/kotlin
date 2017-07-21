fun f(a: Long, b: Int, c: String) {}

fun foo() {
    val a = 0L
    val b = 0
    val c = "0"
    f(a, <caret>)
}

// EXIST: "b, c"