// INTENTION_TEXT: Simplify negated '!in' expression to 'in'
class A(val e: Int) {
    fun contains(i: Int): Boolean = e == i
}

fun test(n: Int) {
    val a = A(1)
    <caret>!(0 !in a)
}
