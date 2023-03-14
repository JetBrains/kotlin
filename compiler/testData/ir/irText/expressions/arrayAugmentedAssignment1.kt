// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun foo(): IntArray = intArrayOf(1, 2, 3)
fun bar() = 42

class C(val x: IntArray)

fun testVariable() {
    var x = foo()
    x[0] += 1
}

fun testCall() {
    foo()[bar()] *= 2
}

fun testMember(c: C) {
    c.x[0]++
}
