class A

operator fun A.set(vararg i: Int, v: Int) {}

fun testSimpleAssignment(a: A) {
    a[1, 2, 3] = 0
}
