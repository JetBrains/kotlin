// ISSUE: KT-64077
fun <R1> build(block: InvBuilder<R1>.() -> Unit): R1 = TODO()
fun <R2> build2(block: InvBuilder<R2>.() -> Unit): R2 = TODO()

class InvBuilder<R>() {
    fun set(r: R) {}
    fun get(): R = TODO()
}

fun consumeInt(e: Int) {}

fun test() {
    val ret = build {
        set("")
        build2 {
            set(1)
            consumeInt(<!TYPE_MISMATCH!>this@build.get()<!>) // K1 red ARGUMENT_TYPE_MISMATCH, runtime crash K2
        }
        Unit // This unit is essential!!!
    }
}
