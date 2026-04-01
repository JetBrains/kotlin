// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76663
// FIR_DUMP

class Test(val something: String?) {
    fun toPlainObj(data: String): dynamic {
        return if (something == null) {
            throw IllegalStateException("Something is not defined.")
        } else {
            JSON.parse(data)
        }
    }
}
