// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-83349 Wrong hashCode values in instantiated annotations
annotation class A(val t: String)
annotation class WithAnn(val a: A, val xs: Array<A>)

fun box(): String {
    val a1 = A("a")
    val a2 = A("a")
    val a3 = A("b")

    if (a1 != a2) return "Fail1"
    if (a1 == a3) return "Fail2"

    val w1 = WithAnn(a1, arrayOf(A("x"), A("y")))
    val w2 = WithAnn(a2, arrayOf(A("x"), A("y")))
    val w3 = WithAnn(a3, arrayOf(A("y"), A("x")))

    if (w1 != w2) return "Fail3"
    if (w1.hashCode() != w2.hashCode()) return "Fail4"
    if (w1 == w3) return "Fail5"

    return "OK"
}