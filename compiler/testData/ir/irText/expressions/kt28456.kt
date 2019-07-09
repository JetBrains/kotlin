class A

operator fun A.get(vararg xs: Int) = 0

operator fun A.set(i: Int, j: Int, v: Int) {}

fun testSimpleAssignment(a: A) {
    a[1, 2] = 0
}

fun testPostfixIncrement(a: A) = a[1, 2]++

fun testCompoundAssignment(a: A) {
    a[1, 2] += 10
}