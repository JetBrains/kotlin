// ISSUE: KT-58028, KT-63377
// FIR_DUMP

class Owner<test> {
    companion object {
        val test = 12
    }

    inner class I<test> {
        val some = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>test<!>

        fun foo() {
            val some = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>test<!>
        }
    }
}
