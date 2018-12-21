// IGNORE_BACKEND: JVM_IR
// FILE: util.kt

inline fun ieq(x: Int, y: Int) = x == y
inline fun ine(x: Int, y: Int) = x != y
inline fun ilt(x: Int, y: Int) = x < y
inline fun ile(x: Int, y: Int) = x <= y
inline fun igt(x: Int, y: Int) = x > y
inline fun ige(x: Int, y: Int) = x >= y

// FILE: test.kt

fun testeq(x: Int) = ieq(x, 0)
fun testne(x: Int) = ine(x, 0)
fun testlt(x: Int) = ilt(x, 0)
fun testle(x: Int) = ile(x, 0)
fun testgt(x: Int) = igt(x, 0)
fun testge(x: Int) = ige(x, 0)

// @TestKt.class:
// 0 IF_ICMPEQ
// 0 IF_ICMPNE
// 0 IF_ICMPGE
// 0 IF_ICMPGT
// 0 IF_ICMPLE
// 0 IF_ICMPLT