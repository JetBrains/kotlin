// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-83349 Wrong hashCode values in instantiated annotations

// This test fails on Native with test grouping and package renaming enabled,
// because the latter doesn't yet handle annotation toString implementations properly.
// Disable test grouping as a workaround:
// NATIVE_STANDALONE

package test

annotation class A(val t: String = "d")
annotation class B(
    val a: A = A(),
    val arr: Array<A> = emptyArray()
)

fun box(): String {
    val s = B().toString()

    if (!(s.contains("@test.B("))) return "Fail1: $s"
    if (!(s.contains("a=@test.A(t=d)"))) return "Fail2: $s"
    if (!s.contains("arr=[]")) return "Fail3: $s"

    return "OK"
}
