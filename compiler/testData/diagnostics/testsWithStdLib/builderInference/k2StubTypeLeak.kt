// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun test_1() {
    sequence {
        constrain(this)
        yieldAll(mk()) // Will be completed in partial mode, due to builder inference

        Unit
    }
}

fun constrain(t: Inv<String>) {}

fun <U> sequence(block: Inv<U>.() -> Unit): U = null!!

interface Inv<T> {
    fun <S: T> yieldAll(seq: Inv<S>)
}

fun <K> mk(): K = TODO()
