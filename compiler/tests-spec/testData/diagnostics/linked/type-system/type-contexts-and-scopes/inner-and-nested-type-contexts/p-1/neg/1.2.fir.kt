// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -USELESS_IS_CHECK -IMPLICIT_CAST_TO_ANY
// SKIP_TXT


// TESTCASE NUMBER: 1

class Case1<AT: CharSequence>(val x: AT) {

    inner class C() {
        fun case1a(x: Any) {
            if (x is AT) {
                ""
            }
        }

        fun case1b(x: Any) = when (x) {
                is AT -> println("at")
                else -> ""
            }

    }
}

// TESTCASE NUMBER: 2

class Case2<AT: CharSequence>(val x: AT) {

    inner class C() {
        fun case2a(x: CharSequence) {
            if (x is AT) {
                ""
            }
        }

        fun case2b(x: CharSequence) {
            when (x) {
                is AT -> ""
            }
        }
    }
}
