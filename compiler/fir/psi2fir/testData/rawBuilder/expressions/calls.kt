infix fun distance(x: Int, y: Int) = x + y

fun test(): Int = 3 distance 4

fun testRegular(): Int = distance(3, 4)

class My(var x: Int) {
    operator fun invoke() = x

    fun foo() {}

    fun copy() = My(x)
}

fun testInvoke(): Int = My(13)()

fun testQualified(first: My, second: My?) {
    println(first.x)
    println(second?.x)
    first.foo()
    second?.foo()
    first.copy().foo()
    first.x = 42
}