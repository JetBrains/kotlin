fun foo(): IntArray = intArrayOf(1, 2, 3)
fun bar() = 42

fun testVariable() {
    var x = foo()
    x[0] += 1
    foo()[0] *= 2
    foo()[bar()] -= 1
}
