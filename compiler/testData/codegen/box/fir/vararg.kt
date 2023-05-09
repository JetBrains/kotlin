// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// (type mismatch)
// WITH_STDLIB

fun <A : Comparable<A>> arrayData(vararg values: A): A = values.first()

fun test(b: Byte) = select(arrayData(1), b)

fun <S : Comparable<S>> select(a: S, b: S): S {
    if (a.compareTo(b) > 0) return a else return b
}

fun box(): String {
    val res = test(-42)
    val res2 = res.dec()
    res.doSomething()
    if (res == 1.toByte() && res2 == 0.toByte()) return "OK" else return res.toString()
}

fun Byte.doSomething() {}
