// RUN_PIPELINE_TILL: FRONTEND
interface A
interface B
interface C : A, B
interface D : A, B

fun <T> select(vararg x: T): T = x[0]

fun test(c: C, d: D) {
    val it = select(c, d)
    acceptString(<!ARGUMENT_TYPE_MISMATCH("A & B; kotlin.String")!>it<!>)
}

fun acceptString(s: String) {}
