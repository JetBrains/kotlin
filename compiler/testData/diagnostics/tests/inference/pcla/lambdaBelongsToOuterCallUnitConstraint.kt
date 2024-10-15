// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-69040

interface Controller<T> {
    fun get(): T
}

fun <T1, R1> T1.foo(g: (T1) -> R1): R1 = TODO()

fun <BK : <!FINAL_UPPER_BOUND!>Unit<!>> build(
    transformer: (Controller<BK>) -> BK,
): BK = TODO()

fun unitParam(x: Unit) {}

fun test(s: String) {
    val x = build { c ->
        unitParam(c.get())

        s
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x<!>
}
