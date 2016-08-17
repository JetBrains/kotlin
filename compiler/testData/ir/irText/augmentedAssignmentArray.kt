fun foo(): IntArray = intArrayOf(1, 2, 3)

fun testVariable() {
    var x = foo()
    x[0] += 1
    foo()[0] *= 2
}

