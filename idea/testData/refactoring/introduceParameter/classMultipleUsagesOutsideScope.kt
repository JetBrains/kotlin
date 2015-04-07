// TARGET:
class A(val a: Int, s: String) {
    val x = a + 1

    fun foo() = (<selection>a + 1</selection>) * 2
}

fun test() {
    val t = A(1, "2").a
}