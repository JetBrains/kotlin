// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -USELESS_IS_CHECK
// SKIP_TXT


// TESTCASE NUMBER: 1

class Case1<AT: CharSequence>(val x: AT) {

    inner class B(val y: AT) {
        fun case1a() {
            val k: AT = x

            if (k is AT) {
                ""
            }
        }

        fun case1b() {
            val k: AT = x

            when (k) {
                is AT -> ""
            }
        }
    }

    inner class D() {
        fun boo(x: Any): AT = TODO()
        fun case1() {
            boo("") checkType { check<AT>() }
        }
    }
}

// TESTCASE NUMBER: 2

class Case2<AT: CharSequence>(val x: AT) {

    inner class B(val y: AT) {
        fun case2a() {
            val k: AT = x

            if (k is AT) {
                ""
            }
        }

        fun case2b() {
            val k: AT = x

            when (k) {
                is AT -> ""
            }
        }
    }


    inner class C() {
        fun boo(x: Any): AT = TODO()
        fun case2() {
            boo("") checkType { check<AT>() }
        }
    }
}
