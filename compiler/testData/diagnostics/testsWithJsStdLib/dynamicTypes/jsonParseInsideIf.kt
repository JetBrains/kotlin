// ISSUE: KT-76663
// FIR_DUMP

class Test(val something: String?) {
    fun toPlainObj(data: String): dynamic {
        return if (something == null) {
            throw IllegalStateException("Something is not defined.")
        } else {
            JSON.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE!>parse<!>(data)
        }
    }
}
