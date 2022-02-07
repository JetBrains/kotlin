fun test_1(x: Int, y: Int) {
    val a = x + y // x.plus(y)
}

fun test_2(b1: Boolean, b2: Boolean) {
    val and1 = b1 && b2
    val or1 = b1 || b2

    val and2 = b1.and(b2)
    val or2 = b1.or(b2)
}

fun test_3(x: String?) {
    val s = x ?: "hello"
}

operator fun A.compareTo(x: A): Int

fun test_4(x: Int, y: Int) {
    val a = x < y
    // CompareNode("<", x.compareTo(y))
    // x.compareTo(y) < 0
}

fun test_5(x: String, y: String) {
    val a = x == y
    // x.equals(y)
    // Any.equals
}

fun test_6() {
    val a = mutableListOf("a")
    a += ""
    // resolved to: a.plusAssign("")

    var b = listOf("a")
    b += ""
    // resolved to: b = b.plus("")

    var c = mutableListOf("c")
    c += ""
    // resolved to: ambigous error call
}
