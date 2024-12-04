// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-69040

interface Controller<T> {
    fun add(t: T)
}

fun <T1, R1> T1.foo(g: (T1) -> R1): R1 = TODO()
fun <T2, T3> Controller<T2>.get(): MyPair<T2, T3> = TODO()

class MyPair<out P1, out P2>

fun <BK, BV, IRR> build(
    i: IRR,
    transformer: (Controller<BK>) -> MyPair<BV, IRR>,
): BV = TODO()

class W

fun test1(w: W) {
    build(w) { c ->
        c.add("")

        c.foo {
            c.get()
        }
    }
}

interface Base
interface A : Base
interface B : Base

fun test2(a: A, b: B, w: W) {
    val x = build(w) { c ->
        c.add(a)

        c.foo {
            c.add(b)
            c.get()
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("Base")!>x<!>
}
