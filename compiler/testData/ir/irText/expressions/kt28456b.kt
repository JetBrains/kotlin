class A

operator fun A.get(i: Int, a: Int = 1, b: Int = 2, c: Int = 3, d: Int = 4) = 0

operator fun A.set(i: Int, j: Int = 42, v: Int) {}

fun testSimpleAssignment(a: A) {
    a[1] = 0
}

fun testPostfixIncrement(a: A) = a[1]++

fun testCompoundAssignment(a: A) {
    a[1] += 10
}