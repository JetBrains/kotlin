// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-64474, KT-66751
// MODULE: a
// FILE: a.kt
interface A
interface B

// MODULE: b(a)
// FILE: b.kt

class C : A, B
class D : A, B

val c = C()
val d = D()

// MODULE: c(b)
// FILE: c.kt
fun <T> select(vararg t: T): T = t[0]

fun test() {
    val x = select(c, d)
    x
}
