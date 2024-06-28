// FIR_IDENTICAL
// ISSUE: KT-69040

interface Controller<T> {
    fun add(t: T)
}

fun <T1, R1> T1.foo(g: (T1) -> R1): R1 = TODO()
fun <T2> Controller<T2>.get(): T2 = TODO()

fun <BK, BV> build(
    transformer: (Controller<BK>) -> BV,
): BV = TODO()

fun test1() {
    build { c ->
        c.add("")

        c.foo {
            c.get()
        }
    }
}

interface Base
interface A : Base
interface B : Base

fun test2(a: A, b: B) {
    val x = build { c ->
        c.add(a)

        c.foo {
            c.add(b)
            c.get()
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("Base")!>x<!>
}
