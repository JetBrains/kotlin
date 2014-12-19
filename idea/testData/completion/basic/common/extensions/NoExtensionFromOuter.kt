class C {
    fun String.memberExtForString(){}

    class object {
        fun foo() {
            "".<caret>
        }
    }
}

// ABSENT: memberExtForString
