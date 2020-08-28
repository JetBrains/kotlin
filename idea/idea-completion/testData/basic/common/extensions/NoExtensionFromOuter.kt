// FIR_COMPARISON
class C {
    fun String.memberExtForString(){}

    companion object {
        fun foo() {
            "".<caret>
        }
    }
}

// ABSENT: memberExtForString
