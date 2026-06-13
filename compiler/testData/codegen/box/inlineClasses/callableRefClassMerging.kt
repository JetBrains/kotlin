// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapperA(val s: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapperB(val s: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapInt1(val n: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapInt2(val n: Int)

fun lambdaA(a: WrapperA): () -> String = { a.s }
fun lambdaB(b: WrapperB): () -> String = { b.s }
fun lambdaC(c: WrapInt1): () -> Int = { c.n }
fun lambdaD(d: WrapInt2): () -> Int = { d.n }

// Generic lambdas with different type parameter names but same upper bound (Any?) should
// erase to the same type and be merged into a single callable reference class.
fun <T> lambdaT(t: T): () -> T = { t }
fun <U> lambdaU(u: U): () -> U = { u }

fun box(): String {
    val a = lambdaA(WrapperA("hello"))
    val b = lambdaB(WrapperB("world"))
    val c = lambdaC(WrapInt1(1))
    val d = lambdaD(WrapInt2(2))
    if (a() != "hello") return "FAIL a: ${a()}"
    if (b() != "world") return "FAIL b: ${b()}"
    if (c() != 1) return "FAIL c: ${c()}"
    if (d() != 2) return "FAIL d: ${d()}"
    val t = lambdaT("generic")
    val u = lambdaU("also generic")
    if (t() != "generic") return "FAIL t: ${t()}"
    if (u() != "also generic") return "FAIL u: ${u()}"
    return "OK"
}
