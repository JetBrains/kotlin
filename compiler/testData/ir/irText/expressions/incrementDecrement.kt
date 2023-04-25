// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

var p: Int = 0
val arr = intArrayOf(1, 2, 3)

class C {
    var p: Int = 0
    operator fun get(i: Int) = i
    operator fun set(i: Int, value: Int) {}
}

object O {
    var p: Int = 0
    operator fun get(i: Int) = i
    operator fun set(i: Int, value: Int) {}
}

fun testVarPrefix() {
    var x = 0
    val x1 = ++x
    val x2 = --x
}

fun testVarPostfix() {
    var x = 0
    val x1 = x++
    val x2 = x--
}

fun testPropPrefix() {
    val p1 = ++p
    val p2 = --p
}

fun testPropPostfix() {
    val p1 = p++
    val p2 = p--
}

fun testArrayPrefix() {
    val a1 = ++arr[0]
    val a2 = --arr[0]
}

fun testArrayPostfix() {
    val a1 = arr[0]++
    val a2 = arr[0]--
}


fun testClassPropPrefix() {
    val p1 = ++C().p
    val p2 = --C().p
}

fun testClassPropPostfix() {
    val p1 = C().p++
    val p2 = C().p--
}

fun testClassOperatorPrefix() {
    val a1 = ++C()[0]
    val a2 = --C()[0]
}

fun testClassOperatorPostfix() {
    val a1 = C()[0]++
    val a2 = C()[0]--
}

fun testObjectPropPrefix() {
    val p1 = ++O.p
    val p2 = --O.p
}

fun testObjectPropPostfix() {
    val p1 = O.p++
    val p2 = O.p--
}

fun testObjectOperatorPrefix() {
    val a1 = ++O[0]
    val a2 = --O[0]
}

fun testObjectOperatorPostfix() {
    val a1 = O[0]++
    val a2 = O[0]--
}
