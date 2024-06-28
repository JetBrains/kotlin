// FIR_IDENTICAL
// ISSUE: KT-58028, KT-63377
// FIR_DUMP

class Owner<test> {
    companion object {
        val test = 12
    }

    inner class I<test> {
        val some = test

        fun foo() {
            val some = test
        }
    }
}
