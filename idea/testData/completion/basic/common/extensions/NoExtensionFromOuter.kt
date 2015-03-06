class C {
    fun String.memberExtForString(){}

    default object {
        fun foo() {
            "".<caret>
        }
    }
}

// ABSENT: memberExtForString
